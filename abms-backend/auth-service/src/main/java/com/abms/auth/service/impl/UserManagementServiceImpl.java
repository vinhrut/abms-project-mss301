package com.abms.auth.service.impl;

import com.abms.auth.client.ApartmentClient;
import com.abms.auth.constant.RoleNames;
import com.abms.auth.constant.UserStatus;
import com.abms.auth.dto.ApartmentResponse;
import com.abms.auth.dto.CreateManagerRequest;
import com.abms.auth.dto.CreateUserRequest;
import com.abms.auth.dto.ResidentRegistrationRequest;
import com.abms.auth.dto.UserResponse;
import com.abms.auth.entity.Role;
import com.abms.auth.entity.User;
import com.abms.auth.repository.RoleRepository;
import com.abms.auth.repository.UserRepository;
import com.abms.auth.security.CurrentUser;
import com.abms.auth.security.SecurityUtils;
import com.abms.auth.service.UserManagementService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final Set<String> MANAGER_CREATABLE_ROLES = Set.of(
            RoleNames.RESIDENT,
            RoleNames.STAFF,
            RoleNames.TECHNICIAN);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;
    private final ApartmentClient apartmentClient;

    @Override
    @Transactional
    public UserResponse createManager(String authorizationHeader, CreateManagerRequest request) {
        CurrentUser actor = securityUtils.parseCurrentUser(authorizationHeader);
        ensureRole(actor, RoleNames.ADMIN);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        Role managerRole = getRole(RoleNames.MANAGER);

        apartmentClient.getBuildingById(request.getBuildingId());

        User manager = User.builder()
                .role(managerRole)
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .idCard(request.getIdCard())
                .status(UserStatus.ACTIVE)
                .buildingId(request.getBuildingId())
                .createdBy(actor.getUserId())
                .build();

        return mapUser(userRepository.save(manager));
    }

    @Override
    @Transactional
    public UserResponse createUser(String authorizationHeader, CreateUserRequest request) {
        CurrentUser actor = securityUtils.parseCurrentUser(authorizationHeader);
        ensureRole(actor, RoleNames.MANAGER);

        String requestedRole = normalizeRole(request.getRoleName());
        if (!MANAGER_CREATABLE_ROLES.contains(requestedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager can only create resident, staff or technician accounts");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        Role role = getRole(requestedRole);

        User user = User.builder()
                .role(role)
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .idCard(request.getIdCard())
                .status(UserStatus.ACTIVE)
                .buildingId(actor.getBuildingId())
                .createdBy(actor.getUserId())
                .build();

        User savedUser = userRepository.save(user);

        if (RoleNames.RESIDENT.equals(requestedRole)) {
            if (request.getApartmentId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apartment id is required for resident account");
            }

            ApartmentResponse apartment = apartmentClient.getApartmentById(request.getApartmentId());
            if (apartment == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apartment not found");
            }

            if (actor.getBuildingId() == null || !actor.getBuildingId().equals(apartment.getBuildingId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager can only create resident in own building");
            }

            apartmentClient.createResidentRegistration(ResidentRegistrationRequest.builder()
                    .userId(savedUser.getUserId().toString())
                    .apartmentId(request.getApartmentId().toString())
                    .relationship(request.getRelationship())
                    .residenceType(request.getResidenceType())
                    .build());

            apartmentClient.approveResidentRegistration(savedUser.getUserId());
        }

        return mapUser(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(String authorizationHeader) {
        CurrentUser actor = securityUtils.parseCurrentUser(authorizationHeader);

        if (RoleNames.ADMIN.equals(actor.getRoleName())) {
            return userRepository.findAllByOrderByFullNameAsc().stream().map(this::mapUser).toList();
        }

        if (RoleNames.MANAGER.equals(actor.getRoleName())) {
            return userRepository.findByBuildingIdOrderByFullNameAsc(actor.getBuildingId()).stream().map(this::mapUser).toList();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view user list");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String authorizationHeader, UUID userId) {
        CurrentUser actor = securityUtils.parseCurrentUser(authorizationHeader);
        User user = getUserOrThrow(userId);
        ensureCanAccess(actor, user);
        return mapUser(user);
    }

    @Override
    @Transactional
    public UserResponse lockUser(String authorizationHeader, UUID userId) {
        CurrentUser actor = securityUtils.parseCurrentUser(authorizationHeader);
        User user = getUserOrThrow(userId);
        ensureCanManage(actor, user);
        user.setStatus(UserStatus.LOCKED);
        user.setLockedBy(actor.getUserId());
        user.setLockedAt(LocalDateTime.now());
        return mapUser(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse unlockUser(String authorizationHeader, UUID userId) {
        CurrentUser actor = securityUtils.parseCurrentUser(authorizationHeader);
        User user = getUserOrThrow(userId);
        ensureCanManage(actor, user);
        user.setStatus(UserStatus.ACTIVE);
        user.setLockedBy(null);
        user.setLockedAt(null);
        return mapUser(userRepository.save(user));
    }

    private void ensureRole(CurrentUser actor, String expectedRole) {
        if (!expectedRole.equals(actor.getRoleName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
    }

    private void ensureCanAccess(CurrentUser actor, User targetUser) {
        if (RoleNames.ADMIN.equals(actor.getRoleName())) {
            return;
        }

        if (RoleNames.MANAGER.equals(actor.getRoleName())
                && actor.getBuildingId() != null
                && actor.getBuildingId().equals(targetUser.getBuildingId())) {
            return;
        }

        if (actor.getUserId().equals(targetUser.getUserId())) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this user");
    }

    private void ensureCanManage(CurrentUser actor, User targetUser) {
        String targetRole = targetUser.getRole().getRoleName();

        if (RoleNames.ADMIN.equals(actor.getRoleName())) {
            if (!RoleNames.MANAGER.equals(targetRole)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin can only lock or unlock manager accounts");
            }
            return;
        }

        if (RoleNames.MANAGER.equals(actor.getRoleName())) {
            if (!MANAGER_CREATABLE_ROLES.contains(targetRole)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager can only lock or unlock resident, staff or technician accounts");
            }
            if (actor.getBuildingId() == null || !actor.getBuildingId().equals(targetUser.getBuildingId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager can only manage users in own building");
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to manage user accounts");
    }

    private Role getRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found: " + roleName));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private String normalizeRole(String roleName) {
        if (roleName == null) {
            return null;
        }
        return roleName.trim().toUpperCase();
    }

    private UserResponse mapUser(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .idCard(user.getIdCard())
                .roleName(user.getRole().getRoleName())
                .status(user.getStatus())
                .buildingId(user.getBuildingId())
                .createdBy(user.getCreatedBy())
                .lockedBy(user.getLockedBy())
                .lockedAt(user.getLockedAt())
                .build();
    }
}