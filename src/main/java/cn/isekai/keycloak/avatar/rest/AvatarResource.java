package cn.isekai.keycloak.avatar.rest;

import cn.isekai.keycloak.avatar.jpa.UserAvatarEntity;
import cn.isekai.keycloak.avatar.spi.ConfigService;
import cn.isekai.keycloak.avatar.storage.AvatarCropParams;
import cn.isekai.keycloak.avatar.storage.AvatarStorageProvider;
import cn.isekai.keycloak.avatar.utils.ResUtils;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.Cache;
import org.jboss.resteasy.reactive.NoCache;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static cn.isekai.keycloak.avatar.utils.ResUtils.*;

@Path("/avatar")
public class AvatarResource {
    private static final Logger logger = Logger.getLogger(AvatarResource.class);

    public static final String STATE_CHECKER_ATTRIBUTE = "state_checker";
    public static final String STATE_CHECKER_PARAMETER = "stateChecker";

    public static final int MAX_AGE = 120;
    public static String DEFAULT_SIZE = "lg";
    protected static final String RESPONSE_FORMAT_PARAMETER = "image";
    protected static final String AVATAR_IMAGE_PARAMETER = "image";
    protected static final String AVATAR_CROP_PARAMETER = "crop";

    private static final AppAuthManager appAuthManager = new AppAuthManager();
    private AdminPermissionEvaluator realmAuth;

    protected KeycloakSession session;
    private ConfigService config;

    // to fix "Unsatisfied dependency" problem on quarkus
    public AvatarResource() {

    }

    public AvatarResource(KeycloakSession session, ConfigService config) {
        this.session = session;
        this.config = config;

        DEFAULT_SIZE = config.getDefaultSize();
    }

    public AvatarStorageProvider getAvatarStorageProvider() {
        return this.config.getStorageService(this.session);
    }

    public String generateAvatarId(String realmId, String userId) {
        String hash = ResUtils.md5(realmId + userId);
        if (hash == null) {
            return userId;
        } else {
            return String.format("%s-%s-%s-%s",
                    hash.substring(0, 8),hash.substring(8, 16), hash.substring(16, 24), hash.substring(24, 32));
        }
    }

    private static AuthenticationManager.AuthResult checkUserAuthentication(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        AuthenticationManager.AuthResult authResult = appAuthManager.authenticateIdentityCookie(session, realm);
        if (authResult == null) {
            authResult = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        }
        return authResult;
    }

