package com.example.websitemotorbike.model;

import com.example.websitemotorbike.util.DBUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@Setter
public class Motorbike extends Product {

    private String brand;
    private String model;
    private Integer registrationYear;
    private Integer ownerCount;
    private String accidentHistory;
    private String chassisNumber;
    private String engineNumber;
    private Integer odometerAtIntake;

    @OneToMany(mappedBy = "motorbike", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VehiclePhoto> photos = new ArrayList<>();

    public void addPhoto(VehiclePhoto photo) {
        photos.add(photo);
        photo.setMotorbike(this);
    }

    public void removePhoto(VehiclePhoto photo) {
        photos.remove(photo);
        photo.setMotorbike(null);
    }

    @Override
    public String getProductType(){
        return "Xe máy";
    }

    @Override
    public String getDetail(){
        return brand + " - " + model;
    }

    @Override
    public String getTypeCode(){
        return "Motorbike";
    }

    /** Thêm và xóa ảnh của một xe trong cùng một transaction. */
    public static void applyChanges(String motorbikeId, List<VehiclePhoto> toAdd,
                                    List<String> removeIds) {
        EntityManager em = DBUtil.getEmFactory().createEntityManager();
        EntityTransaction trans = em.getTransaction();
        trans.begin();
        try {
            Motorbike motorbike = em.find(Motorbike.class, motorbikeId);
            if (motorbike == null) {
                throw new IllegalStateException("Xe không tồn tại");
            }
            for (String photoId : removeIds) {
                VehiclePhoto photo = em.find(VehiclePhoto.class, photoId);
                // Chỉ xóa ảnh thật sự thuộc về xe này
                if (photo != null && photo.getMotorbike().getProductId().equals(motorbikeId)) {
                    motorbike.getPhotos().remove(photo);
                    em.remove(photo);
                }
            }
            for (VehiclePhoto photo : toAdd) {
                motorbike.addPhoto(photo);
                em.persist(photo);
            }
            trans.commit();
        } catch (Exception e) {
            System.out.println(e);
            if (trans.isActive()) {
                trans.rollback();
            }
            throw new IllegalStateException("Không thể cập nhật ảnh, vui lòng thử lại", e);
        } finally {
            em.close();
        }
    }
}
