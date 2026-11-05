package de.quantumquest.triviathrone.auth;

import de.quantumquest.triviathrone.dto.UserFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserConfigLoader {
    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    private final Path file;
    private final Yaml yaml;
    private final String initialPassword;
    private final PasswordEncoder passwordEncoder;

    public UserConfigLoader(AuthProperties props, PasswordEncoder passwordEncoder) {
        this.file = props.userFile();
        this.initialPassword = props.initialPassword();
        this.passwordEncoder = passwordEncoder;

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        loaderOptions.setAllowRecursiveKeys(false);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setIndent(4);
        dumperOptions.setIndicatorIndent(2);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setSplitLines(false);

        Representer representer = new Representer(dumperOptions) {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                if (propertyValue instanceof Collection<?> c) {
                    if (c.isEmpty()) return null;
                }
                return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
            }
        };
        representer.getPropertyUtils().setSkipMissingProperties(true);
        representer.addClassTag(UserFile.class, Tag.MAP);
        representer.addClassTag(UserAccount.class, Tag.MAP);

        this.yaml = new Yaml(new Constructor(UserFile.class, loaderOptions), representer, dumperOptions);

        load();
    }

    private void load() {
        try (InputStream in = Files.newInputStream(file)) {
            UserFile data = yaml.load(in);

            if (data == null || data.getUsers() == null) {
                throw new IllegalStateException("Users file is empty or malformed");
            }

            users.clear();

            boolean modified = false;

            for (UserAccount user : data.getUsers()) {
                if (user.getPasswordHash() == null) {
                    if (initialPassword == null || initialPassword.isBlank()) {
                        throw new IllegalStateException("initialPassword is missing in " + file.getFileName());
                    }

                    String hashed = passwordEncoder.encode(initialPassword);
                    user.setPasswordHash(hashed);
                    modified = true;
                }

                users.put(user.getUsername(), user);
            }

            if (modified) persist();
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