    private static AdminAuth checkAdminAuthentication(KeycloakSession session) {
        AuthenticationManager.AuthResult authResult = checkUserAuthentication(session);
        if (authResult != null) {
            return new AdminAuth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(),
                    session.getContext().getClient());
        } else {
            return null;
        }
    }

    private static ByteArrayInputStream convertImage(InputStream inputStream) {
        try {
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", byteArrayOut);
            return new ByteArrayInputStream(byteArrayOut.toByteArray());
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    public Response getUserAvatar(UserModel user, String size, boolean isJson) {
        if (size == null) size = config.getDefaultSize();
        if (user == null) return responseNotFound(isJson);

        if (!ConfigService.sizeList.containsKey(size)) {
            size = config.getDefaultSize();
        }

        String realmId = session.getContext().getRealm().getId();
        String realmName = session.getContext().getRealm().getName();
        UserAvatarEntity userAvatar = UserAvatarEntity.findByUserId(session, user.getId());
        String avatarId;
        AvatarStorageProvider storageProvider = getAvatarStorageProvider();
        if (userAvatar != null) {
            avatarId = userAvatar.getAvatarId();
            boolean tryFallback = false;
            if ("".equals(userAvatar.getStorage())) {
                tryFallback = true;
            } else if (!Objects.equals(userAvatar.getStorage(), config.getStorageServiceName())) {
                storageProvider = session.getProvider(AvatarStorageProvider.class, userAvatar.getStorage());

                if (storageProvider == null) {
                    tryFallback = true;
                }
            }

            if (tryFallback) {
                if ("".equals(userAvatar.getFallbackURL())) {
                    logger.error("Cannot find storage provider: " + userAvatar.getStorage());
                    return responseInternalServerError(isJson);
                }

                return responseRedirectTo(userAvatar.getFallbackURL());
            }
        } else {
            avatarId = generateAvatarId(realmId, user.getId());
        }

        if (isJson) {
            String avatarURL = storageProvider
                    .getAvatarURL(realmName, user.getId(), avatarId, size, false);
            String avatarTpl = storageProvider.getAvatarURLTemplate(realmName, user.getId(), avatarId);

            if (avatarURL != null) {
                Map<String, Object> res = new HashMap<String, Object>();
                res.put("status", 1);
                res.put("avatar", avatarURL);
                res.put("avatar_tpl", avatarTpl);
                res.put("provided_size", ConfigService.sizeList);

                return responseJson(res);
            }
        } else {
            if (config.isAlwaysRedirect()) {
                String avatarURL = storageProvider
                        .getAvatarURL(realmName, user.getId(), avatarId, size, true);
                if (avatarURL == null && config.getDefaultAvatar() != null) {
                    return responseRedirectTo(config.getDefaultAvatar());
                }
                if (avatarURL != null) {
                    return responseRedirectTo(avatarURL);
                }
            } else {
                try {
                    InputStream stream = storageProvider
                            .loadAvatarImage(realmName, user.getId(), avatarId, size, true);
                    if (stream != null) {
                        return Response.ok(stream).type("image/png").build();
                    } else {
                        return responseNotFound(false);
                    }
                } catch (Exception e) {
                    logger.error("Get avatar error", e);
                }
            }
        }
        return responseNotFound(isJson);
    }

    public Response uploadUserAvatar(UserModel user, String avatarCropParam, FileUpload imageFile) {
        if (user == null) return Response.status(404).build();
        String realmId = session.getContext().getRealm().getId();
        String realmName = session.getContext().getRealm().getName();
        String userId = user.getId();

        String avatarId = generateAvatarId(realmId, userId);
        try {
            AvatarCropParams cropParams = null;
            if(avatarCropParam != null) cropParams = new AvatarCropParams(avatarCropParam);

            InputStream imageInputStream = Files.newInputStream(imageFile.uploadedFile());
            ByteArrayInputStream byteArrayIn = convertImage(imageInputStream);

            AvatarStorageProvider storageProvider = getAvatarStorageProvider();
            storageProvider.saveOriginalAvatarImage(realmName, user.getId(), avatarId, byteArrayIn, cropParams,
                    ConfigService.sizeList);

            // save avatar info to database
            String avatarUrl = storageProvider.getAvatarURL(realmName, user.getId(), avatarId, DEFAULT_SIZE, false);
            Date modifiedTime = new Date();
            String eTag = ResUtils.md5(String.valueOf(modifiedTime.getTime()));

            UserAvatarEntity userAvatar = new UserAvatarEntity();
            userAvatar.setUserId(userId);
            userAvatar.setAvatarId(avatarId);
            userAvatar.setUpdatedTime(modifiedTime.getTime());
            userAvatar.setStorage(config.getStorageServiceName());
            userAvatar.setETag(eTag);
            userAvatar.setFallbackURL(avatarUrl);
            UserAvatarEntity.save(session, userAvatar);

            try {
                user.setSingleAttribute("avatarId", avatarId);
                user.setSingleAttribute("avatar", avatarUrl);
            } catch(Exception ex) { // user attributes can only accept 255 chars
                user.setSingleAttribute("avatar", "provided");
            }

            Map<String, Object> res = new HashMap<String, Object>();
            res.put("status", 1);
            res.put("avatar", avatarUrl);
            res.put("avatarId", avatarId);
            return responseJson(res);
        } catch (Exception ex) {
            logger.error("Cannot save avatar", ex);
            return responseInternalServerError(true);
        }
    }

    @GET
    @Path("/by-username/{username}")
    @Cache(maxAge = MAX_AGE)
    public Response getUserAvatarByUserName(@PathParam("username") String userName,
                                            @QueryParam("size") String size,
                                            @QueryParam("format") String format) {
        UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), userName);
        boolean isJson = "json".equals(format);
        if (isJson && user == null) {
            return responseNotFound(true);
        }
        return getUserAvatar(user, size, isJson);
    }

    @POST
    @Path("/by-username/{username}")
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadUserAvatarByUserName(@PathParam("username") String userName,
                                               @FormParam(AVATAR_CROP_PARAMETER) String avatarCropParam,
                                               @RestForm(AVATAR_IMAGE_PARAMETER) FileUpload imagePart) {
        AdminAuth auth = checkAdminAuthentication(session);
        if (auth == null) {
            return responseNotAuthorized(true);
        }

        realmAuth = AdminPermissions.evaluator(session, auth.getRealm(), auth);
        if (cannotManageUsers(realmAuth)) {
            return responseNotAuthorized(true);
        }

        UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), userName);
        if (user == null) {
            return responseNotFound(true);
        }
        return uploadUserAvatar(user, avatarCropParam, imagePart);
    }

    @GET
    @Path("/by-userid/{user_id}")
    @Cache(maxAge = MAX_AGE)
    public Response getUserAvatarById(@PathParam("user_id") String userId,
                                      @QueryParam("size") String size,
                                      @QueryParam("format") String format) {
        boolean isJson = "json".equals(format);
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if (isJson && user == null) {
            return responseNotFound(true);
        }
        return getUserAvatar(user, size, isJson);
    }

    @POST
    @Path("/by-userid/{user_id}")
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadUserAvatarById(@PathParam("user_id") String userId,
                                         @FormParam(AVATAR_CROP_PARAMETER) String avatarCropParam,
                                         @RestForm(AVATAR_IMAGE_PARAMETER) FileUpload imagePart) {
        AdminAuth auth = checkAdminAuthentication(session);
        if (auth == null) {
            return responseNotAuthorized(true);
        }

        realmAuth = AdminPermissions.evaluator(session, auth.getRealm(), auth);
        if (cannotManageUsers(realmAuth)) {
            return responseNotAuthorized(true);
        }

        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if (user == null) {
            return responseNotFound(true);
        }
        return uploadUserAvatar(user, avatarCropParam, imagePart);
    }

    @DELETE
    @Path("/by-userid/{user_id}")
    @NoCache
    public Response deleteUserAvatar(@PathParam("user_id") String userId) {
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        if (user == null) {
            return responseNotFound(true);
        }

        AdminAuth auth = checkAdminAuthentication(session);
        if (auth == null) {
            return responseNotAuthorized(true);
        }

        realmAuth = AdminPermissions.evaluator(session, auth.getRealm(), auth);
        if (cannotManageUsers(realmAuth)) {
            return responseNotAuthorized(true);
        }

        String realmId = session.getContext().getRealm().getId();
        UserAvatarEntity userAvatar = UserAvatarEntity.findByUserId(session, userId);
        String avatarId;
        AvatarStorageProvider storageProvider = getAvatarStorageProvider();
        if (userAvatar != null) {
            avatarId = userAvatar.getAvatarId();
            if (!"".equals(userAvatar.getStorage()) &&
                    !Objects.equals(userAvatar.getStorage(), config.getStorageServiceName())) {
                storageProvider = session.getProvider(AvatarStorageProvider.class, userAvatar.getStorage());

                if (storageProvider == null) {
                    logger.error("Cannot find storage provider: " + userAvatar.getStorage());
                    return responseInternalServerError(true);
                }
            }
        } else {
            avatarId = generateAvatarId(realmId, userId);
        }

        if (storageProvider.removeAvatar(realmId, userId, avatarId, ConfigService.sizeList.keySet())) {
            user.removeAttribute("avatar");
            user.removeAttribute("avatarId");
            return Response.ok().type(MediaType.APPLICATION_JSON).build();
        }
        return responseInternalServerError(true);
    }

    @GET
    @Path("")
    @Cache(maxAge = MAX_AGE)
    public Response getCurrentUserAvatar(@QueryParam("size") String size,
                                         @QueryParam("format") String format) {
        boolean isJson = "json".equals(format);
        if (size == null) size = DEFAULT_SIZE;

        AuthenticationManager.AuthResult auth = checkUserAuthentication(session);
        if (auth == null) {
            if (isJson) {
                return responseNotAuthorized(true);
            } else {
                return getDefaultAvatar(size, format);
            }
        }

        return getUserAvatar(auth.getUser(), size, isJson);
    }

    @POST
    @Path("")
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadCurrentUserAvatarImage(@FormParam(AVATAR_CROP_PARAMETER) String avatarCropParam,
                                                 @RestForm(AVATAR_IMAGE_PARAMETER) FileUpload imagePart,
                                                 @Context UriInfo uriInfo) {
        AuthenticationManager.AuthResult auth = checkUserAuthentication(session);
        if (auth == null) {
            return responseNotAuthorized(true);
        }

        UserModel user = auth.getUser();
        if (user == null) {
            return responseNotFound(true);
        }

        return uploadUserAvatar(user, avatarCropParam, imagePart);
    }

    @GET
    @Path("/default")
    @Cache(maxAge = MAX_AGE)
    public Response getDefaultAvatar(@QueryParam("size") String size, @QueryParam("format") String format) {
        if (size == null) size = DEFAULT_SIZE;
        boolean isJson = "json".equals(format);
        String realmName = session.getContext().getRealm().getName();

        if (isJson) {
            String defaultAvatar = getAvatarStorageProvider().getAvatarURL(realmName, "", "", size, true);
            if (defaultAvatar != null) {
                Map<String, Object> res = new HashMap<String, Object>();
                res.put("status", 1);
                res.put("avatar", defaultAvatar);
                return responseJson(res);
            }
        } else {
            if (config.isAlwaysRedirect()) {
                if (config.getDefaultAvatar() != null) {
                    String defaultAvatar = getAvatarStorageProvider().getAvatarURL(realmName, "", "", size, true);
                    return responseRedirectTo(defaultAvatar);
                }
            } else {
                try {
                    InputStream stream = getAvatarStorageProvider().
                            loadAvatarImage(realmName, "", "", size, true);
                    return Response.ok(stream).type("image/png").build();
                } catch (Exception e) {
                    logger.error("Get avatar error", e);
                }
            }
        }
        return responseNotFound(isJson);
    }

    private boolean isValidStateChecker(@FormParam(STATE_CHECKER_PARAMETER) String actualStateChecker) {
        try {
            String requiredStateChecker = (String) session.getAttribute(STATE_CHECKER_ATTRIBUTE);

            return Objects.equals(requiredStateChecker, actualStateChecker);
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean cannotManageUsers(AdminPermissionEvaluator realmAuth) {
        return !realmAuth.users().canManage();
    }

    @Path("/res")
    public Object getProviderResource() {
        AvatarStorageProvider sp = getAvatarStorageProvider();
        if (sp == null) {
            logger.warn("Cannot get avatar storage provider");
            return responseNotFound(false);
        }

        Object res = sp.getResource();
        if (res == null) {
            return responseNotFound(false);
        }
        return res;
    }
}
