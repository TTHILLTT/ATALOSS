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

    /** 1.8 皮肤必须是正方形（64x64 或等比高清）；64x32 旧版会错位 */
    public static boolean isSquareSkin(File file) {
        int[] size = size(file);
        return size != null && size[0] == size[1];
    }

    public static void clear() {
        CACHE.clear();
        SIZES.clear();
    }
}
