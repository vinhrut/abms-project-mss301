package com.abms.auth.config;

import com.abms.auth.constant.RoleNames;
import com.abms.auth.constant.UserStatus;
import com.abms.auth.entity.Role;
import com.abms.auth.entity.User;
import com.abms.auth.repository.RoleRepository;
import com.abms.auth.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "11111111";
    private static final UUID BUILDING_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BUILDING_B = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BUILDING_C = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID MANAGER_B_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    private static final UUID RESIDENT_A101_ID = UUID.fromString("00000000-0000-0000-0000-000000001101");
    private static final UUID RESIDENT_A102_ID = UUID.fromString("00000000-0000-0000-0000-000000001102");
    private static final UUID RESIDENT_B101_ID = UUID.fromString("00000000-0000-0000-0000-000000001201");
    private static final UUID STAFF_A_ID = UUID.fromString("00000000-0000-0000-0000-000000002101");
    private static final UUID TECHNICIAN_A_ID = UUID.fromString("00000000-0000-0000-0000-000000003101");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRole(1, RoleNames.RESIDENT);
        seedRole(2, RoleNames.STAFF);
        seedRole(3, RoleNames.MANAGER);
        seedRole(4, RoleNames.ADMIN);
        seedRole(5, RoleNames.TECHNICIAN);

        Role residentRole = roleRepository.findByRoleName(RoleNames.RESIDENT).orElseThrow();
        Role staffRole = roleRepository.findByRoleName(RoleNames.STAFF).orElseThrow();
        Role managerRole = roleRepository.findByRoleName(RoleNames.MANAGER).orElseThrow();
        Role adminRole = roleRepository.findByRoleName(RoleNames.ADMIN).orElseThrow();
        Role technicianRole = roleRepository.findByRoleName(RoleNames.TECHNICIAN).orElseThrow();

        List<User> seedUsers = List.of(
                User.builder()
                .userId(ADMIN_ID)
                .role(adminRole)
                .email("admin@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("System Administrator")
                .phone("0900000000")
                .idCard("ID-ADMIN-001")
                .status(UserStatus.ACTIVE)
                .build(),
                User.builder()
                .userId(MANAGER_A_ID)
                .role(managerRole)
                .email("manager.a@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Manager Building A")
                .phone("0900000001")
                .idCard("ID-MANAGER-001")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(ADMIN_ID)
                .build(),
                User.builder()
                .userId(MANAGER_B_ID)
                .role(managerRole)
                .email("manager.b@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Manager Building B")
                .phone("0900000002")
                .idCard("ID-MANAGER-002")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_B)
                .createdBy(ADMIN_ID)
                .build(),
                User.builder()
                .userId(RESIDENT_A101_ID)
                .role(residentRole)
                .email("resident.a101@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Resident A101 Owner")
                .phone("0900000011")
                .idCard("ID-RESIDENT-001")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(MANAGER_A_ID)
                .build(),
                User.builder()
                .userId(RESIDENT_A102_ID)
                .role(residentRole)
                .email("resident.a102@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Resident A102 Tenant")
                .phone("0900000012")
                .idCard("ID-RESIDENT-002")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(MANAGER_A_ID)
                .build(),
                User.builder()
                .userId(RESIDENT_B101_ID)
                .role(residentRole)
                .email("resident.b101@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Resident B101 Owner")
                .phone("0900000013")
                .idCard("ID-RESIDENT-003")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_B)
                .createdBy(MANAGER_B_ID)
                .build(),
                User.builder()
                .userId(STAFF_A_ID)
                .role(staffRole)
                .email("staff.a@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Staff Building A")
                .phone("0900000021")
                .idCard("ID-STAFF-001")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(MANAGER_A_ID)
                .build(),
                User.builder()
                .userId(TECHNICIAN_A_ID)
                .role(technicianRole)
                .email("technician.a@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Technician Building A")
                .phone("0900000031")
                .idCard("ID-TECHNICIAN-001")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(MANAGER_A_ID)
                .build());

        cleanupUsers(seedUsers);
        seedUsers.forEach(this::seedUser);
    }

    private void cleanupUsers(List<User> seedUsers) {
        Set<UUID> allowedUserIds = new HashSet<>();
        seedUsers.stream().map(User::getUserId).forEach(allowedUserIds::add);

        userRepository.findAll().stream()
                .filter(existingUser -> !allowedUserIds.contains(existingUser.getUserId()))
                .forEach(userRepository::delete);
    }

    private void seedUser(User user) {
        userRepository.findById(user.getUserId())
                .ifPresentOrElse(existingUser -> ensureSeedUserState(existingUser, user), () -> userRepository.save(user));
    }

    private void ensureSeedUserState(User existingUser, User seedUser) {
        existingUser.setRole(seedUser.getRole());
        existingUser.setEmail(seedUser.getEmail());
        existingUser.setPassword(seedUser.getPassword());
        existingUser.setFullName(seedUser.getFullName());
        existingUser.setPhone(seedUser.getPhone());
        existingUser.setIdCard(seedUser.getIdCard());
        existingUser.setStatus(seedUser.getStatus());
        existingUser.setBuildingId(seedUser.getBuildingId());
        existingUser.setCreatedBy(seedUser.getCreatedBy());
        existingUser.setLockedAt(null);
        existingUser.setLockedBy(null);
        userRepository.save(existingUser);
    }

    private void seedRole(Integer roleId, String roleName) {
        if (roleRepository.existsByRoleName(roleName)) {
            return;
        }

        roleRepository.save(Role.builder()
                .roleId(roleId)
                .roleName(roleName)
                .build());
    }
}