package com.mai1015.keycloak.avatar.rest;

import java.net.URI;
import java.net.URISyntaxException;

public class AvatarConfig {
    private int maxAge;
    private URI defaultAvatar;
    private boolean alwaysRedirect;
    private int maxSize;

    public AvatarConfig(int maxAge, String defaultAvatar, boolean alwaysRedirect, int size) {
        this.maxAge = maxAge;
        try {
            if (defaultAvatar == null)
                this.defaultAvatar = null;
            else
                this.defaultAvatar = new URI(defaultAvatar);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            this.defaultAvatar = null;
        }
        this.alwaysRedirect = alwaysRedirect;
        this.maxSize = size;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public URI getDefaultAvatar() {
        return defaultAvatar;
    }

    public boolean isAlwaysRedirect() {
        return alwaysRedirect;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
