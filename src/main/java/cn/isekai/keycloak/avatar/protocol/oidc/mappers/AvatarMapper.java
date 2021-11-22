package cn.isekai.keycloak.avatar.protocol.oidc.mappers;

import cn.isekai.keycloak.avatar.jpa.UserAvatarEntity;
import cn.isekai.keycloak.avatar.spi.ConfigService;
import cn.isekai.keycloak.avatar.storage.AvatarStorageProvider;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.*;

/**
 * Set the 'name' claim to be first + last name.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AvatarMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, org.keycloak.protocol.oidc.mappers.FullNameMapper.class);

    }

    public static final String PROVIDER_ID = "oidc-avatar-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "User's avatar";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Maps the user's avatar to the OpenID Connect 'avatar' and 'avatarSet' claim.";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        UserModel user = userSession.getUser();
        ConfigService config = keycloakSession.getProvider(ConfigService.class);
        AvatarStorageProvider defaultProvider = config.getStorageService(keycloakSession);
        String realmName = keycloakSession.getContext().getRealm().getName();

        String avatarAttribute = user.getFirstAttribute("avatar");
        if (avatarAttribute != null) {
            token.getOtherClaims().put("avatar", avatarAttribute);
        } else if(defaultProvider != null) {
            String defaultAvatar = defaultProvider
                    .getAvatarURL(realmName, "", "", config.getDefaultSize(), true);
            token.getOtherClaims().put("avatar", defaultAvatar);
        }

        UserAvatarEntity userAvatar = UserAvatarEntity.findByUserId(keycloakSession, user.getId());
        if (userAvatar != null) {
            String avatarId = userAvatar.getAvatarId();
            AvatarStorageProvider storageProvider = null;
            boolean tryFallback = false;
            if ("".equals(userAvatar.getStorage())) {
                tryFallback = true;
            } else {
                storageProvider = keycloakSession.getProvider(AvatarStorageProvider.class, userAvatar.getStorage());

                if (storageProvider == null) {
                    tryFallback = true;
                }
            }

            if (tryFallback) {
                if (!"".equals(userAvatar.getFallbackURL())) {
                    token.getOtherClaims().put("avatar", userAvatar.getFallbackURL());
                }
            } else {
                String mainAvatar = storageProvider
                        .getAvatarURL(realmName, user.getId(), avatarId, config.getDefaultSize(), false);
                token.getOtherClaims().put("avatar", mainAvatar);
                Map<String, String> avatarSet = new HashMap<String, String>();
                for (String sizeName : ConfigService.sizeList.keySet()) {
                    String avatar = storageProvider
                            .getAvatarURL(realmName, user.getId(), avatarId, sizeName, false);
                    String size = String.format("%dpx", ConfigService.sizeList.get(sizeName));
                    avatarSet.put(size, avatar);
                }
                token.getOtherClaims().put("avatarSet", avatarSet);
            }
        }
    }

    public static ProtocolMapperModel create(String name, boolean accessToken, boolean idToken, boolean userInfo) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "false");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "false");
        if (userInfo) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, "true");
        mapper.setConfig(config);
        return mapper;
    }
}
