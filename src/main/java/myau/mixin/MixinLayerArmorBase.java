package myau.mixin;

import myau.module.legit.OldAnimations;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.7 的受伤闪烁（护甲层合并纹理），对应 FPSMaster 的 oldDamage */
@Mixin(LayerArmorBase.class)
public class MixinLayerArmorBase {
    @Inject(method = {"shouldCombineTextures"}, at = {@At("HEAD")}, cancellable = true)
    private void ataloss$oldDamage(CallbackInfoReturnable<Boolean> callbackInfo) {
        OldAnimations module = OldAnimations.get();
        if (OldAnimations.using && module != null && module.oldDamage.getValue()) {
            callbackInfo.setReturnValue(true);
        }
    }
}
