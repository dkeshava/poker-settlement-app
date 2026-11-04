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

    @GetMapping("/users/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        return ResponseEntity.ok(userDetailsService.searchUsers(query));
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
        System.out.println("getting verification");
        String verification=userDetailsService.verify(user);

        if (verification!=null) {
            return ResponseEntity.ok(verification);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
