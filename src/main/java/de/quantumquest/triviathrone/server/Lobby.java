package de.quantumquest.triviathrone.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class Lobby {
    private final String code;
    private final String name;
    private final String hostUser;
    private final Set<String> users = Collections.synchronizedSet(new TreeSet<>());

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

    public String getLobbyCode() {
        return code;
    }

    public String getLobbyName() {
        return name;
    }

    public String getHostUsername() {
        return hostUser;
    }

    public Set<String> listUsernames() {
        return Set.copyOf(users);
    }
}
