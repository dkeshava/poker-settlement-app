package com.dk.pokersettlement.controllers;

import com.dk.pokersettlement.models.User;
import com.dk.pokersettlement.services.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.AccessDeniedException;

@RestController
public class TestController {

    private CurrentUserService currentUserService;

    public TestController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public User getCurrentUser() throws AccessDeniedException {
        return currentUserService.getCurrentUser();
    }
}