package quantumquest.triviathrone.auth;

import quantumquest.triviathrone.api.storage.IRepository;

import java.util.Set;

public interface IUserAccountRepository extends IRepository<UserAccount> {
    void insert(UserAccount account);

    void updatePassword(String username, String passwordHash);

    void updateRoles(String username, Set<String> roles);

    void updateEnabled(String username, boolean enabled);

    void updateDefaultAdmin(String username, boolean defaultAdmin);

    void delete(String username);
}
