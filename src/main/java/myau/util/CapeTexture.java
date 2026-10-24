package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;

/**
 * 自定义披风贴图的加载/缓存。
 *
 * <p>支持本地文件（相对路径按游戏目录解析）和 http(s) URL；URL 会缓存到
 * {@code config/ATALOSS/capes/}，下次离线也能用。下载是**同步**的（只在玩家主动开启模块时发生），
 * 因为 {@code TextureManager.loadTexture} 必须在渲染线程上调用。
 */
public final class CapeTexture {
    public static final ResourceLocation LOCATION = new ResourceLocation("ataloss", "capes/custom.png");

    private static boolean ready;
    private static int width;
    private static int height;

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    private CapeTexture() {
    }

    public static boolean isReady() {
        return ready;
    }

    /** @return null 表示成功，否则是给玩家看的错误信息 */
    public static String load(String source) {
        if (source == null || source.trim().isEmpty()) {
            return "路径为空";
        }
        String input = source.trim();
        File file;
        String downloadError = null;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            file = cacheFile(input);
            downloadError = download(input, file);
            if (downloadError != null && !file.isFile()) {
                return downloadError;
            }
        } else {
            file = resolve(input);
        }
        if (file == null || !file.isFile()) {
            return "文件不存在: " + input;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return "不是有效的图片: " + file.getName();
            }
            int w = image.getWidth();
            int h = image.getHeight();
            if (w <= 0 || h <= 0) {
                return "图片尺寸无效: " + w + "x" + h;
            }
            if (w != h * 2) {
                return "宽高比必须是 2:1（当前 " + w + "x" + h + "）";
            }
            // 披风 UV 是归一化的（ModelBox 按模型声明的 64x32 归一化），贴图实际尺寸不影响
            // 贴图区域，只决定 texel 密度 —— 所以这里按原始分辨率上传，HD 图就是真的 HD。
            // 只有超大图做保护性缩放，避免撑爆显存。
            if (w > 4096) {
                int targetW = 4096;
                int targetH = 2048;
                image = resize(image, targetW, targetH);
                w = targetW;
                h = targetH;
            }
            CapeTexture.width = w;
            CapeTexture.height = h;
            DynamicTexture texture = new DynamicTexture(image);
            Minecraft.getMinecraft().getTextureManager().loadTexture(LOCATION, texture);
            ready = true;
            return null;
        } catch (Throwable t) {
            return "加载失败: " + t.getClass().getSimpleName() + " " + t.getMessage();
        }
    }

    /** 高质量缩放到 64x32（披风 UV 空间是固定的） */
    private static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                    java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    public static void clear() {
        ready = false;
    }

    private static File resolve(String path) {
        File file = new File(path);
        if (!file.isAbsolute()) {
            file = new File(Minecraft.getMinecraft().mcDataDir, path);
        }
        return file;
    }

    private static File cacheFile(String url) {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config/ATALOSS/capes");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            return new File(dir, "cape.png");
        }
        return new File(dir, "cape_" + md5(url).substring(0, 12) + ".png");
    }

    private static String download(String url, File target) {
        File tmp = new File(target.getPath() + ".part");
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "ATALOSS");
            connection.setInstanceFollowRedirects(true);
            int code = connection.getResponseCode();
            if (code != 200) {
                return "下载失败: HTTP " + code;
            }
            if (target.getParentFile() != null && !target.getParentFile().isDirectory()) {
                target.getParentFile().mkdirs();
            }
            try (InputStream in = connection.getInputStream()) {
                Files.copy(in, tmp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            if (target.isFile() && !target.delete()) {
                return "无法覆盖旧缓存: " + target.getName();
            }
            if (!tmp.renameTo(target)) {
                return "无法写入缓存文件";
            }
            return null;
        } catch (Throwable t) {
            return "下载失败: " + t.getClass().getSimpleName();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            if (tmp.isFile()) {
                tmp.delete();
            }
        }
    }

    private static String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte value : bytes) {
                sb.append(String.format("%02x", value));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
