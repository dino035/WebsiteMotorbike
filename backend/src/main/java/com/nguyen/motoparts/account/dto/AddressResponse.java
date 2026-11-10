package com.nguyen.motoparts.account.dto;

import com.nguyen.motoparts.account.entity.Address;

public record AddressResponse(
        String addressId,
        String receiverName,
        String receiverPhone,
        String houseNumber,
        String street,
        String district,
        String city,
        String fullAddress,
        boolean defaultAddress) {

    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getAddressId(), a.getReceiverName(), a.getReceiverPhone(), a.getHouseNumber(),
                a.getStreet(), a.getDistrict(), a.getCity(), a.getFullAddress(), a.isDefaultAddress());
    }
}
