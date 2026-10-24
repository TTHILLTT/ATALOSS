package myau.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * 衣柜的 3D 预览渲染：皮肤用 {@link ModelPlayer}，披风用几段弯折的 cape 模型。
 *
 * <p>渲染在 GUI 的正交投影里完成，靠 scissor 裁到方块内部；深度缓冲在 {@link #begin} 里清一次，
 * 否则会吃到世界渲染残留的深度导致模型被裁掉。
 */
public final class WardrobeRenderer {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static ModelPlayer playerModel;
    private static ModelBiped capeBase;
    private static ModelRenderer[] capeSegments;

    private WardrobeRenderer() {
    }

    public static void begin(int x, int y, int w, int h, int scaleFactor) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scaleFactor, mc.displayHeight - (y + h) * scaleFactor, w * scaleFactor, h * scaleFactor);
        GlStateManager.enableDepth();
        // glClear 受 depthMask 影响：模糊管线可能把 depthMask 关了，必须显式打开，否则清不掉旧深度，
        // 3D 模型会被世界渲染残留的深度直接裁掉（表现就是"什么都看不到"）
        GlStateManager.depthMask(true);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        // 模型在 X 轴做了镜像缩放，绕序会翻转；这里直接关剔除，避免整块模型被背面剔除掉
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableStandardItemLighting();
    }

    public static void end() {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(true);
        // 恢复 GUI 期望的状态：关剔除、开纹理、复位颜色，避免后续玻璃/文字被裁或变暗
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** 小人预览；size 是模型缩放（0.0625 = 原尺寸） */
    public static void renderSkin(ResourceLocation texture, float centerX, float centerY, float size,
                                  float yaw, float pitch) {
        if (texture == null) {
            return;
        }
        try {
            if (playerModel == null) {
                playerModel = new ModelPlayer(0.0F, false);
            }
            playerModel.setRotationAngles(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F, mc.thePlayer);
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX, centerY, 120.0F);
            GlStateManager.scale(-size, size, size);
            GlStateManager.rotate(180.0F + yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            mc.getTextureManager().bindTexture(texture);
            playerModel.render(mc.thePlayer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
            GlStateManager.popMatrix();
        } catch (Throwable ignored) {
        }
    }

    /** 披风预览：4 段小方块按角度叠加，模拟布料的弯曲 */
    public static void renderCape(ResourceLocation texture, float centerX, float centerY, float size,
                                  float yaw, float pitch) {
        if (texture == null) {
            return;
        }
        try {
            buildCape();
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX, centerY, 120.0F);
            GlStateManager.scale(-size, size, size);
            GlStateManager.rotate(180.0F + yaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
            mc.getTextureManager().bindTexture(texture);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, -8.0F, 0.0F);
            for (int i = 0; i < capeSegments.length; i++) {
                GlStateManager.pushMatrix();
                GlStateManager.translate(0.0F, i * 4.0F, 0.0F);
                GlStateManager.rotate(i * 7.0F, 1.0F, 0.0F, 0.0F);
                capeSegments[i].render(0.0625F);
                GlStateManager.popMatrix();
            }
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        } catch (Throwable ignored) {
        }
    }

    private static void buildCape() {
        if (capeSegments != null) {
            return;
        }
        capeBase = new ModelBiped();
        capeSegments = new ModelRenderer[4];
        for (int i = 0; i < capeSegments.length; i++) {
            ModelRenderer segment = new ModelRenderer(capeBase, 0, 0);
            // 每段 10x4x1，UV 从 (0, i*4) 起，正好取披风贴图的对应条带
            segment.setTextureOffset(0, i * 4);
            segment.addBox(-5.0F, 0.0F, -0.5F, 10, 4, 1, 0.0F);
            capeSegments[i] = segment;
        }
    }
}
