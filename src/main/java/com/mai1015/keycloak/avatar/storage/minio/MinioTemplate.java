package com.mai1015.keycloak.avatar.storage.minio;

import io.minio.ErrorCode;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import okhttp3.OkHttpClient;
import org.jboss.logging.Logger;

import javax.ws.rs.InternalServerErrorException;
import java.util.concurrent.TimeUnit;

public class MinioTemplate {
    private static final Logger logger = Logger.getLogger(MinioTemplate.class);
    private static final int TIMEOUT_SECONDS = 15;

    private final MinioConfig minioConfig;

    private final OkHttpClient httpClient;
    private final MinioClient minioClient;

    public MinioTemplate(MinioConfig minioConfig) throws InvalidPortException, InvalidEndpointException {
        this.minioConfig = minioConfig;
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        minioClient = new MinioClient(minioConfig.getServerUrl(), 0, minioConfig.getAccessKey(),
                minioConfig.getSecretKey(), "", false, httpClient);
    }

    public <T> T execute(MinioCallback<T> callback) {
        try {
            MinioClient minioClient = new MinioClient(minioConfig.getServerUrl(), 0, minioConfig.getAccessKey(),
                    minioConfig.getSecretKey(), "", false, httpClient);
            return callback.doInMinio(minioClient);
        } catch (Exception mex) {
            throw new RuntimeException(mex);
        }
    }

    public boolean hasBucket(String bucketName) {
        return execute(client -> client.bucketExists(bucketName));
    }

    public boolean fileExists(String bucketName, String fileName) {
        return execute(client -> {
            try {
                client.statObject(bucketName, fileName);
                return true;
            } catch (ErrorResponseException e) {
                if (e.errorResponse().errorCode() != ErrorCode.NO_SUCH_OBJECT) {
                    throw new InternalServerErrorException(e);
                }
            }
            return false;
        });
    }

    public void ensureBucketExists(String bucketName) {
        execute(client -> {

            boolean exists = client.bucketExists(bucketName);
            if (exists) {
                logger.debugf("Bucket: %s already exists", bucketName);
            } else {
                client.makeBucket(bucketName);
            }

            return null;
        });
    }


    public String getBucketName(String realmName) {
        return realmName + minioConfig.getDefaultBucketSuffix();
    }

}
