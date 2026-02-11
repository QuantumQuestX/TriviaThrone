package de.quantumquest.triviathrone.server;

import de.quantumquest.triviathrone.auth.UserAccount;

public class Player {
    private final UserAccount userAccount;
    private String token;
    
    public Player(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public String getUsername() {
        return userAccount.getUsername();
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }
}
