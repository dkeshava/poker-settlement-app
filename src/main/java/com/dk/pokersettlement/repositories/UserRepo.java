package com.dk.pokersettlement.repositories;

import com.dk.pokersettlement.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User,Long> {
    User findByUsername(String username);
    Optional<User> findOptionalByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    List<User> findTop10ByUsernameContainingIgnoreCase(String username);
}
