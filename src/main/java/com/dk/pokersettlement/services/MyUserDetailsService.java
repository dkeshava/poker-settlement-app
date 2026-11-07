package com.dk.pokersettlement.services;

import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.models.UserPrincipal;
import com.dk.pokersettlement.repositories.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MyUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepo repo;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.debug("Loading user by username: {}", username);

        User user1 = repo.findByUsername(username);

        if (user1 == null) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException(username);
        }

        return new UserPrincipal(user1);
    }
}
