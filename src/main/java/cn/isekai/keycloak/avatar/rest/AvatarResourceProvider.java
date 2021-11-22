package cn.isekai.keycloak.avatar.rest;

import cn.isekai.keycloak.avatar.spi.ConfigService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class AvatarResourceProvider implements RealmResourceProvider {

    private final KeycloakSession keycloakSession;
    private final ConfigService config;

    public AvatarResourceProvider(KeycloakSession keycloakSession, ConfigService config) {
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
