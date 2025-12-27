package com.example.dailyfix.controller;

import com.example.dailyfix.dto.request.UserRequest;
import com.example.dailyfix.model.User;
import com.example.dailyfix.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<String> registerUser(@RequestBody UserRequest request){
        userService.registerUser(request);
        return new ResponseEntity<>("Registered successfully", HttpStatus.CREATED);
    }

}
