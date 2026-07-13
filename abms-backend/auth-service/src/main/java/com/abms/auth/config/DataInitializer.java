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
    private static final UUID ADMIN_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID BUILDING_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BUILDING_B = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID BUILDING_C = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID MANAGER_1_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MANAGER_2_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID RESIDENT_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESIDENT_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RESIDENT_3_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID RESIDENT_4_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

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
        Role managerRole = roleRepository.findByRoleName(RoleNames.MANAGER).orElseThrow();
        Role adminRole = roleRepository.findByRoleName(RoleNames.ADMIN).orElseThrow();

        List<User> seedUsers = List.of(
                User.builder()
                .userId(ADMIN_ID)
                .role(adminRole)
                .email("admin@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("System Admin")
                .phone("0900000000")
                .idCard("ID-ADMIN-001")
                .status(UserStatus.ACTIVE)
                .build(),
                User.builder()
                .userId(MANAGER_1_ID)
                .role(managerRole)
                .email("manager1@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Manager Building A")
                .phone("0900000001")
                .idCard("ID-MANAGER-001")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(ADMIN_ID)
                .build(),
                User.builder()
                .userId(MANAGER_2_ID)
                .role(managerRole)
                .email("manager2@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Manager Building B")
                .phone("0900000002")
                .idCard("ID-MANAGER-002")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_B)
                .createdBy(ADMIN_ID)
                .build(),
                User.builder()
                .userId(RESIDENT_1_ID)
                .role(residentRole)
                .email("resident1@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Resident One")
                .phone("0900000011")
                .idCard("ID-RESIDENT-001")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(MANAGER_1_ID)
                .build(),
                User.builder()
                .userId(RESIDENT_2_ID)
                .role(residentRole)
                .email("resident2@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Resident Two")
                .phone("0900000012")
                .idCard("ID-RESIDENT-002")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_B)
                .createdBy(MANAGER_2_ID)
                .build(),
                User.builder()
                .userId(RESIDENT_3_ID)
                .role(residentRole)
                .email("resident3@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Resident Three")
                .phone("0900000013")
                .idCard("ID-RESIDENT-003")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_C)
                .createdBy(ADMIN_ID)
                .build(),
                User.builder()
                .userId(RESIDENT_4_ID)
                .role(residentRole)
                .email("resident4@abms.local")
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .fullName("Resident Four")
                .phone("0900000014")
                .idCard("ID-RESIDENT-004")
                .status(UserStatus.ACTIVE)
                .buildingId(BUILDING_A)
                .createdBy(MANAGER_1_ID)
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