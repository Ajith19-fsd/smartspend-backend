package com.smartspend.auth.repository;

import com.smartspend.auth.model.Role;
import com.smartspend.auth.model.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}