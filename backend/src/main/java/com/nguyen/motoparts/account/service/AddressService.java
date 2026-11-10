package com.nguyen.motoparts.account.service;

import com.nguyen.motoparts.account.dto.AddressRequest;
import com.nguyen.motoparts.account.dto.AddressResponse;
import com.nguyen.motoparts.account.entity.Address;
import com.nguyen.motoparts.account.entity.User;
import com.nguyen.motoparts.account.repository.AddressRepository;
import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Sổ địa chỉ của khách. Mọi thao tác ghi khóa dòng tài khoản (SELECT ... FOR UPDATE) trước,
 * để hai yêu cầu cùng lúc của một khách không làm sai "tối đa một địa chỉ mặc định" hay vượt giới hạn.
 */
@Service
public class AddressService {

    /** Số địa chỉ tối đa mỗi tài khoản (giả định từ bản JSP, cần chốt với nhóm). */
    public static final int MAX_PER_ACCOUNT = 10;

    private static final Pattern PHONE = Pattern.compile("^(0|\\+84)[0-9]{9}$");

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(String accountId) {
        return addressRepository.findByUser_AccountIdOrderByDefaultAddressDescCreatedAtAsc(accountId)
                .stream().map(AddressResponse::from).toList();
    }

    /** Địa chỉ đầu tiên tự là mặc định; chọn makeDefault thì địa chỉ này thành mặc định và địa chỉ cũ bị bỏ. */
    @Transactional
    public AddressResponse create(String accountId, AddressRequest req) {
        User user = lockAccount(accountId);
        String phone = normalizePhone(req.receiverPhone());

        long count = addressRepository.countByUser_AccountId(accountId);
        if (count >= MAX_PER_ACCOUNT) {
            throw new BadRequestException("Mỗi tài khoản chỉ lưu tối đa " + MAX_PER_ACCOUNT + " địa chỉ.");
        }
        boolean isDefault = req.makeDefault() || count == 0;
        if (isDefault) {
            addressRepository.clearDefault(accountId);
        }

        Address address = new Address();
        address.setUser(user);
        apply(address, req, phone);
        address.setDefaultAddress(isDefault);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(String accountId, String addressId, AddressRequest req) {
        lockAccount(accountId);
        String phone = normalizePhone(req.receiverPhone());
        Address address = findOwned(accountId, addressId);

        if (req.makeDefault() && !address.isDefaultAddress()) {
            addressRepository.clearDefault(accountId);
            address = findOwned(accountId, addressId);   // clearDefault làm mới lại bộ nhớ đệm
            address.setDefaultAddress(true);
        }
        apply(address, req, phone);
        return AddressResponse.from(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse setDefault(String accountId, String addressId) {
        lockAccount(accountId);
        Address address = findOwned(accountId, addressId);
        if (!address.isDefaultAddress()) {
            addressRepository.clearDefault(accountId);
            address = findOwned(accountId, addressId);
            address.setDefaultAddress(true);
            address = addressRepository.save(address);
        }
        return AddressResponse.from(address);
    }

    /** Xóa một địa chỉ; xóa địa chỉ mặc định mà còn địa chỉ khác thì địa chỉ cũ nhất còn lại thành mặc định. */
    @Transactional
    public void delete(String accountId, String addressId) {
        lockAccount(accountId);
        Address address = findOwned(accountId, addressId);
        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            addressRepository.findFirstByUser_AccountIdOrderByCreatedAtAsc(accountId).ifPresent(a -> {
                a.setDefaultAddress(true);
                addressRepository.save(a);
            });
        }
    }

    private User lockAccount(String accountId) {
        return userRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }

    private Address findOwned(String accountId, String addressId) {
        return addressRepository.findByAddressIdAndUser_AccountId(addressId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ."));
    }

    private static void apply(Address address, AddressRequest req, String normalizedPhone) {
        address.setReceiverName(req.receiverName().trim());
        address.setReceiverPhone(normalizedPhone);
        address.setHouseNumber(req.houseNumber().trim());
        address.setStreet(req.street().trim());
        address.setDistrict(req.district().trim());
        address.setCity(req.city().trim());
    }

    /** Bỏ khoảng trắng và dấu . ( ) - rồi kiểm tra dạng 0xxxxxxxxx hoặc +84xxxxxxxxx. */
    private static String normalizePhone(String phone) {
        String normalized = phone.trim().replaceAll("[ .()-]", "");
        if (!PHONE.matcher(normalized).matches()) {
            throw new BadRequestException("Số điện thoại không hợp lệ.");
        }
        return normalized;
    }
}
