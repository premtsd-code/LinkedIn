package com.premtsd.linkedin.userservice.repository;

import com.premtsd.linkedin.userservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    public Optional<Role> findByName(String name);
    public boolean existsByName(String name);

}
