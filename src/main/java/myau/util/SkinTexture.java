package myau.util;

import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;

/**
 * 自定义皮肤贴图。
 *
 * <p>1.8 的皮肤布局是 64x64，UV 同样按模型声明的 64x64 归一化，所以 128x128、256x256
 * 这类等比例高清皮肤也能直接按原分辨率上传（texel 密度更高）。
 * 旧版 64x32 布局不支持（会明确提示去转换），因为 1.8 的模型缺下半部分 UV。
 */
public final class SkinTexture {
    public static final ResourceLocation LOCATION = new ResourceLocation("ataloss", "skins/custom.png");
    private static final int MAX_SIZE = 4096;
    private static final int MIN_SIZE = 64;

    private static boolean ready;
    private static int width;
    private static int height;

    private SkinTexture() {
    }

    public static boolean isReady() {
        return ready;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    /** @return null 表示成功，否则是给玩家看的错误信息 */
    public static String load(String source) {
        TextureLoader.ensureDir("config/ATALOSS/skins");
        TextureLoader.Result result = TextureLoader.fetch(source, "skins");
        if (result.error != null) {
            return result.error;
        }
        BufferedImage image = result.image;
        int w = image.getWidth();
        int h = image.getHeight();
        if (w != h) {
            if (w == h * 2) {
                return "这是旧版 64x32 皮肤，请先转换成 64x64（当前 " + w + "x" + h + "）";
            }
            return "皮肤必须是正方形（当前 " + w + "x" + h + "）";
        }
        if (w < MIN_SIZE) {
            return "皮肤至少 " + MIN_SIZE + "x" + MIN_SIZE + "（当前 " + w + "x" + h + "）";
        }
        if (w > MAX_SIZE) {
            image = TextureLoader.resize(image, MAX_SIZE, MAX_SIZE);
            w = MAX_SIZE;
            h = MAX_SIZE;
        }
        width = w;
        height = h;
        TextureLoader.upload(LOCATION, image);
        ready = true;
        return null;
    }

    public static void clear() {
        ready = false;
    }
}
