package cn.isekai.keycloak.avatar.storage.fs;

import cn.isekai.keycloak.avatar.spi.ConfigService;
import cn.isekai.keycloak.avatar.utils.ResUtils;
import org.apache.http.client.utils.DateUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Date;

@Path("/avatar-file")
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
    }

    @GET
    @Path("/{path:.*\\.png$}")
    public Response getUserAvatarStaticById(@PathParam("path") String path,
                                            @HeaderParam("if-none-match") String ifNoneMatch,
                                            @HeaderParam("if-modified-since") String ifModifiedSince) {
        String realmName = session.getContext().getRealm().getName();
        String rootPath = fsConfig.getStorageRoot();
        if ("/".equals(rootPath.substring(rootPath.length() - 1))) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }

        String filePath = String.format("%s/%s/avatar/%s", rootPath, realmName, path);
        //logger.info("Static file: " + filePath);

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
