package cn.isekai.keycloak.avatar.storage;

import net.coobird.thumbnailator.Thumbnails;
import org.jboss.logging.Logger;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;

public abstract class AvatarStorageProviderAbstract implements AvatarStorageProvider {
    private static final Logger logger = Logger.getLogger(AvatarStorageProviderAbstract.class);

    @Override
    public boolean saveOriginalAvatarImage(String realmName, String userId, String avatarId, InputStream input,
                                           AvatarCropParams cropParams, Map<String, Integer> sizeList) {
        try {
            boolean succeed = true;
            // 开始裁剪图片
            Thumbnails.Builder<? extends InputStream> cropper = Thumbnails.of(input).outputFormat("png");
            if (cropParams != null) {
                cropper.sourceRegion(cropParams.x, cropParams.y, cropParams.size, cropParams.size);
            }
            BufferedImage croppedImage = cropper.scale(1).asBufferedImage();
            for(Map.Entry<String, Integer> entries: sizeList.entrySet()) {
                // 转换尺寸
                Thumbnails.Builder<BufferedImage> resizer = Thumbnails.of(croppedImage)
                        .size(entries.getValue(), entries.getValue());

                ByteArrayOutputStream os = new ByteArrayOutputStream(10240);
                resizer.outputFormat("png").toOutputStream(os);
                InputStream is = new ByteArrayInputStream(os.toByteArray());
                os.close();
                // 保存对应尺寸的文件
                succeed = succeed && this.saveAvatarImage(realmName, userId, avatarId, is, entries.getKey());
                is.close();
            }
            return succeed;
        } catch (IOException ex){
            logger.error(ex);
            return false;
        }
    }
}
