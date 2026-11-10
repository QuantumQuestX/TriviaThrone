package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.api.storage.IRepository;

public interface IUserAccountRepository extends IRepository<UserAccount> {
    void updatePassword(String username, String passwordHash);
}
