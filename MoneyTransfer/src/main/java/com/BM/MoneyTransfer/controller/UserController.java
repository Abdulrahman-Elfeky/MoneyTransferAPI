package com.BM.MoneyTransfer.controller;


import com.BM.MoneyTransfer.dto.LoginResponseDTO;
import com.BM.MoneyTransfer.dto.SignUpRequestDTO;
import com.BM.MoneyTransfer.dto.UserLoginRequestDTO;
import com.BM.MoneyTransfer.dto.ViewUserProfileDTO;
import com.BM.MoneyTransfer.service.JwtService;
import com.BM.MoneyTransfer.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {
    UserService userService;
    AuthenticationManager authenticationManager;
    JwtService jwtService;

    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody SignUpRequestDTO userSignUpDto, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Unknown error"
                    ));

            return ResponseEntity.badRequest().body(errors);
        }
        userService.save(userSignUpDto.toUser());
        return ResponseEntity.ok("User registered successfully");

    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginRequestDTO userLoginDto) {
        try {
            // Authenticate the user
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(userLoginDto.getEmail(), userLoginDto.getPassword())
            );

            // Generate the JWT token
            String jwt = jwtService.generateToken(userLoginDto.getEmail());

            // Return the JWT token in the response
            return ResponseEntity.ok(new LoginResponseDTO(HttpStatus.OK,"Bearer","Login successful!",jwt));

        } catch (AuthenticationException e) {
            // Handle authentication failure
            return ResponseEntity.status(401).body("Invalid credentials");
        }

    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String refreshedToken = jwtService.refreshToken(token);
        return ResponseEntity.ok(refreshedToken);

    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        jwtService.blacklistToken(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/user")
    public ViewUserProfileDTO getCurrentUser() {
        return userService.findById(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
