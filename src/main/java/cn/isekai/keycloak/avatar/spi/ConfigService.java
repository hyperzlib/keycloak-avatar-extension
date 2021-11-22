package cn.isekai.keycloak.avatar.spi;

import cn.isekai.keycloak.avatar.storage.AvatarStorageProvider;
import lombok.Getter;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ConfigService implements Provider {
    public static Map<String, Integer> sizeList = new HashMap<String, Integer>() {
        {
            put("xs", 32);
            put("sm", 48);
            put("md", 64);
            put("lg", 128);
            put("xl", 256);
            put("xxl", 512);
        }
    };

    private static final int MAX_AGE = 1800;

    private final int maxAge;
    private final String defaultAvatar;
    private final boolean alwaysRedirect;
    private final String defaultSize;

    private final String storageServiceName;

    public ConfigService(Config.Scope scope) {
        this.maxAge = scope.getInt("maxAge", MAX_AGE);
        this.defaultAvatar = scope.get("defaultAvatar", "/");
        this.alwaysRedirect = scope.getBoolean("alwaysRedirect", false);
        this.defaultSize = scope.get("defaultSize", "lg");
        this.storageServiceName = scope.get("storageService", "file");
    }

    public AvatarStorageProvider getStorageService(KeycloakSession session) {
        return session.getProvider(AvatarStorageProvider.class, storageServiceName);
    }

    @Override
    public void close() {

    }
}
