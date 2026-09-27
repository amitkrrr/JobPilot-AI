package com.jobpilot.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobpilot.dto.auth.LoginRequest;
import com.jobpilot.dto.auth.LoginResponse;
import com.jobpilot.dto.auth.RegisterRequest;
import com.jobpilot.dto.auth.RegisterResponse;
import com.jobpilot.user.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {

        User savedUser = authService.registerUser(request);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail()
        );
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        User user = authService.authenticate(request);
        

        String token = jwtService.generateToken(user.getEmail());

        return new LoginResponse(token);
    }
}