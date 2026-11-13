package de.quantumquest.triviathrone.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class AdminUserServiceTest {
    private final UserConfigLoader users = mock(UserConfigLoader.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final AdminUserService service = new AdminUserService(users, passwordEncoder, refreshTokens);

    @Test
    void createsAdminWithOnlyRequestedRolesAndStoresOnlyEncodedPassword() {
        when(passwordEncoder.encode("SecurePassword#123")).thenReturn("encoded-password");
        UserAccount defaultAdmin = account(UserConfigLoader.DEFAULT_ADMIN_USERNAME,
                Set.of(UserAccount.ROLE_USER, UserAccount.ROLE_ADMIN));
        defaultAdmin.setDefaultAdmin(true);
        when(users.findByUsername(UserConfigLoader.DEFAULT_ADMIN_USERNAME)).thenReturn(defaultAdmin);
        doAnswer(invocation -> {
            defaultAdmin.setEnabled(false);
            return null;
        }).when(users).reconcileDefaultAdmin();

        AdminUserService.UserSummary created = service.createUser(new AdminUserService.CreateUserRequest(
                "  player  ", "  Player Name ", "SecurePassword#123", Set.of(UserAccount.ROLE_ADMIN)
        ));

        assertEquals("player", created.username());
        assertEquals("Player Name", created.displayName());
        assertEquals(Set.of(UserAccount.ROLE_ADMIN), created.roles());
        assertTrue(created.enabled());
        assertFalse(defaultAdmin.isEnabled());
        verify(users).createUser(any(UserAccount.class));
        verify(refreshTokens).revokeAll(UserConfigLoader.DEFAULT_ADMIN_USERNAME);
    }

    @Test
    void preventsDemotingOrDeletingTheDefaultAdmin() {
        UserAccount admin = account("admin", Set.of(UserAccount.ROLE_USER, UserAccount.ROLE_ADMIN));
        admin.setDefaultAdmin(true);
        when(users.findByUsername("admin")).thenReturn(admin);

        ResponseStatusException demotion = assertThrows(
                ResponseStatusException.class,
                () -> service.updateRoles("admin", Set.of(UserAccount.ROLE_USER))
        );
        ResponseStatusException deletion = assertThrows(
                ResponseStatusException.class,
                () -> service.deleteUser("admin")
        );

        assertEquals(HttpStatus.CONFLICT, demotion.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, deletion.getStatusCode());
        verify(users, never()).updateRoles("admin", Set.of(UserAccount.ROLE_USER));
        verify(users, never()).deleteUser("admin");
    }

    @Test
    void preventsRemovingUserRoleFromDefaultAdmin() {
        UserAccount admin = account("admin", Set.of(UserAccount.ROLE_USER, UserAccount.ROLE_ADMIN));
        admin.setDefaultAdmin(true);
        when(users.findByUsername("admin")).thenReturn(admin);

        assertThrows(ResponseStatusException.class, () -> service.updateRoles("admin", Set.of(UserAccount.ROLE_ADMIN)));
        verify(users, never()).updateRoles("admin", Set.of(UserAccount.ROLE_ADMIN));
    }

    @Test
    void allowsRemovingUserRoleFromOtherAccountsAndRevokesTheirSessions() {
        UserAccount player = account("player", Set.of(UserAccount.ROLE_USER));
        when(users.findByUsername("player")).thenReturn(player);
        doAnswer(invocation -> {
            player.setRoles(invocation.getArgument(1));
            return null;
        }).when(users).updateRoles("player", Set.of());

        AdminUserService.UserSummary updated = service.updateRoles("player", Set.of());

        assertTrue(updated.roles().isEmpty());
        verify(users).updateRoles("player", Set.of());
        verify(refreshTokens).revokeAll("player");
        verify(users).reconcileDefaultAdmin();
    }

    @Test
    void reEnablesDefaultAdminWhenAnotherAdminIsDisabled() {
        UserAccount secondaryAdmin = account("secondary", Set.of(UserAccount.ROLE_USER, UserAccount.ROLE_ADMIN));
        UserAccount defaultAdmin = account(UserConfigLoader.DEFAULT_ADMIN_USERNAME,
                Set.of(UserAccount.ROLE_USER, UserAccount.ROLE_ADMIN));
        defaultAdmin.setDefaultAdmin(true);
        defaultAdmin.setEnabled(false);
        when(users.findByUsername("secondary")).thenReturn(secondaryAdmin);
        when(users.findByUsername(UserConfigLoader.DEFAULT_ADMIN_USERNAME)).thenReturn(defaultAdmin);
        doAnswer(invocation -> {
            secondaryAdmin.setEnabled(invocation.getArgument(1));
            return null;
        }).when(users).updateEnabled("secondary", false);
        doAnswer(invocation -> {
            defaultAdmin.setEnabled(true);
            return null;
        }).when(users).reconcileDefaultAdmin();
        AdminUserService.UserSummary disabled = service.updateEnabled("secondary", false);

        assertFalse(disabled.enabled());
        assertTrue(defaultAdmin.isEnabled());
        verify(refreshTokens).revokeAll("secondary");
    }

    private static UserAccount account(String username, Set<String> roles) {
        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setRoles(roles);
        return account;
    }
}
