package com.aifinance.service;

import com.aifinance.dto.AuthResponse;
import com.aifinance.dto.LoginRequest;
import com.aifinance.dto.RefreshTokenRequest;
import com.aifinance.dto.SignupRequest;
import com.aifinance.entity.Role;
import com.aifinance.entity.User;
import com.aifinance.repository.UserRepository;
import com.aifinance.security.CustomUserDetailsService;
import com.aifinance.security.JwtTokenProvider;
import com.aifinance.security.RateLimiterService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final RateLimiterService rateLimiterService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       CustomUserDetailsService userDetailsService,
                       RateLimiterService rateLimiterService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.rateLimiterService = rateLimiterService;
    }

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_ANALYST;
        User user = new User(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                role
        );
        userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                tokenProvider.getExpirationMs()
        );
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        if (!rateLimiterService.allowLoginAttempt(clientIp)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts from this IP. Please try again in a minute.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userDetailsService.getUserEntity(request.getUsernameOrEmail());
            String accessToken = tokenProvider.generateAccessToken(user);
            String refreshToken = tokenProvider.generateRefreshToken(user);

            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().name(),
                    tokenProvider.getExpirationMs()
            );
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username/email or password");
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                tokenProvider.getExpirationMs()
        );
    }

    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userDetailsService.getUserEntity(authentication.getName());
    }

    @Override
    public void run(String... args) {
        // Seed default users if database is fresh
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin", "admin@finance.local", passwordEncoder.encode("admin123"), Role.ROLE_ADMIN));
            userRepository.save(new User("analyst", "analyst@finance.local", passwordEncoder.encode("analyst123"), Role.ROLE_ANALYST));
            userRepository.save(new User("viewer", "viewer@finance.local", passwordEncoder.encode("viewer123"), Role.ROLE_VIEWER));
        }
    }
}
