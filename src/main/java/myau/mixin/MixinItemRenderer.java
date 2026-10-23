package myau.mixin;

import myau.module.legit.OldAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemCarrotOnAStick;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.7 动画的第一人称部分（移植自 FPSMaster Edge 的 MixinItemRenderer）。
 *
 * <p>用 {@code @Inject(HEAD, cancellable)} 而不是 {@code @Overwrite}：
 * 只有 {@link OldAnimations} 开着时才接管 {@code renderItemInFirstPerson}，
 * 关掉后完全走原版，避免和 OptiFine 等同样注入该方法的 mod 冲突。
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {
    @Shadow
    private ItemStack itemToRender;
    @Shadow
    private float equippedProgress;

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Shadow
    protected abstract void rotateArroundXAndY(float angle, float angleY);

    @Shadow
    protected abstract void setLightMapFromPlayer(AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks);

    @Shadow
    protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);

    @Shadow
    protected abstract void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks);

    @Shadow
    protected abstract void doBlockTransformations();

    @Shadow
    protected abstract void doItemUsedTransformations(float swingProgress);

    @Shadow
    public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform);

    @Shadow
    protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

    @Inject(method = {"renderItemInFirstPerson"}, at = {@At("HEAD")}, cancellable = true)
    private void ataloss$oldAnimations(float partialTicks, CallbackInfo callbackInfo) {
        if (!OldAnimations.using) {
            return;
        }
        this.ataloss$renderFirstPerson(partialTicks);
        callbackInfo.cancel();
    }

    private void ataloss$renderFirstPerson(float partialTicks) {
        OldAnimations module = OldAnimations.get();
        if (module == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }
        float equipped = 1.0F - this.equippedProgress;
        EntityPlayerSP player = mc.thePlayer;
        float swingProgress = player.getSwingProgress(partialTicks);
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;
        this.rotateArroundXAndY(pitch, yaw);
        this.setLightMapFromPlayer(player);
        this.rotateWithPlayerRotations(player, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        if (this.itemToRender != null) {
            if (module.oldRod.getValue()
                    && (this.itemToRender.getItem() instanceof ItemCarrotOnAStick
                    || this.itemToRender.getItem() instanceof ItemFishingRod)) {
                GlStateManager.translate(0.08F, -0.027F, -0.33F);
                GlStateManager.scale(0.93F, 1.0F, 1.0F);
            }
            if (module.oldSwing.getValue() && swingProgress != 0.0F
                    && !player.isBlocking() && !player.isUsingItem()) {
                GlStateManager.scale(0.85F, 0.85F, 0.85F);
                GlStateManager.translate(-0.06F, 0.003F, 0.05F);
            }
            if (this.itemToRender.getItem() instanceof ItemMap) {
                this.renderItemMap(player, pitch, equipped, swingProgress);
            } else if (player.getItemInUseCount() > 0) {
                EnumAction action = this.itemToRender.getItemUseAction();
                switch (action) {
                    case NONE:
                        this.transformFirstPersonItem(equipped, 0.0F);
                        break;
                    case EAT:
                    case DRINK:
                        if (module.oldUsing.getValue()) {
                            this.performDrinking(player, partialTicks);
                            this.transformFirstPersonItem(equipped, swingProgress);
                        } else {
                            this.transformFirstPersonItem(equipped, 0.0F);
                            this.doItemUsedTransformations(swingProgress);
                        }
                        break;
                    case BLOCK:
                        if (module.oldBlock.getValue()) {
                            this.ataloss$drawBlocking(module, equipped, swingProgress);
                        } else {
                            this.transformFirstPersonItem(equipped, 0.0F);
                            this.doBlockTransformations();
                        }
                        break;
                    case BOW:
                        if (module.oldBow.getValue()) {
                            this.transformFirstPersonItem(equipped, swingProgress);
                        } else {
                            this.transformFirstPersonItem(equipped, 0.0F);
                        }
                        this.doBowTransformations(partialTicks, player);
                        break;
                    default:
                        break;
                }
            } else {
                this.doItemUsedTransformations(swingProgress);
                this.transformFirstPersonItem(equipped, swingProgress);
            }
            this.renderItem(player, this.itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
        } else if (!player.isInvisible()) {
            this.renderPlayerArm(player, equipped, swingProgress);
        }

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    private void ataloss$drawBlocking(OldAnimations module, float equipped, float swing) {
        GlStateManager.translate(module.offsetX.getValue(), module.offsetY.getValue(), module.offsetZ.getValue());
        float scale = module.itemScale.getValue();
        if (scale != 1.0F) {
            GlStateManager.scale(scale, scale, scale);
        }
        Minecraft mc = Minecraft.getMinecraft();
        int mode = module.animationMode.getValue();
        if (mode == OldAnimations.MODE_LUNAR) {
            this.ataloss$transformLunar(0.2F, swing);
            this.doBlockTransformations();
            GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        } else if (mode == OldAnimations.MODE_SIGMA) {
            this.transformFirstPersonItem(equipped, swing);
            float sin = MathHelper.sin((float) (MathHelper.sqrt_float(equipped) * Math.PI));
            GlStateManager.rotate(-sin * 55.0F / 2.0F, -8.0F, 0.0F, 9.0F);
            GlStateManager.rotate(-sin * 45.0F, 1.0F, sin / 2.0F, 0.0F);
            this.doBlockTransformations();
            GlStateManager.translate(0.2F, mc.thePlayer != null && mc.thePlayer.isSneaking() ? 0.1F : 0.1F, 0.7F);
        } else if (mode == OldAnimations.MODE_DEBUG) {
            this.transformFirstPersonItem(0.2F, swing);
            this.doBlockTransformations();
            GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        } else if (mode == OldAnimations.MODE_LUNA) {
            this.transformFirstPersonItem(equipped, 0.0F);
            this.doBlockTransformations();
            float sin = MathHelper.sin((float) (MathHelper.sqrt_float(swing) * Math.PI));
            GlStateManager.translate(-0.2F, 0.45F, 0.25F);
            GlStateManager.rotate(-sin * 20.0F, -5.0F, -5.0F, 9.0F);
        } else if (mode == OldAnimations.MODE_SWANG) {
            this.transformFirstPersonItem(equipped / 2.0F, swing);
            float sin = MathHelper.sin((float) (MathHelper.sqrt_float(swing) * Math.PI));
            GlStateManager.rotate(sin * 30.0F / 2.0F, -sin, 0.0F, 9.0F);
            GlStateManager.rotate(sin * 40.0F, 1.0F, -sin / 2.0F, 0.0F);
            this.doBlockTransformations();
        } else if (mode == OldAnimations.MODE_SWANK) {
            this.transformFirstPersonItem(equipped / 2.0F, swing);
            float sin = MathHelper.sin((float) (MathHelper.sqrt_float(equipped) * Math.PI));
            GlStateManager.rotate(sin * 30.0F, -sin, 0.0F, 9.0F);
            GlStateManager.rotate(sin * 40.0F, 1.0F, -sin, 0.0F);
            this.doBlockTransformations();
        } else if (mode == OldAnimations.MODE_SWONG) {
            this.transformFirstPersonItem(equipped / 2.0F, 0.0F);
            float sin = MathHelper.sin((float) (MathHelper.sqrt_float(swing) * Math.PI));
            GlStateManager.rotate(-sin * 40.0F / 2.0F, sin / 2.0F, 0.0F, 9.0F);
            GlStateManager.rotate(-sin * 30.0F, 1.0F, sin / 2.0F, 0.0F);
            this.doBlockTransformations();
        } else if (mode == OldAnimations.MODE_JIGSAW) {
            this.transformFirstPersonItem(0.1F, swing);
            this.doBlockTransformations();
            GlStateManager.translate(-0.5F, 0.0F, 0.0F);
        } else if (mode == OldAnimations.MODE_JELLO) {
            GlStateManager.translate(0.56F, -0.52F, -0.72F);
            GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
            float sin = MathHelper.sin((float) (MathHelper.sqrt_float(swing) * Math.PI));
            GlStateManager.rotate(sin * -20.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(sin * -20.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(sin * -80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(0.4F, 0.4F, 0.4F);
            GlStateManager.translate(-0.5F, 0.2F, 0.0F);
            GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
            this.doBlockTransformations();
        } else if (mode == OldAnimations.MODE_PUSH) {
            this.transformFirstPersonItem(equipped, 0.0F);
            this.doBlockTransformations();
            float sin = MathHelper.sin((float) (MathHelper.sqrt_float(swing) * Math.PI));
            GlStateManager.rotate(-sin * 35.0F, -8.0F, 0.0F, 9.0F);
            GlStateManager.rotate(-sin * 10.0F, 1.0F, -0.4F, -0.5F);
        } else {
            // 1.7（默认）：格挡时保留挥动进度
            this.transformFirstPersonItem(equipped, swing);
            this.doBlockTransformations();
        }
    }

    private void ataloss$transformLunar(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.07F, -0.14F, -0.11F);
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }
}
