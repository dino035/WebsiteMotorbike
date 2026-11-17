package com.nguyen.motoparts.advisory.service;

import com.nguyen.motoparts.advisory.llm.ProductFact;
import com.nguyen.motoparts.advisory.llm.SearchCriteria;
import com.nguyen.motoparts.catalog.entity.Motorbike;
import com.nguyen.motoparts.catalog.entity.Product;
import com.nguyen.motoparts.catalog.entity.SparePart;
import com.nguyen.motoparts.common.enums.ProductStatus;
import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lọc sản phẩm THẬT trong CSDL theo tiêu chí mà LLM đọc ra. Đây là nơi quyết định sản phẩm nào được gợi ý;
 * LLM không được chọn. Chỉ gợi ý sản phẩm còn hàng (AVAILABLE), rẻ nhất trước.
 * Phụ tùng theo xe của khách đi qua luật tương thích (CompatibilityRule) do phần Sản phẩm & Quản trị quản lý.
 */
@Service
public class ChatProductSearch {

    static final int MAX_RESULTS = 5;

    /** Kết quả: mã sản phẩm theo thứ tự, dữ kiện cho LLM, và cờ cho biết dòng xe khách nhắc tới có trong hệ thống không. */
    public record Found(List<String> productIds, List<ProductFact> facts, boolean vehicleUnknown) {
        static Found none(boolean vehicleUnknown) {
            return new Found(List.of(), List.of(), vehicleUnknown);
        }
    }

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public Found search(SearchCriteria raw) {
        SearchCriteria c = raw.sanitized();
        boolean hasVehicle = c.brand() != null || c.model() != null;

        List<? extends Product> products;
        switch (c.kind()) {
            case SPARE_PART -> {
                if (hasVehicle) {
                    List<VehicleModel> models = findVehicleModels(c);
                    if (models.isEmpty()) {
                        return Found.none(true);
                    }
                    List<String> compatibleIds = compatiblePartIds(models);
                    if (compatibleIds.isEmpty()) {
                        return Found.none(false);
                    }
                    products = spareParts(c, compatibleIds);
                } else {
                    products = spareParts(c, null);
                }
            }
            case MOTORBIKE -> products = motorbikes(c);
            default -> products = hasVehicle ? motorbikes(c) : anyProduct(c);
        }
        if (products.isEmpty()) {
            return Found.none(false);
        }
        List<String> ids = new ArrayList<>();
        List<ProductFact> facts = new ArrayList<>();
        for (Product p : products) {
            ids.add(p.getProductId());
            facts.add(toFact(p));
        }
        return new Found(ids, facts, false);
    }

    // ---------------------------------------------------------------- xe máy

