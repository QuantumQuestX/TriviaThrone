package de.quantumquest.triviathrone.ws;

import de.quantumquest.triviathrone.auth.UserAccount;

import java.security.Principal;

public class WebSocketUser implements Principal {
    private final UserAccount user;

    public WebSocketUser(UserAccount user) {
        this.user = user;
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

    public UserAccount getUserAccount() {
        return user;
    }
}
