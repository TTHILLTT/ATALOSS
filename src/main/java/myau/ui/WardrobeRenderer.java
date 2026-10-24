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
    private static ModelRenderer capeSegment;

    private WardrobeRenderer() {
    }

    public static void begin(int x, int y, int w, int h, int scaleFactor) {
        // 先不做 scissor / 深度：这两者是"模型完全不可见"的高嫌疑项，先确认模型能画出来
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (WardrobeTuning.depth) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        } else {
            GlStateManager.disableDepth();
        }
        if (WardrobeTuning.cull) {
            GlStateManager.enableCull();
        } else {
            GlStateManager.disableCull();
        }
        GlStateManager.enableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (WardrobeTuning.lighting) {
            RenderHelper.enableStandardItemLighting();
        }
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
    }

    /** 小人预览；size 是"每个模型单位对应多少 GUI 像素"（约 30 时小人 ≈ 60px 高） */
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
            GlStateManager.translate(centerX + WardrobeTuning.skinOffsetX,
                    centerY + WardrobeTuning.skinOffsetY, 100.0F);
            // 双轴负缩放 + 绕 Z 旋 180°，保持三角形绕序（原版 GUI 物品栏预览同款做法）
            float skinSize = size * WardrobeTuning.skinScale / 100.0F;
            GlStateManager.scale(-skinSize, -skinSize, skinSize);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(WardrobeTuning.skinYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(WardrobeTuning.skinPitch, 1.0F, 0.0F, 0.0F);
            mc.getTextureManager().bindTexture(texture);
            playerModel.render(mc.thePlayer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
            GlStateManager.popMatrix();
        } catch (Throwable ignored) {
        }
    }

    /** 披风预览：单个 10x16x1 的 cape 模型，按固定角度摆放 */
    public static void renderCape(ResourceLocation texture, float centerX, float centerY, float size,
                                  float yaw, float pitch) {
        if (texture == null) {
            return;
        }
        try {
            buildCape();
            GlStateManager.pushMatrix();
            GlStateManager.translate(centerX + WardrobeTuning.capeOffsetX,
                    centerY + WardrobeTuning.capeOffsetY, 100.0F);
            float capeSize = size * WardrobeTuning.capeScale / 100.0F;
            GlStateManager.scale(-capeSize, -capeSize, capeSize);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(WardrobeTuning.capeYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(WardrobeTuning.capePitch, 1.0F, 0.0F, 0.0F);
            mc.getTextureManager().bindTexture(texture);
            // 整块披风一次画完：分段做偏移时 translate 在 scale 之后会被放大 size 倍，
            // 段与段之间被拉开到几百像素，散落到屏幕各处
            capeSegment.render(0.0625F);
            GlStateManager.popMatrix();
        } catch (Throwable ignored) {
        }
    }

    private static void buildCape() {
        if (capeSegment != null) {
            return;
        }
        capeBase = new ModelBiped();
        // 与 MC 披风模型一致：10x16x1 的薄板，UV 从 (0,0) 起
        capeSegment = new ModelRenderer(capeBase, 0, 0);
        capeSegment.addBox(-5.0F, 0.0F, -0.5F, 10, 16, 1, 0.0F);
        capeSegment.setRotationPoint(0.0F, 0.0F, 0.0F);
    }
}
