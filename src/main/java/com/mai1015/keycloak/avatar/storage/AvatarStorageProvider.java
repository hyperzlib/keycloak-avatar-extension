package com.mai1015.keycloak.avatar.storage;

import org.keycloak.provider.Provider;

import java.io.InputStream;
import java.net.URI;

public interface AvatarStorageProvider extends Provider {

    boolean hasAvatar(String realmName, String userId);

    void saveAvatarImage(String realmName, String userId, InputStream input);

    InputStream loadAvatarImage(String realmName, String userId);

    URI getAvatarURI(String realmName, String userId);

    boolean removeAvatar(String realmName, String userId);
}
