package com.devteria.profile.repository;

import com.devteria.profile.entity.Profile;
import com.devteria.profile.entity.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {
    Optional<Role> findByNameEquals(String name);
}
