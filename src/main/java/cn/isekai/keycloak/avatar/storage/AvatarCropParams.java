package cn.isekai.keycloak.avatar.storage;

public class AvatarCropParams {
    int x;
    int y;
    int size;

    public AvatarCropParams() { }

    public AvatarCropParams(String paramStr) {
        String[] paramStrList = paramStr.split(",");
        if (paramStrList.length >= 3){
            this.x = Integer.valueOf(paramStrList[0]);
            this.y = Integer.valueOf(paramStrList[1]);
            this.size = Integer.valueOf(paramStrList[2]);
        }
    }
}
