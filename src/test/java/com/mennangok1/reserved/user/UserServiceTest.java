package com.mennangok1.reserved.user;

import com.mennangok1.reserved.customerUser.CustomerUser;
import com.mennangok1.reserved.customerUser.CustomerUserRepository;
import com.mennangok1.reserved.role.Role;
import com.mennangok1.reserved.role.RoleRepository;
import com.mennangok1.reserved.roleAction.PermissionService;
import com.mennangok1.reserved.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CustomerUserRepository customerUserRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private UserService userService;

    private Role customerRole() {
        Role role = new Role();
        role.setId(3L);
        role.setName("CUSTOMER");
        return role;
    }

    private Role restaurantUserRole() {
        Role role = new Role();
        role.setId(2L);
        role.setName("RESTAURANT_USER");
        return role;
    }

    @Test
    void register_savesUserWithCustomerRole_andLowercasesEmail() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "Jane@Example.com", "password123", "5551234567");

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole()));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.register(request);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());

        assertThat(savedUser.getValue().getEmail()).isEqualTo("jane@example.com");
        assertThat(savedUser.getValue().getPassword()).isEqualTo("hashed");
        assertThat(savedUser.getValue().getRole().getName()).isEqualTo("CUSTOMER");
        assertThat(response.role()).isEqualTo("CUSTOMER");

        ArgumentCaptor<CustomerUser> savedCustomerUser = ArgumentCaptor.forClass(CustomerUser.class);
        verify(customerUserRepository).save(savedCustomerUser.capture());
        assertThat(savedCustomerUser.getValue().getUser()).isEqualTo(savedUser.getValue());
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@example.com", "password123", null);

        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(409);
    }

    @Test
    void registerRestaurantUser_assignsRestaurantUserRole() {
        RegisterRequest request = new RegisterRequest("Remy", "remy@example.com", "password123", null);

        when(userRepository.existsByEmail("remy@example.com")).thenReturn(false);
        when(roleRepository.findByName("RESTAURANT_USER")).thenReturn(Optional.of(restaurantUserRole()));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.registerRestaurantUser(request);

        assertThat(response.role()).isEqualTo("RESTAURANT_USER");
        verify(customerUserRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("jane@example.com", "password123");

        User user = new User();
        user.setEmail("jane@example.com");
        user.setRole(customerRole());
        UserPrincipal principal = new UserPrincipal(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("signed-jwt");

        var response = userService.login(request);

        assertThat(response.token()).isEqualTo("signed-jwt");
    }

    @Test
    void login_throwsUnauthorized_whenCredentialsInvalid() {
        LoginRequest request = new LoginRequest("jane@example.com", "wrong-password");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(401);
    }

    @Test
    void listAllUsers_returnsAllUsers_excludingPassword() {
        User caller = new User();
        caller.setId(1L);
        caller.setRole(customerRole());

        User admin = new User();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@reserved.local");
        admin.setPassword("hashed");
        admin.setRole(customerRole());

        when(userRepository.findAllWithRole()).thenReturn(List.of(admin));

        List<UserResponse> response = userService.listAllUsers(caller);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).email()).isEqualTo("admin@reserved.local");
        verify(permissionService).requirePermission(caller, "USER_READ");
    }

    @Test
    void listAllUsers_throwsForbidden_whenPermissionMissing() {
        User caller = new User();
        caller.setId(1L);
        caller.setRole(customerRole());

        doThrow(new ResponseStatusException(FORBIDDEN)).when(permissionService)
                .requirePermission(caller, "USER_READ");

        assertThatThrownBy(() -> userService.listAllUsers(caller))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value())
                .isEqualTo(403);

        verify(userRepository, never()).findAllWithRole();
    }

}
