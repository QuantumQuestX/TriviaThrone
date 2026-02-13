package de.quantumquest.triviathrone.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private Path userFile;
    private String jwtSecret;
    private long tokenValiditySeconds;
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

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}
