package myau.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.File;

/**
 * 衣柜方块的平面预览：皮肤画"正面小人"拼贴，披风只画有图案的外侧面。
 * UV 按图片实际尺寸归一化，64x64 / 128x128 皮肤、64x32 / 128x64 披风都适用。
 */
public final class WardrobeFlatPreview {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private WardrobeFlatPreview() {
    }

    public static void drawSkinFront(File file, ResourceLocation texture, int x, int y, int w, int h) {
        int[] size = WardrobePreview.size(file);
        if (size == null) {
            return;
        }
        float scale = (float) size[0] / 64.0F;
        float unit = (float) h / 32.0F;
        float left = x + (w - 16.0F * unit) / 2.0F;
        float top = y;
        part(texture, left + 4 * unit, top, 8 * unit, 8 * unit, size, 8 * scale, 8 * scale, 8 * scale, 8 * scale);
        part(texture, left + 4 * unit, top + 8 * unit, 8 * unit, 12 * unit, size, 20 * scale, 20 * scale, 8 * scale, 12 * scale);
        part(texture, left, top + 8 * unit, 4 * unit, 12 * unit, size, 44 * scale, 20 * scale, 4 * scale, 12 * scale);
        part(texture, left + 12 * unit, top + 8 * unit, 4 * unit, 12 * unit, size, 36 * scale, 52 * scale, 4 * scale, 12 * scale);
        part(texture, left + 4 * unit, top + 20 * unit, 4 * unit, 12 * unit, size, 4 * scale, 20 * scale, 4 * scale, 12 * scale);
        part(texture, left + 8 * unit, top + 20 * unit, 4 * unit, 12 * unit, size, 20 * scale, 52 * scale, 4 * scale, 12 * scale);
    }

    public static void drawCapeFront(File file, ResourceLocation texture, int x, int y, int w, int h) {
        int[] size = WardrobePreview.size(file);
        if (size == null) {
            return;
        }
        float sx = (float) size[0] / 64.0F;
        float sy = (float) size[1] / 32.0F;
        float ratio = 10.0F / 16.0F;
        float drawH = h;
        float drawW = drawH * ratio;
        if (drawW > w) {
            drawW = w;
            drawH = drawW / ratio;
        }
        float left = x + (w - drawW) / 2.0F;
        float top = y + (h - drawH) / 2.0F;
        part(texture, left, top, drawW, drawH, size, 1 * sx, 1 * sy, 10 * sx, 16 * sy);
    }

    private static void part(ResourceLocation texture, float x, float y, float w, float h, int[] image,
                             float u, float v, float uw, float uh) {
        if (image[0] <= 0 || image[1] <= 0) {
            return;
        }
        float u1 = u / image[0];
        float v1 = v / image[1];
        float u2 = (u + uw) / image[0];
        float v2 = (v + uh) / image[1];
        GlStateManager.bindTexture(Minecraft.getMinecraft().getTextureManager().getTexture(texture).getGlTextureId());
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u1, v1);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(u2, v1);
        GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f(u2, v2);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(u1, v2);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
