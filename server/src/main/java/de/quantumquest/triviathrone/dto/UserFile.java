package de.quantumquest.triviathrone.dto;

import de.quantumquest.triviathrone.auth.UserAccount;

import java.util.ArrayList;
import java.util.List;

public class UserFile {
    private List<UserAccount> users = new ArrayList<>();

    public List<UserAccount> getUsers() {
        return users;
    }

    public void setUsers(List<UserAccount> users) {
        this.users = users;
    }
}
