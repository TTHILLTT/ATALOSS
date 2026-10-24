package myau.ui;

import myau.util.TextureLoader;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** 衣柜预览贴图缓存：每个 png 只加载一次，按 路径+修改时间 作 key。 */
public final class WardrobePreview {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    private static final Map<String, int[]> SIZES = new HashMap<>();
    private static final Map<String, Boolean> SLIM = new HashMap<>();

    private WardrobePreview() {
    }

    public static ResourceLocation get(File file) {
        String key = file.getAbsolutePath() + "@" + file.lastModified();
        ResourceLocation cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return null;
            }
            ResourceLocation location = new ResourceLocation(
                    "ataloss", "wardrobe/" + Integer.toHexString(key.hashCode()) + ".png");
            TextureLoader.upload(location, image);
            CACHE.put(key, location);
            return location;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 图片尺寸 [w, h]；读不到返回 null */
    public static int[] size(File file) {
        String key = file.getAbsolutePath() + "@" + file.lastModified();
        int[] cached = SIZES.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return null;
            }
            int[] size = new int[]{image.getWidth(), image.getHeight()};
            SIZES.put(key, size);
            return size;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 自动识别皮肤模型：Alex（slim，3px 手臂）还是 Steve（classic，4px 手臂）。
     *
     * <p>判定依据是 64x64 布局里手臂正面第 4 列是否全透明：slim 皮肤只用 3px 宽的手臂，
     * 右臂 x=47、左臂 x=51 这两列（以及 47/51 相邻的 1px）是全透明的。
     */
    public static boolean isSlim(File file) {
        String key = file.getAbsolutePath() + "@" + file.lastModified();
        Boolean cached = SLIM.get(key);
        if (cached != null) {
            return cached;
        }
        boolean slim = false;
        try {
            BufferedImage image = ImageIO.read(file);
            if (image != null && image.getWidth() >= 64 && image.getHeight() >= 64) {
                int scale = image.getWidth() / 64;
                slim = columnEmpty(image, 47 * scale, 20 * scale, 12 * scale)
                        && columnEmpty(image, 51 * scale, 20 * scale, 12 * scale);
            }
        } catch (Throwable ignored) {
        }
        SLIM.put(key, slim);
        return slim;
    }

    private static boolean columnEmpty(BufferedImage image, int x, int yStart, int height) {
        for (int y = yStart; y < yStart + height; y++) {
            int argb = image.getRGB(x, y);
            if (((argb >>> 24) & 0xFF) != 0) {
                return false;
            }
        }
        return true;
    }
    /** 1.8 皮肤必须是正方形（64x64 或等比高清）；64x32 旧版会错位 */
    public static boolean isSquareSkin(File file) {
        int[] size = size(file);
        return size != null && size[0] == size[1];
    }

    public static void clear() {
        CACHE.clear();
        SIZES.clear();
        SLIM.clear();
    }
}
