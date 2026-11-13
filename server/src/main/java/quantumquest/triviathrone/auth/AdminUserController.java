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

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    private final AdminUserService adminUsers;

    public AdminUserController(AdminUserService adminUsers) {
        this.adminUsers = Objects.requireNonNull(adminUsers, "adminUsers");
    }

    @GetMapping
    public List<AdminUserService.UserSummary> listUsers() {
        return adminUsers.listUsers();
    }

    @PostMapping
    public AdminUserService.UserSummary createUser(@RequestBody AdminUserService.CreateUserRequest request) {
        return adminUsers.createUser(request);
    }

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

    @DeleteMapping("/{username}")
    public void deleteUser(@PathVariable String username) {
        adminUsers.deleteUser(username);
    }

    public record UpdateRolesRequest(Set<String> roles) {
    }

    public record UpdateEnabledRequest(Boolean enabled) {
    }
}
