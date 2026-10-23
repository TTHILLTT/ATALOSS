package myau.mixin;

import myau.module.legit.OldAnimations;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 第三人称的 1.7 格挡姿态（手持物在举盾时前移/旋转） */
@Mixin(LayerHeldItem.class)
public class MixinLayerHeldItem {
    @Inject(
            method = {"doRenderLayer"},
            at = {@At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;renderItem(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/ItemCameraTransforms$TransformType;)V"
            )}
    )
    private void ataloss$oldThirdPerson(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                        float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo callbackInfo) {
        OldAnimations module = OldAnimations.get();
        if (!OldAnimations.using || module == null || !module.oldThirdPerson.getValue()) {
            return;
        }
        if (!(entity instanceof AbstractClientPlayer) || !((AbstractClientPlayer) entity).isBlocking()) {
            return;
        }
        GlStateManager.translate(0.05F, 0.0F, -0.1F);
        GlStateManager.rotate(-50.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-60.0F, 0.0F, 0.0F, 1.0F);
    }
}
