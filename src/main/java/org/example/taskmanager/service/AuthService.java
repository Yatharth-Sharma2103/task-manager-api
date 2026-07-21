package org.example.taskmanager.service;

import org.example.taskmanager.domain.Role;
import org.example.taskmanager.domain.User;
import org.example.taskmanager.dto.AuthResponse;
import org.example.taskmanager.dto.LoginRequest;
import org.example.taskmanager.dto.RegisterRequest;
import org.example.taskmanager.exception.BadRequestException;
import org.example.taskmanager.repository.UserRepository;
import org.example.taskmanager.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.ROLE_USER);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.bearer(token, jwtService.getExpirationMs(), user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        // Delegates credential validation to Spring Security (throws BadCredentialsException on failure).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return AuthResponse.bearer(token, jwtService.getExpirationMs(), user.getUsername(), user.getRole().name());
    }
}
