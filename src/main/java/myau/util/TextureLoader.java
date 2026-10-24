package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/**
 * 贴图加载公共件：本地文件 / http(s) URL → BufferedImage，再上传成 Minecraft 纹理。
 *
 * <p>URL 会缓存到 {@code config/ATALOSS/<cacheDir>/<prefix>_<md5>.png}，失败时若缓存存在则复用。
 * 下载是同步的（只在玩家主动开启对应模块时发生），因为 {@code TextureManager.loadTexture}
 * 必须在渲染线程调用。
 */
public final class TextureLoader {

    private TextureLoader() {
    }

    public static final class Result {
        public final BufferedImage image;
        public final String error;

        private Result(BufferedImage image, String error) {
            this.image = image;
            this.error = error;
        }

        static Result ok(BufferedImage image) {
            return new Result(image, null);
        }

        static Result fail(String error) {
            return new Result(null, error);
        }
    }

    public static void ensureDir(String relative) {
        try {
            File dir = new File(Minecraft.getMinecraft().mcDataDir, relative);
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
        } catch (Throwable ignored) {
        }
    }

    public static Result fetch(String source, String cacheDir) {
        if (source == null || source.trim().isEmpty()) {
            return Result.fail("路径为空");
        }
        String input = source.trim();
        File file;
        if (input.startsWith("http://") || input.startsWith("https://")) {
            file = new File(new File(Minecraft.getMinecraft().mcDataDir, "config/ATALOSS/" + cacheDir),
                    "cache_" + md5(input).substring(0, 12) + ".png");
            String error = download(input, file);
            if (error != null && !file.isFile()) {
                return Result.fail(error);
            }
        } else {
            file = new File(input);
            if (!file.isAbsolute()) {
                file = new File(Minecraft.getMinecraft().mcDataDir, input);
            }
        }
        if (!file.isFile()) {
            return Result.fail("文件不存在: " + input);
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                return Result.fail("不是有效的图片: " + file.getName());
            }
            return Result.ok(image);
        } catch (Throwable t) {
            return Result.fail("读取失败: " + t.getClass().getSimpleName());
        }
    }

    public static void upload(ResourceLocation location, BufferedImage image) {
        Minecraft.getMinecraft().getTextureManager().loadTexture(location, new DynamicTexture(image));
    }

    public static BufferedImage resize(BufferedImage source, int width, int height) {
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
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
                Files.copy(in, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
