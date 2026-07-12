package com.abms.auth.config;

import com.abms.auth.entity.Role;
import com.abms.auth.entity.User;
import com.abms.auth.repository.RoleRepository;
import com.abms.auth.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRole(1, "RESIDENT");
        seedRole(2, "STAFF");
        seedRole(3, "MANAGER");

        if (userRepository.count() > 0) {
            return;
        }

        Role residentRole = roleRepository.findByRoleName("RESIDENT").orElseThrow();
        Role staffRole = roleRepository.findByRoleName("STAFF").orElseThrow();
        Role managerRole = roleRepository.findByRoleName("MANAGER").orElseThrow();

        userRepository.save(User.builder()
                .userId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .role(residentRole)
                .email("resident1@abms.local")
                .password(passwordEncoder.encode("11111111"))
                .fullName("Resident One")
                .phone("0900000001")
                .idCard("ID-RESIDENT-001")
                .status("ACTIVE")
                .build());

        userRepository.save(User.builder()
                .userId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .role(staffRole)
                .email("staff1@abms.local")
                .password(passwordEncoder.encode("11111111"))
                .fullName("Staff One")
                .phone("0900000002")
                .idCard("ID-STAFF-001")
                .status("ACTIVE")
                .build());

        userRepository.save(User.builder()
                .userId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
                .role(managerRole)
                .email("manager1@abms.local")
                .password(passwordEncoder.encode("11111111"))
                .fullName("Manager One")
                .phone("0900000003")
                .idCard("ID-MANAGER-001")
                .status("ACTIVE")
                .build());

        userRepository.save(User.builder()
                .userId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .role(residentRole)
                .email("resident2@abms.local")
                .password(passwordEncoder.encode("11111111"))
                .fullName("Resident Two")
                .phone("0900000004")
                .idCard("ID-RESIDENT-002")
                .status("ACTIVE")
                .build());
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