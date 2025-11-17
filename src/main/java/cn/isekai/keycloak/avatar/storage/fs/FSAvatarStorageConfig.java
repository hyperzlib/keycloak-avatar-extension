package cn.isekai.keycloak.avatar.storage.fs;

import cn.isekai.keycloak.avatar.utils.ResUtils;
import lombok.Getter;
import org.keycloak.Config;

@Getter
public class FSAvatarStorageConfig {
    public final String urlPathFormat = "/{realm}/avatar/res/{avatar_id}/avatar-{size}.png";
    public final String defaultAvatarUrlPathFormat = "/{realm}/avatar/res/default.png";

    public final String storageRoot;
    public final String filePathFormat;
    public final String urlRoot;
    public final String defaultAvatar;

    public FSAvatarStorageConfig(Config.Scope scope){
        this.storageRoot = scope.get("root", "storage");
        this.filePathFormat = scope.get("file-path", "/{realm}/avatar/{avatar_id}/avatar-{size}.png");
        this.urlRoot = scope.get("baseurl", "/realms/");
        this.defaultAvatar = scope.get("default-avatar", "/{realm}/avatar/default.png");
    }

    public String getAvatarPath(String realm, String userId, String avatarId, String size){
        return getPath(realm, userId, avatarId, size, filePathFormat, storageRoot);
    }

    public String getAvatarURL(String realm, String userId, String avatarId, String size){
        return getPath(realm, userId, avatarId, size, urlPathFormat, urlRoot);
    }

    private String getAvatarId(String realm, String userId) {
        String avatarId = ResUtils.md5(realm + userId);
        if (avatarId == null) avatarId = userId;
        return avatarId;
    }

    private String getPath(String realm, String userId, String avatarId, String size, String tpl, String urlRoot) {
        StringBuilder path = new StringBuilder();
        if ("/".equals(urlRoot.substring(urlRoot.length() - 1))) { //如果末尾有斜杠则去除
            urlRoot = urlRoot.substring(0, urlRoot.length() - 1);
        }
        path.append(urlRoot);

        path.append(tpl.replace("{realm}", realm).replace("{user_id}", userId)
                .replace("{avatar_id}", avatarId).replace("{size}", size));
        return path.toString();
    }

    public String getDefaultAvatarPath(String realm, String size){
        return getPath(realm, "", "", size, defaultAvatar, storageRoot);
    }

    public String getDefaultAvatarURL(String realm, String size){
        return getPath(realm, "", "", size, defaultAvatarUrlPathFormat, urlRoot);
    }
}