    private List<Motorbike> motorbikes(SearchCriteria c) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Motorbike> q = cb.createQuery(Motorbike.class);
        Root<Motorbike> r = q.from(Motorbike.class);
        List<Predicate> ps = basePredicates(cb, r, c);
        if (c.brand() != null) {
            ps.add(cb.like(cb.lower(r.get("brand")), like(c.brand()), '\\'));
        }
        if (c.model() != null) {
            ps.add(cb.like(cb.lower(r.get("model")), like(c.model()), '\\'));
        }
        if (c.minRegistrationYear() != null) {
            ps.add(cb.greaterThanOrEqualTo(r.<Integer>get("registrationYear"), c.minRegistrationYear()));
        }
        if (c.maxRegistrationYear() != null) {
            ps.add(cb.lessThanOrEqualTo(r.<Integer>get("registrationYear"), c.maxRegistrationYear()));
        }
        if (c.keyword() != null) {
            ps.add(cb.or(containsName(cb, r, c.keyword()),
                    cb.like(cb.lower(r.get("brand")), like(c.keyword()), '\\'),
                    cb.like(cb.lower(r.get("model")), like(c.keyword()), '\\')));
        }
        q.select(r).where(ps.toArray(new Predicate[0])).orderBy(cb.asc(r.get("price")), cb.asc(r.get("productId")));
        return em.createQuery(q).setMaxResults(MAX_RESULTS).getResultList();
    }

    // ---------------------------------------------------------------- phụ tùng

    private List<SparePart> spareParts(SearchCriteria c, List<String> onlyIds) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SparePart> q = cb.createQuery(SparePart.class);
        Root<SparePart> r = q.from(SparePart.class);
        List<Predicate> ps = basePredicates(cb, r, c);
        if (onlyIds != null) {
            ps.add(r.get("productId").in(onlyIds));
        }
        if (c.partType() != null) {
            ps.add(cb.like(cb.lower(r.get("partType")), like(c.partType()), '\\'));
        }
        if (c.partOrigin() != null) {
            ps.add(cb.equal(r.get("partOrigin"), c.partOrigin()));
        }
        if (c.keyword() != null) {
            ps.add(cb.or(containsName(cb, r, c.keyword()), cb.like(cb.lower(r.get("partType")), like(c.keyword()), '\\')));
        }
        q.select(r).where(ps.toArray(new Predicate[0])).orderBy(cb.asc(r.get("price")), cb.asc(r.get("productId")));
        return em.createQuery(q).setMaxResults(MAX_RESULTS).getResultList();
    }

    /**
     * Dòng xe của khách trong bảng VehicleModel: hãng khớp không phân biệt hoa thường, dòng xe chứa chuỗi khách nói
     * (có dòng trùng tên hẳn thì chỉ lấy dòng đó); có năm thì chỉ lấy dòng có khoảng năm sản xuất bao trùm năm đó.
     */
    private List<VehicleModel> findVehicleModels(SearchCriteria c) {
        StringBuilder jpql = new StringBuilder("select v from VehicleModel v where 1 = 1");
        if (c.brand() != null) {
            jpql.append(" and lower(v.brand) like :brand escape '\\'");
        }
        if (c.model() != null) {
            jpql.append(" and lower(v.model) like :model escape '\\'");
        }
        var query = em.createQuery(jpql.toString(), VehicleModel.class);
        if (c.brand() != null) {
            query.setParameter("brand", like(c.brand()));
        }
        if (c.model() != null) {
            query.setParameter("model", like(c.model()));
        }
        List<VehicleModel> found = query.getResultList();

        if (c.model() != null) {
            String wanted = c.model().toLowerCase(Locale.ROOT);
            List<VehicleModel> exact = found.stream().filter(v -> v.getModel().toLowerCase(Locale.ROOT).equals(wanted)).toList();
            if (!exact.isEmpty()) {
                found = exact;
            }
        }
        if (c.vehicleYear() != null) {
            int year = c.vehicleYear();
            found = found.stream().filter(v -> v.covers(year)).toList();
        }
        return found;
    }

    private List<String> compatiblePartIds(List<VehicleModel> models) {
        List<String> modelIds = models.stream().map(VehicleModel::getModelId).toList();
        return em.createQuery("select distinct r.sparePart.productId from CompatibilityRule r where r.vehicleModel.modelId in :ids",
                String.class).setParameter("ids", modelIds).getResultList();
    }

    // ---------------------------------------------------------------- chung

    private List<Product> anyProduct(SearchCriteria c) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Product> q = cb.createQuery(Product.class);
        Root<Product> r = q.from(Product.class);
        List<Predicate> ps = basePredicates(cb, r, c);
        if (c.keyword() != null) {
            ps.add(containsName(cb, r, c.keyword()));
        }
        q.select(r).where(ps.toArray(new Predicate[0])).orderBy(cb.asc(r.get("price")), cb.asc(r.get("productId")));
        return em.createQuery(q).setMaxResults(MAX_RESULTS).getResultList();
    }

    /** Còn hàng và nằm trong khoảng giá. */
    private List<Predicate> basePredicates(CriteriaBuilder cb, Root<? extends Product> r, SearchCriteria c) {
        List<Predicate> ps = new ArrayList<>();
        ps.add(cb.equal(r.get("status"), ProductStatus.AVAILABLE));
        if (c.minPrice() != null) {
            ps.add(cb.greaterThanOrEqualTo(r.<BigDecimal>get("price"), BigDecimal.valueOf(c.minPrice())));
        }
        if (c.maxPrice() != null) {
            ps.add(cb.lessThanOrEqualTo(r.<BigDecimal>get("price"), BigDecimal.valueOf(c.maxPrice())));
        }
        return ps;
    }

    private Predicate containsName(CriteriaBuilder cb, Root<? extends Product> r, String keyword) {
        return cb.like(cb.lower(r.get("name")), like(keyword), '\\');
    }

    /** Mẫu LIKE chứa chuỗi, thoát ký tự đặc biệt (% _ \) để chuỗi từ LLM không thành mẫu tìm kiếm. */
    private static String like(String s) {
        String escaped = s.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static ProductFact toFact(Product p) {
        String description;
        if (p instanceof Motorbike m) {
            List<String> parts = new ArrayList<>();
            parts.add(nullToEmpty(m.getBrand()) + " " + nullToEmpty(m.getModel()));
            if (m.getRegistrationYear() != null) {
                parts.add("đăng ký năm " + m.getRegistrationYear());
            }
            if (m.getOdometerAtIntake() != null) {
                parts.add("đã đi " + m.getOdometerAtIntake() + " km");
            }
            if (m.getOwnerCount() != null) {
                parts.add(m.getOwnerCount() + " chủ");
            }
            description = String.join(", ", parts);
        } else {
            description = p.getDetail();
        }
        return new ProductFact(p.getName(), p.getProductType(), p.getPrice().longValue(), description);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
