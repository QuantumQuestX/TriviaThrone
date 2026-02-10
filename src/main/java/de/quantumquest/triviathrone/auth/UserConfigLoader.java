package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.UserFile;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserConfigLoader {

    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    private final Path file;
    private final Yaml yaml;

    public UserConfigLoader(AuthProperties props) {
        this.file = props.getUserFile();

        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setAllowRecursiveKeys(false);

        this.yaml = new Yaml(new Constructor(UserFile.class, options));

        load();
    }

    private void load() {
        try (InputStream in = Files.newInputStream(file)) {
            UserFile data = yaml.load(in);

            if (data == null || data.getUsers() == null) {
                throw new IllegalStateException("Users file is empty or malformed");
            }

            users.clear();
            for (UserAccount user : data.getUsers()) {
                users.put(user.getUsername(), user);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load users file", e);
        }
    }

    public UserAccount findByUsername(String username) {
        return users.get(username);
    }

    public synchronized void updatePassword(String username, String newHash) {
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

        Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

        try (Writer out = Files.newBufferedWriter(tempFile)) {
            yaml.dump(data, out);
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist users file", e);
        }
    }
}
