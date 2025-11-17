package cn.isekai.keycloak.avatar.storage.fs;

import cn.isekai.keycloak.avatar.spi.ConfigService;
import cn.isekai.keycloak.avatar.utils.ResUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import org.apache.http.client.utils.DateUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import java.io.File;
import java.util.Date;

@Path("")
public class FSAvatarResource {
    private static final Logger logger = Logger.getLogger(FSAvatarResource.class);

    public static final int MAX_AGE = 86400;
    public static String DEFAULT_SIZE = "lg";

    protected KeycloakSession session;
    private FSAvatarStorageConfig fsConfig;

    // to fix "Unsatisfied dependency" problem on quarkus
    public FSAvatarResource() {

    }

    public FSAvatarResource(KeycloakSession session,
                            ConfigService config,
                            FSAvatarStorageProvider fsProvider) {
        this.session = session;
        this.fsConfig = fsProvider.getConfig();

        DEFAULT_SIZE = config.getDefaultSize();

        logger.info("FSAvatarResource initialized.");
    }

    @Path("{avatar_id}/avatar-{size}.png")
    @GET
    public Response getUserAvatarStaticById(@PathParam("avatar_id") String avatarId,
                                            @PathParam("size") String size,
                                            @HeaderParam("if-none-match") String ifNoneMatch,
                                            @HeaderParam("if-modified-since") String ifModifiedSince) {
        logger.infof("Get static avatar file: %s/avatar-%s.png", avatarId, size);
        String realmName = session.getContext().getRealm().getName();

        String filePath = fsConfig.getAvatarPath(realmName, "", avatarId, size);
        logger.info("Static file: " + filePath);

        File staticFile = new File(filePath);
        if (!staticFile.exists()) {
            return ResUtils.responseNotFound(false);
        }

        if (!staticFile.canRead()) {
            return ResUtils.responseNotAuthorized(false);
        }

        Date lastModified = new Date(staticFile.lastModified());
        long fileSize = staticFile.length();
        String eTag = ResUtils.md5(String.format("%d%d", lastModified.getTime(), fileSize));

        boolean ifModified = false;
        if (ifModifiedSince != null) {
            Date clientLastModified = DateUtils.parseDate(ifModifiedSince);
            long clientLastModifiedTime = clientLastModified.getTime() / 1000;
            long lastModifiedTime = lastModified.getTime() / 1000;
            //logger.info(String.format("clientLastModified: %d, lastModified: %d", clientLastModifiedTime, lastModifiedTime));
            if (clientLastModifiedTime < lastModifiedTime) {
                ifModified = true;
            }
        }

        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            //logger.info(String.format("clientETag: %s, eTag: %s", ifNoneMatch, eTag));
            ifModified = true;
        }

        if (ifModifiedSince == null && ifNoneMatch == null) {
            ifModified = true;
        }

        // deal with cache
        if (!ifModified) {
            return Response
                    .status(Response.Status.NOT_MODIFIED)
                    .type("image/png")
                    .build();
        }

        try {
            CacheControl cc = new CacheControl();
            cc.setMaxAge(MAX_AGE);
            cc.setPrivate(true);

            return Response
                    .status(Response.Status.OK)
                    .cacheControl(cc)
                    .lastModified(lastModified)
                    .header("E-Tag", eTag)
                    .type("image/png")
                    .entity(staticFile)
                    .build();
        } catch (Exception e) {
            logger.error("Cannot load file", e);
        }
        return ResUtils.responseInternalServerError(false);
    }
}
