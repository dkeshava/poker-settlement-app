package com.dk.pokersettlement.services;

import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.models.UserPrincipal;
import com.dk.pokersettlement.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepo repo;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        System.out.println("Searching for username: " + username);

        User user1 = repo.findByUsername(username);

        if (user1 == null) {
            System.out.println("USER NOT FOUND");
            throw new UsernameNotFoundException(username);
        }

        System.out.println("Found user: " + user1.getUsername());
        System.out.println("User ID from DB: " + user1.getId());

        return new UserPrincipal(user1);
    }
}
