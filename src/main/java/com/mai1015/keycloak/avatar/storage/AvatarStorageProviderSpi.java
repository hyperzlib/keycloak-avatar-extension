package com.mai1015.keycloak.avatar.storage;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class AvatarStorageProviderSpi implements Spi {
    public static final String ID = "avatar-storage-provider";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return AvatarStorageProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return AvatarStorageProviderFactory.class;
    }
}
