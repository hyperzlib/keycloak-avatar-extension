package com.mai1015.keycloak.avatar.storage.minio;

public class MinioConfig {

    private final String serverUrl;

    private final String accessKey;

    private final String secretKey;

    private String defaultBucketSuffix = "-avatars";

    public MinioConfig(String serverUrl, String accessKey, String secretKey) {
        this.serverUrl = serverUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getDefaultBucketSuffix() {
        return defaultBucketSuffix;
    }
}
