package cn.isekai.keycloak.avatar.storage.fs;

import cn.isekai.keycloak.avatar.storage.AvatarStorageProvider;
import cn.isekai.keycloak.avatar.storage.AvatarStorageProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class FSAvatarStorageProviderFactory implements AvatarStorageProviderFactory {

    private FSAvatarStorageConfig config;

    @Override
    public AvatarStorageProvider create(KeycloakSession session) {
        try {
            return new FSAvatarStorageProvider(session, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void init(Config.Scope scope) {
        config = new FSAvatarStorageConfig(scope);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {}

    @Override
    public void close() {}

    @Override
    public String getId() {
        return "file";
    }
}
