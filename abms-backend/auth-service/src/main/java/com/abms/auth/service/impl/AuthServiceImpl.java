package com.abms.auth.service.impl;

import com.abms.auth.client.ApartmentClient;
import com.abms.auth.constant.RoleNames;
import com.abms.auth.constant.UserStatus;
import com.abms.auth.dto.AuthResponse;
import com.abms.auth.dto.ChangePasswordRequest;
import com.abms.auth.dto.LoginRequest;
import com.abms.auth.dto.RegisterRequest;
import com.abms.auth.dto.ResidentRegistrationRequest;
import com.abms.auth.entity.Role;
import com.abms.auth.entity.User;
import com.abms.auth.repository.RoleRepository;
import com.abms.auth.repository.UserRepository;
import com.abms.auth.security.CurrentUser;
import com.abms.auth.security.JwtUtil;
import com.abms.auth.security.SecurityUtils;
import com.abms.auth.service.AuthService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Integer DEFAULT_ROLE_ID = 1;
    private static final String DEFAULT_ROLE_NAME = RoleNames.RESIDENT;
    private static final String DEFAULT_STATUS = "PENDING_APPROVAL";
    private static final String ACTIVE_STATUS = UserStatus.ACTIVE;
    private static final String LOCKED_STATUS = UserStatus.LOCKED;
    private static final String INACTIVE_STATUS = UserStatus.INACTIVE;
    private static final String REJECTED_STATUS = "REJECTED";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApartmentClient apartmentClient;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Self registration is disabled. Please contact your manager or administrator to receive an account.");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (LOCKED_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Account is locked");
        }

        if (DEFAULT_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is pending manager approval");
        }

        if (INACTIVE_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
        }

        if (REJECTED_STATUS.equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account registration was rejected");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return AuthResponse.builder()
                .userId(user.getUserId().toString())
                .roleName(user.getRole().getRoleName())
                .buildingId(user.getBuildingId() == null ? null : user.getBuildingId().toString())
                .email(user.getEmail())
                .status(user.getStatus())
                .message("Đăng nhập thành công")
                .token(jwtUtil.generateToken(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse changePassword(String authorizationHeader, ChangePasswordRequest request) {
        CurrentUser currentUser = securityUtils.parseCurrentUser(authorizationHeader);
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + currentUser.getUserId()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        User savedUser = userRepository.save(user);

        return AuthResponse.builder()
                .userId(savedUser.getUserId().toString())
                .roleName(savedUser.getRole().getRoleName())
                .email(savedUser.getEmail())
                .status(savedUser.getStatus())
                .message("Đổi mật khẩu thành công")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuthResponse> getPendingResidents() {
        return userRepository.findByRoleRoleNameAndStatusOrderByFullNameAsc(DEFAULT_ROLE_NAME, DEFAULT_STATUS)
                .stream()
                .map(user -> AuthResponse.builder()
                        .userId(user.getUserId().toString())
                        .roleName(user.getRole().getRoleName())
                        .buildingId(user.getBuildingId() == null ? null : user.getBuildingId().toString())
                        .email(user.getEmail())
                        .status(user.getStatus())
                        .message(user.getFullName())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public AuthResponse approveResident(UUID userId) {
        User user = getUserOrThrow(userId);
        apartmentClient.approveResidentRegistration(userId);
        user.setStatus(ACTIVE_STATUS);
        User savedUser = userRepository.save(user);
        return AuthResponse.builder()
                .userId(savedUser.getUserId().toString())
                .roleName(savedUser.getRole().getRoleName())
                .buildingId(savedUser.getBuildingId() == null ? null : savedUser.getBuildingId().toString())
                .email(savedUser.getEmail())
                .status(savedUser.getStatus())
                .message("Đã duyệt cư dân thành công")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse rejectResident(UUID userId) {
        User user = getUserOrThrow(userId);
        apartmentClient.rejectResidentRegistration(userId);
        user.setStatus(REJECTED_STATUS);
        User savedUser = userRepository.save(user);
        return AuthResponse.builder()
                .userId(savedUser.getUserId().toString())
                .roleName(savedUser.getRole().getRoleName())
                .buildingId(savedUser.getBuildingId() == null ? null : savedUser.getBuildingId().toString())
                .email(savedUser.getEmail())
                .status(savedUser.getStatus())
                .message("Đã từ chối đăng ký cư dân")
                .build();
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }
}