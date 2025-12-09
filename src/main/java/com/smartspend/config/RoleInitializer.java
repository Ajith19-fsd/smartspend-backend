package com.smartspend.config;

import com.smartspend.auth.model.ERole;
import com.smartspend.auth.model.Role;
import com.smartspend.auth.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoleInitializer {

    @Autowired
    private RoleRepository roleRepository;

    @PostConstruct
    public void initRoles() {
        try {
            if (roleRepository.findByName(ERole.ROLE_USER).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_USER));
                System.out.println("✔ ROLE_USER inserted");
            }
            if (roleRepository.findByName(ERole.ROLE_ADMIN).isEmpty()) {
                roleRepository.save(new Role(ERole.ROLE_ADMIN));
                System.out.println("✔ ROLE_ADMIN inserted");
            }
        } catch (Exception e) {
            System.out.println("⚠ Error inserting roles: " + e.getMessage());
        }
    }
}
