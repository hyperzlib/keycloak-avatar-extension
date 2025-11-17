package cn.isekai.keycloak.avatar.jpa;

import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Collections;
import java.util.List;

public class UserAvatarJpaEntityProvider implements JpaEntityProvider {
    private static final Logger log = Logger.getLogger(UserAvatarJpaEntityProvider.class);

    @Override
    public List<Class<?>> getEntities() {
        return Collections.singletonList(UserAvatarEntity.class);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/changelog/user-avatar-changelog.xml";
    }

    @Override
    public void close() {
    }

    @Override
    public String getFactoryId() {
        return UserAvatarJpaEntityProviderFactory.ID;
    }
}