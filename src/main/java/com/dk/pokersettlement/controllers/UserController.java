package com.dk.pokersettlement.controllers;

import com.dk.pokersettlement.dtos.UserResponse;
import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class UserController {
    UserService userDetailsService;
    public UserController(UserService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register")
    public UserResponse createUser(@RequestBody User user) {

        User user1= userDetailsService.addUser(user);
        return new UserResponse(
                user1.getId(),
                user1.getUsername()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        String verification = userDetailsService.verify(user);

        if (verification != null) {
            return ResponseEntity.ok(verification);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam("q") String query) {
        List<UserResponse> users = userDetailsService.searchUsers(query);
        return ResponseEntity.ok(users);
    }
}
