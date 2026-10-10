package com.example.websitemotorbike.util;

import com.example.websitemotorbike.dao.ProductDB;
import com.example.websitemotorbike.model.Motorbike;
import com.example.websitemotorbike.model.Product;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Danh sách hãng và dòng xe có sẵn để khách chọn khi khai báo hồ sơ xe (SRS 3.15, bước 2).
 * Gồm các dòng xe phổ biến ở Việt Nam (BUILT_IN) và mọi hãng, dòng xe đang có trong kho cửa hàng (đọc qua
 * ProductDB của Nhóm 2). Gộp cả hai để khách chọn được xe của mình dù cửa hàng chưa bán dòng đó.
 * Muốn thêm dòng xe cho mọi khách chỉ cần thêm vào BUILT_IN.
 */
public class VehicleCatalog {

    /** Mỗi phần tử: hãng, rồi các dòng xe của hãng đó. */
    private static final List<List<String>> BUILT_IN = List.of(
            Arrays.asList("Honda", "Wave Alpha", "Wave RSX", "Blade", "Future", "Super Cub", "Vision", "Lead",
                    "Air Blade", "SH Mode", "SH", "PCX", "Winner X"),
            Arrays.asList("Yamaha", "Sirius", "Jupiter", "Exciter", "Janus", "Grande", "FreeGo", "NVX"),
            Arrays.asList("Suzuki", "Raider", "Satria", "Impulse", "Address"),
            Arrays.asList("SYM", "Attila", "Elegant", "Galaxy"),
            Arrays.asList("Piaggio", "Vespa Sprint", "Vespa Primavera", "Liberty", "Medley"),
            Arrays.asList("VinFast", "Klara", "Feliz", "Evo200"));

    /** Hãng và các dòng xe (sắp theo tên) để hiển thị ô chọn. */
    public static Map<String, TreeSet<String>> options() {
        Map<String, TreeSet<String>> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (List<String> row : BUILT_IN) {
            for (int i = 1; i < row.size(); i++) {
                add(options, row.get(0), row.get(i));
            }
        }
        for (Product product : ProductDB.search(null, "Motorbike", null, null, null)) {
            if (product instanceof Motorbike) {
                add(options, ((Motorbike) product).getBrand(), ((Motorbike) product).getModel());
            }
        }
        return options;
    }

    /**
     * Cặp hãng và dòng xe này có trong danh sách để chọn không. So sánh đúng từng ký tự (kể cả hoa thường)
     * để giá trị lưu vào hồ sơ luôn đúng như trong danh sách.
     */
    public static boolean contains(String brand, String model) {
        if (brand == null || model == null) {
            return false;
        }
        for (Map.Entry<String, TreeSet<String>> entry : options().entrySet()) {
            if (entry.getKey().equals(brand)) {
                for (String candidate : entry.getValue()) {
                    if (candidate.equals(model)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Bỏ qua mục trống hoặc chứa ký tự "|" (ký tự dùng để ghép hãng và dòng xe thành một giá trị của ô chọn)
     * để một dòng dữ liệu xấu của cửa hàng không làm hỏng ô chọn.
     */
    private static void add(Map<String, TreeSet<String>> options, String brand, String model) {
        if (brand == null || model == null) {
            return;
        }
        brand = brand.trim();
        model = model.trim();
        if (brand.isEmpty() || model.isEmpty() || brand.contains("|") || model.contains("|")) {
            return;
        }
        options.computeIfAbsent(brand, key -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(model);
    }
}
