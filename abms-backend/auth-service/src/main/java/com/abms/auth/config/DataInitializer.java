package com.abms.auth.config;

import com.abms.auth.constant.RoleNames;
import com.abms.auth.constant.UserStatus;
import com.abms.auth.entity.Role;
import com.abms.auth.entity.User;
import com.abms.auth.repository.RoleRepository;
import com.abms.auth.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "11111111";

    // Mixed alphanumeric UUIDs — keep in sync across apartment/vehicle/notification/maintenance seeds.
    private static final UUID BUILDING_A = UUID.fromString("7f3a9c2e-1b4d-4e8f-a6c0-92d5e8b1f4a3");
    private static final UUID BUILDING_B = UUID.fromString("8e4b0d3f-2c5e-4f9a-b7d1-a3e6f9c2d5b4");
    private static final UUID BUILDING_C = UUID.fromString("9f5c1e4a-3d6f-4a0b-c8e2-b4f7a0d3e6c5");

    private static final UUID ADMIN_ID = UUID.fromString("1a2b3c4d-5e6f-4789-a012-b3c4d5e6f789");
    private static final UUID MANAGER_A_ID = UUID.fromString("2b3c4d5e-6f70-489a-b123-c4d5e6f7890a");
    private static final UUID MANAGER_B_ID = UUID.fromString("3c4d5e6f-7081-49ab-c234-d5e6f7890a1b");
    private static final UUID RESIDENT_A101_ID = UUID.fromString("4d5e6f70-8192-4abc-d345-e6f7890a1b2c");
    private static final UUID RESIDENT_A102_ID = UUID.fromString("5e6f7081-92a3-4bcd-e456-f7890a1b2c3d");
    private static final UUID RESIDENT_B101_ID = UUID.fromString("6f708192-a3b4-4cde-f567-890a1b2c3d4e");
    private static final UUID STAFF_A_ID = UUID.fromString("708192a3-b4c5-4def-a678-90a1b2c3d4e5");
    private static final UUID TECHNICIAN_A_ID = UUID.fromString("8192a3b4-c5d6-4ef0-b789-0a1b2c3d4e5f");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
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

        seedUsers.forEach(this::seedUser);
    }

    private void seedUser(User user) {
        userRepository.findByEmail(user.getEmail()).ifPresent(existingByEmail -> {
            if (!existingByEmail.getUserId().equals(user.getUserId())) {
                userRepository.delete(existingByEmail);
                userRepository.flush();
            }
        });

        userRepository.findById(user.getUserId())
                .ifPresentOrElse(
                        existingUser -> ensureSeedUserState(existingUser, user),
                        () -> userRepository.save(user));
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
