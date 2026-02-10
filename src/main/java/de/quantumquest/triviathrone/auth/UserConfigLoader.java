package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.UserFile;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserConfigLoader {

    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();

    public UserConfigLoader(AuthProperties props) {
        load(props.getUserFile());
    }

    private void load(Path file) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setAllowRecursiveKeys(false);

        Constructor ctor = new Constructor(UserFile.class, options);
        Yaml yaml = new Yaml(ctor);

        try (InputStream in = Files.newInputStream(file)) {
            UserFile data = yaml.load(in);

            if (data == null || data.getUsers() == null) {
                throw new IllegalStateException("Users file is empty or malformed");
            }

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
}
