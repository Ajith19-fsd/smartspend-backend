package com.smartspend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleConstraintFix {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void fixConstraint() {
        try {
            jdbcTemplate.execute(
                    "ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check"
            );
            jdbcTemplate.execute(
                    "ALTER TABLE roles ADD CONSTRAINT roles_name_check CHECK (name IN ('ROLE_USER', 'ROLE_ADMIN'))"
            );
            System.out.println("✔ Updated roles_name_check constraint successfully");
        } catch (Exception e) {
            System.out.println("⚠ Error updating constraint: " + e.getMessage());
        }
    }
}
