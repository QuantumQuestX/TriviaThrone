package quantumquest.triviathrone.auth;

import quantumquest.triviathrone.api.storage.IRepository;

import java.util.Set;

/**
 * Repository for user accounts and their independently mutable authorization attributes.
 *
 * @author TiJ
 * @since 2.0.0
 */
public interface IUserAccountRepository extends IRepository<UserAccount> {
    /**
     * Persists a new user account.
     */
    void insert(UserAccount account);

    /**
     * Replaces the password hash for the identified user.
     */
    void updatePassword(String username, String passwordHash);

    /**
     * Replaces the assigned roles for the identified user.
     */
    void updateRoles(String username, Set<String> roles);

    /**
     * Changes whether the identified user may authenticate.
     */
    void updateEnabled(String username, boolean enabled);

    /**
     * Changes whether the identified user is the bootstrap administrator.
     */
    void updateDefaultAdmin(String username, boolean defaultAdmin);

    /**
     * Removes the identified user account.
     */
    void delete(String username);
}
