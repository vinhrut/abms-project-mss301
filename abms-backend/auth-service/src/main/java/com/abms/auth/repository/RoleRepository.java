package com.abms.auth.repository;

import com.abms.auth.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);
}