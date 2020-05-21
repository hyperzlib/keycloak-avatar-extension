package com.mai1015.keycloak.avatar.rest;

import com.mai1015.keycloak.avatar.storage.AvatarStorageProvider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.Cache;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

public class AvatarResource {
    private static final Logger logger = Logger.getLogger(AvatarResource.class);

    public static final String STATE_CHECKER_ATTRIBUTE = "state_checker";
    public static final String STATE_CHECKER_PARAMETER = "stateChecker";
    protected static final String AVATAR_IMAGE_PARAMETER = "image";

    private static final AppAuthManager appAuthManager = new AppAuthManager();
    private AdminPermissionEvaluator realmAuth;

    protected KeycloakSession session;
    private final AvatarConfig config;

    public AvatarResource(KeycloakSession session, AvatarConfig config) {
        this.session = session;
        this.config = config;
    }

    public AvatarStorageProvider getAvatarStorageProvider() {
        return session.getProvider(AvatarStorageProvider.class);
    }

    protected static Response badRequest() {
        return Response.status(Response.Status.BAD_REQUEST).build();
    }

    private static AuthenticationManager.AuthResult checkCookieAuthentication(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        AuthenticationManager.AuthResult authResult = appAuthManager.authenticateIdentityCookie(session, realm);
        if (authResult == null) {
            throw new NotAuthorizedException("Not login");
        }
        return authResult;
    }

    private static AdminAuth checkBearerAuthentication(KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        AuthenticationManager.AuthResult authResult = appAuthManager.authenticateBearerToken(session, realm);
        if (authResult != null) {
            return new AdminAuth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(), session.getContext().getClient());
        }
        throw new NotAuthorizedException("Not login");
    }

//    protected StreamingOutput fetchUserImage(String realmId, String userId) {
//        return output -> copyStream(getAvatarStorageProvider().loadAvatarImage(realmId, userId), output);
//    }

//    private static InputStream readForm(InputStream input) {
//        if (input == null) {
//            throw new IllegalArgumentException("Multipart request is empty");
//        }
//
//        try {
//            int read = 0;
//            byte[] bytes = new byte[1024];
//            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
//            while ((read = input.read(bytes)) != -1) {
//                byteArrayOut.write(bytes, 0, read);
//            }
//
//            logger.logf(Logger.Level.INFO, "read size %d", byteArrayOut.size());
//            return new ByteArrayInputStream(byteArrayOut.toByteArray());
//        } catch (IOException e) {
//            throw new IllegalArgumentException("Error while reading multipart request", e);
//        }
//    }

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

    @GET
    @Path("/{user_id}")
    @Cache(maxAge = 1800)
    public Response getUserAvatar(@PathParam("user_id") String userId) {
        UserModel user = session.users().getUserById(userId, session.getContext().getRealm());
        if (user == null) throw new NotFoundException();
        if (config.isAlwaysRedirect()) {
            URI avatarURL = getAvatarStorageProvider().getAvatarURI(session.getContext().getRealm().getName(), userId);
            if (avatarURL == null && config.getDefaultAvatar() != null) {
                return Response.temporaryRedirect(config.getDefaultAvatar()).type("image/png").build();
            }
            if (avatarURL != null) {
                return Response.temporaryRedirect(avatarURL).type("image/png").build();
            }
        } else {
            try {
                InputStream stream = getAvatarStorageProvider().loadAvatarImage(session.getContext().getRealm().getName(), userId);
                return Response.ok(stream).type("image/png").build();
            } catch (Exception e) {e.printStackTrace();}
        }
        return Response.status(404).build();
    }

    @POST
    @Path("/{user_id}")
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserAvatar(@PathParam("user_id") String userId, MultipartFormDataInput input) {
        AdminAuth auth = checkBearerAuthentication(session);
        realmAuth = AdminPermissions.evaluator(session, auth.getRealm(), auth);
        canManageUsers(realmAuth);

        try {
            InputStream imageInputStream = input.getFormDataPart(AVATAR_IMAGE_PARAMETER, InputStream.class, null);
            ByteArrayInputStream byteArrayIn = convertImage(imageInputStream);

            UserModel user = session.users().getUserById(userId, session.getContext().getRealm());
            if (user == null) return Response.status(404).build();
            String realmName = session.getContext().getRealm().getName();

            getAvatarStorageProvider().saveAvatarImage(realmName, userId, byteArrayIn);
            auth.getUser().setSingleAttribute("has_avatar", "true");
            return Response.ok().type(MediaType.APPLICATION_JSON).build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }

    @DELETE
    @Path("/{user_id}")
    @NoCache
    public Response deleteUserAvatar(@PathParam("user_id") String userId) {
        UserModel user = session.users().getUserById(userId, session.getContext().getRealm());
        if (user == null) throw new NotFoundException();

        AdminAuth auth = checkBearerAuthentication(session);
        realmAuth = AdminPermissions.evaluator(session, auth.getRealm(), auth);
        canManageUsers(realmAuth);
        String realmName = session.getContext().getRealm().getName();
        if (getAvatarStorageProvider().removeAvatar(realmName, userId)) {
            user.setSingleAttribute("has_avatar", "false");
            return Response.ok().type(MediaType.APPLICATION_JSON).build();
        }
        return Response.serverError().build();
    }

    @GET
    @Path("")
    @Cache(maxAge = 1800)
    public Response getCurrentUserAvatar() {
        AuthenticationManager.AuthResult auth = checkCookieAuthentication(session);
        return getUserAvatar(auth.getUser().getId());
    }

    @POST
    @Path("")
    @NoCache
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadCurrentUserAvatarImage(MultipartFormDataInput input, @Context UriInfo uriInfo) {
        AuthenticationManager.AuthResult auth = checkCookieAuthentication(session);
        if (!isValidStateChecker(input)) {
            return badRequest();
        }

        try {
            InputStream imageInputStream = input.getFormDataPart(AVATAR_IMAGE_PARAMETER, InputStream.class, null);
//            InputStream read = readForm(imageInputStream);
            ByteArrayInputStream byteArrayIn = convertImage(imageInputStream);

            String realmName = auth.getSession().getRealm().getName();
            String userId = auth.getUser().getId();

            getAvatarStorageProvider().saveAvatarImage(realmName, userId, byteArrayIn);
            auth.getUser().setSingleAttribute("has_avatar", "true");
            return Response.seeOther(RealmsResource.accountUrl(session.getContext().getUri().getBaseUriBuilder()).build(realmName)).build();
        } catch (Exception ex) {
            return Response.serverError().build();
        }
    }

    private boolean isValidStateChecker(MultipartFormDataInput input) {
        try {
            String actualStateChecker = input.getFormDataPart(STATE_CHECKER_PARAMETER, String.class, null);
            String requiredStateChecker = (String) session.getAttribute(STATE_CHECKER_ATTRIBUTE);

            return Objects.equals(requiredStateChecker, actualStateChecker);
        } catch (Exception ex) {
            return false;
        }
    }

    private static void canManageUsers(AdminPermissionEvaluator realmAuth) {
        if (!realmAuth.users().canManage()) {
            logger.info("user does not have permission to manage users");
            throw new ForbiddenException("user does not have permission to manage users");
        }
    }
}
