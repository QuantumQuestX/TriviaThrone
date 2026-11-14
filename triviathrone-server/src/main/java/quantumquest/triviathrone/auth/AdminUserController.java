package quantumquest.triviathrone.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Provides administrator-only HTTP endpoints for managing user accounts.
 *
 * @author TiJ
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    /**
     * Holds the admi sers used by this component.
     */
    private final AdminUserService adminUsers;

    /**
     * Creates a AdminUserController.
     *
     * @param adminUsers the admin users
     */
    public AdminUserController(AdminUserService adminUsers) {
        this.adminUsers = Objects.requireNonNull(adminUsers, "adminUsers");
    }

    /**
     * Returns summaries for every configured user account.
     *
     * @return summaries for every configured user account
     */
    @GetMapping
    public List<AdminUserService.UserSummary> listUsers() {
        return adminUsers.listUsers();
    }

    /**
     * Creates a validated user account.
     *
     * @param request the request payload
     * @return the created a validated user account
     */
    @PostMapping
    public AdminUserService.UserSummary createUser(@RequestBody AdminUserService.CreateUserRequest request) {
        return adminUsers.createUser(request);
    }

    /**
     * Persists and applies an account's roles.
     *
     * @param username the account username
     * @param request  the request payload
     * @return the updated account summary
     */
    @PutMapping("/{username}/roles")
    public AdminUserService.UserSummary updateRoles(
            @PathVariable String username,
            @RequestBody UpdateRolesRequest request
    ) {
        UpdateRolesRequest roleRequest = Objects.requireNonNull(request, "request");
        if (roleRequest.roles() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles are required");
        }
        return adminUsers.updateRoles(username, roleRequest.roles());
    }

    /**
     * Persists and applies an account's enabled state.
     *
     * @param username the account username
     * @param request  the request payload
     * @return the updated account summary
     */
    @PutMapping("/{username}/enabled")
    public AdminUserService.UserSummary updateEnabled(
            @PathVariable String username,
            @RequestBody UpdateEnabledRequest request
    ) {
        UpdateEnabledRequest enabledRequest = Objects.requireNonNull(request, "request");
        if (enabledRequest.enabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enabled state is required");
        }
        return adminUsers.updateEnabled(username, enabledRequest.enabled());
    }

    /**
     * Deletes a non-default account.
     *
     * @param username the account username
     */
    @DeleteMapping("/{username}")
    public void deleteUser(@PathVariable String username) {
        adminUsers.deleteUser(username);
    }

    /**
     * Carries requested roles for an account.
     *
     * @param roles the replacement role set
     * @author TiJ
     * @since 2.0.0
     */
    public record UpdateRolesRequest(Set<String> roles) {
    }

    /**
     * Carries the requested enabled state.
     *
     * @param enabled the replacement enabled state
     * @author TiJ
     * @since 2.0.0
     */
    public record UpdateEnabledRequest(Boolean enabled) {
    }
}
