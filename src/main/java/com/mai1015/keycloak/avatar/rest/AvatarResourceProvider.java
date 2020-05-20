package com.mai1015.keycloak.avatar.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class AvatarResourceProvider implements RealmResourceProvider {

    private final KeycloakSession keycloakSession;
    private final AvatarConfig config;

    public AvatarResourceProvider(KeycloakSession keycloakSession, AvatarConfig config) {
        this.keycloakSession = keycloakSession;
        this.config = config;
    }

    @Override
    public Object getResource() {
        return new AvatarResource(keycloakSession, config);
    }

    @Override
    public void close() {}
}
