package com.example.dailyfix.controller;

import com.example.dailyfix.dto.request.UserRequest;
import com.example.dailyfix.model.User;
import com.example.dailyfix.service.JwtService;
import com.example.dailyfix.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService service;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public User register(@RequestBody UserRequest request){
        return service.saveUser(request);
    }

    @PostMapping("/login")
    public String login(@RequestBody UserRequest request){
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getName(),request.getPassword()));

        if(authentication.isAuthenticated())
            return jwtService.generateToken(request.getName());
        else
            return "Login Failed";
    }
}
