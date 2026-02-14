package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Lobby {
    private final String code;
    private final String name;
    private final String hostUser;
    private final Set<String> users = Collections.synchronizedSet(new HashSet<>());

    public Lobby(String code, String name, String hostUser) {
        this.code = code;
        this.name = name;
        this.hostUser = hostUser;
        addUser(hostUser);
    }

    public void addUser(String username) {
        users.add(username);
    }

    public void removeUser(String username) {
        users.remove(username);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getHostUser() {
        return hostUser;
    }

    public Set<String> listUsernames() {
        return Set.copyOf(users);
    }
}
