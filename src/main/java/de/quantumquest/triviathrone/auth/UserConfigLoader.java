package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.UserFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserConfigLoader {
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    private final UserConfigFileStore userFileStore;
    private final String initialPassword;
    private final PasswordEncoder passwordEncoder;

    public UserConfigLoader(
            AuthProperties props,
            PasswordEncoder passwordEncoder,
            UserConfigFileStore userFileStore
    ) {
        AuthProperties properties = Objects.requireNonNull(props, "props");
        this.userFileStore = Objects.requireNonNull(userFileStore, "userFileStore");
        this.initialPassword = properties.initialPassword();
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder");

        load();
    }

    private void load() {
        UserFile data = userFileStore.load();
        if (data.getUsers() == null) {
            throw new IllegalStateException("Users file is empty or malformed");
        }

        users.clear();
        boolean modified = false;
        for (UserAccount user : data.getUsers()) {
            if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
                throw new IllegalStateException("Users file contains an invalid user");
            }
            if (user.getPasswordHash() == null) {
                if (initialPassword == null || initialPassword.isBlank()) {
                    throw new IllegalStateException(
                            "initialPassword is missing in " + userFileStore.fileName()
                    );
                }

                user.setPasswordHash(passwordEncoder.encode(initialPassword));
                modified = true;
            }
            users.put(user.getUsername(), user);
        }

        if (modified) persist();
    }

    public UserAccount findByUsername(String username) {
        return users.get(Objects.requireNonNull(username, "username"));
    }

    public synchronized void updatePassword(String username, String newHash) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(newHash, "newHash");
        UserAccount user = users.get(username);

        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        user.setPasswordHash(newHash);
        persist();
    }

    private void persist() {
        UserFile data = new UserFile();
        data.setUsers(new ArrayList<>(users.values()));
        userFileStore.save(data);
    }
}
