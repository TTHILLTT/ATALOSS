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

    public static void clear() {
        CACHE.clear();
    }
}
