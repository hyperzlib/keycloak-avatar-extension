package com.mai1015.keycloak.avatar.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class AvatarResourceProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "avatar-provider";

    private static final int MAX_AGE = 1800;

//    private AvatarResourceProvider avatarResourceProvider;
    private AvatarConfig config;

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
//        if (avatarResourceProvider == null) {
//            avatarResourceProvider = new AvatarResourceProvider(keycloakSession);
//        }
//        return avatarResourceProvider;
        return new AvatarResourceProvider(keycloakSession, config);
    }

    @Override
    public void init(Config.Scope scope) {
        int maxAge = scope.getInt("max-age", MAX_AGE);
        String avatar = scope.get("default-avatar", null);
        boolean redirect = scope.getBoolean("always-redirect", true);
        int size = scope.getInt("max-size", 128);

        config = new AvatarConfig(maxAge, avatar, redirect, size);
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return ID;
    }
}
