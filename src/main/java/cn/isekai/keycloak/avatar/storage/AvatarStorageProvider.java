package cn.isekai.keycloak.avatar.storage;

import org.keycloak.provider.Provider;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

public interface AvatarStorageProvider extends Provider {

    boolean hasAvatar(String realmName, String userId, String avatarId, String defaultSize);

    boolean saveAvatarImage(String realmName, String userId, String avatarId, InputStream input, String size);

    boolean saveOriginalAvatarImage(String realmName, String userId, String avatarId, InputStream input,
                                    AvatarCropParams cropParams, Map<String, Integer> sizeList);

    InputStream loadAvatarImage(String realmName, String userId, String avatarId, String size, boolean fallbackDefault);

    String getAvatarURL(String realmName, String userId, String avatarId, String size, boolean fallbackDefault);

    String getAvatarURLTemplate(String realmName, String userId, String avatarId);

    boolean removeAvatar(String realmName, String userId, String avatarId, Iterable<String> sizeList);

    Object getResource();
}
