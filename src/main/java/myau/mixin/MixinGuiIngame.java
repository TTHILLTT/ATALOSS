package myau.mixin;

import myau.module.legit.Crosshair;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Crosshair：开启自定义准星时取消原版准星 */
@Mixin(GuiIngame.class)
public class MixinGuiIngame {
    @Inject(method = {"showCrosshair"}, at = {@At("HEAD")}, cancellable = true)
    private void ataloss$crosshair(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (Crosshair.using) {
            callbackInfo.setReturnValue(false);
        }
    }
}
