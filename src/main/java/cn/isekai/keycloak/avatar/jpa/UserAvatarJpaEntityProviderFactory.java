package cn.isekai.keycloak.avatar.jpa;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class UserAvatarJpaEntityProviderFactory implements JpaEntityProviderFactory {
    private static final Logger log = Logger.getLogger(UserAvatarJpaEntityProviderFactory.class);
    public static String ID = "userAvatarEntityProviderFactory";

    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new UserAvatarJpaEntityProvider();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}
