package cn.isekai.keycloak.avatar.storage.fs;

import cn.isekai.keycloak.avatar.rest.AvatarResource;
import cn.isekai.keycloak.avatar.spi.ConfigService;
import cn.isekai.keycloak.avatar.storage.AvatarStorageProviderAbstract;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;

import java.io.*;

public class FSAvatarStorageProvider extends AvatarStorageProviderAbstract {
    private static final Logger logger = Logger.getLogger(AvatarResource.class);

    public final KeycloakSession session;
    public final FSAvatarStorageConfig config;

    public FSAvatarStorageProvider(KeycloakSession session, FSAvatarStorageConfig config){
        this.session = session;
        this.config = config;
    }

    @Override
    public boolean hasAvatar(String realmName, String userId, String avatarId, String defaultSize){
        return new File(config.getAvatarPath(realmName, userId, avatarId, defaultSize)).exists();
    }

    @Override
    public boolean saveAvatarImage(String realmName, String userId, String avatarId, InputStream input, String size) {
        String filePath = config.getAvatarPath(realmName, userId, avatarId, size);
        File avatarFile = new File(filePath);
        File avatarFolderPath = avatarFile.getParentFile();
        if(!avatarFolderPath.exists()){
            if(!avatarFolderPath.mkdirs()) return false;
        }
        try {
            FileOutputStream avatarFileStream = new FileOutputStream(avatarFile);
            byte[] buffer = new byte[10240];
            int bufferLen;
            while((bufferLen = input.read(buffer)) > 0){
                avatarFileStream.write(buffer, 0, bufferLen);
            }
            avatarFileStream.close();
            return true;
        } catch(Exception e){
            return false;
        }
    }

    @Override
    public InputStream loadAvatarImage(String realmName, String userId, String avatarId, String size, boolean fallbackDefault) {
        String filePath = config.getAvatarPath(realmName, userId, avatarId, size);
        File avatarFile = new File(filePath);
        logger.info("load avatar: " + filePath);
        if(avatarFile.exists()){
            try {
                return new FileInputStream(avatarFile);
            } catch (FileNotFoundException e){
                logger.error(e.getLocalizedMessage(), e);
            }
        } else if(fallbackDefault) {
            //返回默认头像
            filePath = config.getDefaultAvatarPath(realmName, size);
            logger.info("load default avatar: " + filePath);
            avatarFile = new File(filePath);
            if(avatarFile.exists()){
                try {
                    return new FileInputStream(avatarFile);
                } catch (FileNotFoundException e){
                    logger.error(e.getLocalizedMessage(), e);
                }
            }
        }
        return null;
    }

    @Override
    public String getAvatarURL(String realmName, String userId, String avatarId, String size, boolean fallbackDefault){
        String filePath = config.getAvatarPath(realmName, userId, avatarId, size);
        File avatarFile = new File(filePath);
        logger.info("load avatar: " + filePath);
        if(avatarFile.exists()){
            return config.getAvatarURL(realmName, userId, avatarId, size);
        } else if(fallbackDefault) {
            //返回默认头像
            filePath = config.getDefaultAvatarPath(realmName, size);
            avatarFile = new File(filePath);
            if(avatarFile.exists()){
                return config.getDefaultAvatarURL(realmName, size);
            }
        }
        return null;
    }

    @Override
    public boolean removeAvatar(String realmName, String userId, String avatarId, Iterable<String> sizeList){
        boolean succeed = true;
        for(String size : sizeList){
            String filePath = config.getAvatarPath(realmName, userId, avatarId, size);
            File avatarFile = new File(filePath);
            if(avatarFile.exists()){
                succeed = succeed && avatarFile.delete();
            }
        }
        return succeed;
    }

    public FSAvatarStorageConfig getConfig() {
        return config;
    }

    @Override
    public void close() {

    }

    @Override
    public Object getResource() {
        ConfigService config = session.getProvider(ConfigService.class);
        return new FSAvatarResource(session, config, this);
    }
}
