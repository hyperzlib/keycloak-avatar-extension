package com.mai1015.keycloak.avatar.storage.minio;

import com.mai1015.keycloak.avatar.storage.AvatarStorageProvider;
import com.mai1015.keycloak.avatar.storage.AvatarStorageProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MinioAvatarStorageProviderFactory implements AvatarStorageProviderFactory {

    //TODO remove default settings
    private static final String DEFAULT_SERVER_URL = "http://localhost:9000";
    private static final String DEFAULT_ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String DEFAULT_SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

    private MinioConfig minioConfig;

    @Override
    public AvatarStorageProvider create(KeycloakSession session) {
        try {
            return new MinioAvatarStorageProvider(minioConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void init(Config.Scope config) {

        String serverUrl = config.get("server-url", DEFAULT_SERVER_URL);
        String accessKey = config.get("access-key", DEFAULT_ACCESS_KEY);
        String secretKey = config.get("secret-key", DEFAULT_SECRET_KEY);

        this.minioConfig = new MinioConfig(serverUrl, accessKey, secretKey);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getId() {
        return "avatar-storage-minio";
    }
}
