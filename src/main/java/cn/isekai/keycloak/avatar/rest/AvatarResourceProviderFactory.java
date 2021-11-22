package cn.isekai.keycloak.avatar.rest;

import cn.isekai.keycloak.avatar.spi.ConfigService;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class AvatarResourceProviderFactory implements RealmResourceProviderFactory {
    public static final String ID = "avatar";

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        ConfigService config = keycloakSession.getProvider(ConfigService.class);
        return new AvatarResourceProvider(keycloakSession, config);
    }

    @Override
    public void init(Config.Scope scope) {

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
