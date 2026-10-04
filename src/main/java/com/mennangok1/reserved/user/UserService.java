package com.mennangok1.reserved.user;

import com.mennangok1.reserved.role.Role;
import com.mennangok1.reserved.role.RoleRepository;
import com.mennangok1.reserved.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private static final String DEFAULT_REGISTRATION_ROLE = "CUSTOMER";
    private static final String RESTAURANT_USER_ROLE = "RESTAURANT_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        return registerWithRole(request, DEFAULT_REGISTRATION_ROLE);
    }

    @Transactional
    public UserResponse registerRestaurantUser(RegisterRequest request) {
        return registerWithRole(request, RESTAURANT_USER_ROLE);
    }

    private UserResponse registerWithRole(RegisterRequest request, String roleName) {
        String email = request.email().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role is not seeded"));

        User user = new User();
        user.setName(request.name());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber());
        user.setRole(role);

        User saved = userRepository.save(user);

        return UserResponse.from(saved);
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().toLowerCase();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password())
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(token);
    }

}
