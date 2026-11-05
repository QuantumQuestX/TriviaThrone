package de.quantumquest.triviathrone.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Path userFile;
    private String jwtSecret;
    private long tokenValiditySeconds;
    private long refreshTokenValiditySeconds;
    private long refreshSessionMaxSeconds;
    private boolean refreshCookieSecure;
    private String initialPassword;

    public Path getUserFile() {
        return userFile;
    }

    public void setUserFile(Path userFile) {
        this.userFile = userFile;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenValiditySeconds() {
        return tokenValiditySeconds;
    }

    public void setTokenValiditySeconds(long tokenValiditySeconds) {
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    public void setRefreshTokenValiditySeconds(long refreshTokenValiditySeconds) {
        this.refreshTokenValiditySeconds = refreshTokenValiditySeconds;
    }

    public long getRefreshSessionMaxSeconds() {
        return refreshSessionMaxSeconds;
    }

    public void setRefreshSessionMaxSeconds(long refreshSessionMaxSeconds) {
        this.refreshSessionMaxSeconds = refreshSessionMaxSeconds;
    }

    public boolean isRefreshCookieSecure() {
        return refreshCookieSecure;
    }

    public void setRefreshCookieSecure(boolean refreshCookieSecure) {
        this.refreshCookieSecure = refreshCookieSecure;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}
