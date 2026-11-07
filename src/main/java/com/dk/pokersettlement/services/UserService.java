package com.dk.pokersettlement.services;

import com.dk.pokersettlement.dtos.UserResponse;
import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.repositories.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AuthenticationManager authenticationManager;
    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Autowired
    private JWTService jwtService;

    public User addUser(User user) {
        log.info("Registering new user: {}", user.getUsername());
        user.setPassword(encoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public String verify(User user) {
        log.debug("Authenticating user: {}", user.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if (authentication.isAuthenticated()) {
            return jwtService.generateToken(user.getUsername());
        }
        return "failure";
    }

    public List<UserResponse> searchUsers(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return userRepo.findTop10ByUsernameContainingIgnoreCase(query.trim())
                .stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername()))
                .toList();
    }
}