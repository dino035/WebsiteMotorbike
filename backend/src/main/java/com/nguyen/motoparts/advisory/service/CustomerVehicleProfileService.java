package com.nguyen.motoparts.advisory.service;

import com.nguyen.motoparts.account.repository.UserRepository;
import com.nguyen.motoparts.advisory.dto.VehicleProfileRequest;
import com.nguyen.motoparts.advisory.dto.VehicleProfileResponse;
import com.nguyen.motoparts.advisory.entity.CustomerVehicleProfile;
import com.nguyen.motoparts.advisory.repository.ChatSessionRepository;
import com.nguyen.motoparts.advisory.repository.CustomerVehicleProfileRepository;
import com.nguyen.motoparts.common.exception.BadRequestException;
import com.nguyen.motoparts.common.exception.ConflictException;
import com.nguyen.motoparts.common.exception.ResourceNotFoundException;
import com.nguyen.motoparts.vehicle.entity.VehicleModel;
import com.nguyen.motoparts.vehicle.repository.VehicleModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hồ sơ xe của khách (SRS 3.15): mỗi hồ sơ là một xe khách đang có (dòng xe + năm đăng ký, biệt danh tùy chọn).
 * Chat box dùng hồ sơ làm ngữ cảnh để gợi ý phụ tùng lắp vừa xe đó. Mọi thao tác giới hạn theo accountId lấy từ token.
 */
@Service
public class CustomerVehicleProfileService {

    /** Mỗi khách lưu tối đa chừng này xe. */
    static final int MAX_PROFILES = 10;

    private final CustomerVehicleProfileRepository profiles;
    private final VehicleModelRepository vehicleModels;
    private final UserRepository users;
    private final ChatSessionRepository sessions;

    public CustomerVehicleProfileService(CustomerVehicleProfileRepository profiles, VehicleModelRepository vehicleModels,
                                         UserRepository users, ChatSessionRepository sessions) {
        this.profiles = profiles;
        this.vehicleModels = vehicleModels;
        this.users = users;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public List<VehicleProfileResponse> listMine(String accountId) {
        return profiles.findMine(accountId).stream().map(VehicleProfileResponse::from).toList();
    }

    @Transactional
    public VehicleProfileResponse create(String accountId, VehicleProfileRequest request) {
        // khóa dòng tài khoản để hai lần thêm cùng lúc không vượt giới hạn
        users.findByIdForUpdate(accountId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
        if (profiles.countByAccountId(accountId) >= MAX_PROFILES) {
            throw new ConflictException("Bạn chỉ lưu được tối đa " + MAX_PROFILES + " xe. Hãy xóa bớt một xe trước khi thêm.");
        }
        VehicleModel vehicle = checkedVehicle(request);
        CustomerVehicleProfile profile = new CustomerVehicleProfile();
        profile.setAccountId(accountId);
        fill(profile, request, vehicle);
        profiles.save(profile);
        return VehicleProfileResponse.from(profile);
    }

    @Transactional
    public VehicleProfileResponse update(String accountId, String profileId, VehicleProfileRequest request) {
        CustomerVehicleProfile profile = owned(accountId, profileId);
        VehicleModel vehicle = checkedVehicle(request);
        fill(profile, request, vehicle);
        return VehicleProfileResponse.from(profile);
    }

    @Transactional
    public void delete(String accountId, String profileId) {
        CustomerVehicleProfile profile = owned(accountId, profileId);
        sessions.clearProfile(profile.getProfileId());
        profiles.delete(profile);
    }

    private CustomerVehicleProfile owned(String accountId, String profileId) {
        return profiles.findOwned(profileId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ xe này."));
    }

    /** Dòng xe phải có trong hệ thống và năm đăng ký phải nằm trong khoảng năm sản xuất của dòng xe đó. */
    private VehicleModel checkedVehicle(VehicleProfileRequest request) {
        VehicleModel vehicle = vehicleModels.findById(request.vehicleModelId().trim())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy dòng xe này."));
        if (!vehicle.covers(request.registrationYear())) {
            throw new BadRequestException("Năm đăng ký phải nằm trong khoảng năm sản xuất của dòng xe này ("
                    + vehicle.getYearFrom() + " - " + vehicle.getYearTo() + ").");
        }
        return vehicle;
    }

    private static void fill(CustomerVehicleProfile profile, VehicleProfileRequest request, VehicleModel vehicle) {
        String nickname = request.nickname() == null ? "" : request.nickname().trim();
        profile.setNickname(nickname.isEmpty() ? null : nickname);
        profile.setVehicleModel(vehicle);
        profile.setRegistrationYear(request.registrationYear());
    }
}
