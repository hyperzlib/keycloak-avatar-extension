package com.mai1015.keycloak.avatar.storage.minio;

import com.mai1015.keycloak.avatar.storage.AvatarStorageProvider;
import io.minio.PutObjectOptions;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;

import java.io.InputStream;
import java.net.URI;

public class MinioAvatarStorageProvider implements AvatarStorageProvider {

    private final MinioTemplate minioTemplate;

    public MinioAvatarStorageProvider(MinioConfig minioConfig) throws InvalidPortException, InvalidEndpointException {
        this.minioTemplate = new MinioTemplate(minioConfig);
    }

    @Override
    public boolean hasAvatar(String realmName, String userId) {
        String bucketName = minioTemplate.getBucketName(realmName);
        if (!minioTemplate.hasBucket(bucketName)) return false;
        return minioTemplate.fileExists(bucketName, userId + ".png");
    }

    @Override
    public void saveAvatarImage(String realmName, String userId, InputStream input) {

        String bucketName = minioTemplate.getBucketName(realmName);
        minioTemplate.ensureBucketExists(bucketName);

        minioTemplate.execute(minioClient -> {
            PutObjectOptions options = new PutObjectOptions(input.available(), -1);
            options.setContentType("image/png");
            minioClient.putObject(bucketName, userId + ".png", input, options);
            return null;
        });
    }

    @Override
    public InputStream loadAvatarImage(String realmName, String userId) {
        String bucketName = minioTemplate.getBucketName(realmName);

        return minioTemplate.execute(minioClient -> minioClient.getObject(bucketName, userId + ".png"));
    }

    @Override
    public URI getAvatarURI(String realmName, String userId) {
        String bucketName = minioTemplate.getBucketName(realmName);
        try {
            String url = minioTemplate.execute(minioClient -> minioClient.getObjectUrl(bucketName, userId + ".png"));
            return new URI(url);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean removeAvatar(String realmName, String userId) {
        String bucketName = minioTemplate.getBucketName(realmName);
        return minioTemplate.execute(minioClient -> {
            try {
                minioClient.removeObject(bucketName, userId + ".png");
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Override
    public void close() {
        // NOOP
    }

}
