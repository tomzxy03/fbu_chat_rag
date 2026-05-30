package com.tomzxy.fbu_chat.controller;

import com.tomzxy.fbu_chat.dto.LoginRequest;
import com.tomzxy.fbu_chat.dto.LoginResponse;
import com.tomzxy.fbu_chat.dto.RegisterRequest;
import com.tomzxy.fbu_chat.entity.User;
import com.tomzxy.fbu_chat.repository.UserRepository;
import com.tomzxy.fbu_chat.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    UserDetails userDetails = (UserDetails) auth.getPrincipal();

    String token = jwtUtil.generateToken(userDetails);

    String role = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("ROLE_USER")
            .replace("ROLE_", "");
    return ResponseEntity.ok(LoginResponse.builder()
            .token(token)
            .username(userDetails.getUsername())
            .role(role)
            .build());
}

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepo.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username đã tồn tại"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .build();
        userRepo.save(user);

        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công"));
    }
}
