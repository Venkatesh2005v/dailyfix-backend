package com.example.dailyfix.service;

import com.example.dailyfix.enums.Role;
import com.example.dailyfix.model.User;
import com.example.dailyfix.repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends OidcUserService { // Changed to OidcUserService

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        // 1. Get the original user data
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String name = oidcUser.getAttribute("name");

        // 2. Your existing DB saving logic
        if (email != null) {
            userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setName(name != null ? name : email);
                newUser.setRole(Role.USER);
                newUser.setActive(true);
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setPassword("OIDC_LOGIN_" + UUID.randomUUID());
                return userRepository.save(newUser);
            });
        }

        // 3. THE CRITICAL CHANGE: Return a user that uses "email" as the Principal Name
        return new DefaultOidcUser(
                oidcUser.getAuthorities(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email" // This tells Spring to use email as the name/ID for this session
        );
    }
}