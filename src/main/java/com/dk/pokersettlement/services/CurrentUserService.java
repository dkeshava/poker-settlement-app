package com.dk.pokersettlement.services;

import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.repositories.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;

@Service
public class CurrentUserService {
    private UserRepo userRepo;

    public CurrentUserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User getCurrentUser() throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||  authentication.getPrincipal() instanceof String) {
            throw new AccessDeniedException(
                    "No authenticated user");
        }
        String username = authentication.getName();
        return userRepo.findOptionalByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(username));
    }
}
