package myau.mixin;

import myau.module.legit.CleanView;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.particle.EntitySpellParticleFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CleanView：拦掉药水漩涡粒子（只影响视觉，不动药水计时） */
@Mixin(EffectRenderer.class)
public class MixinEffectRenderer {
    @Inject(method = {"addEffect"}, at = {@At("HEAD")}, cancellable = true)
    private void ataloss$cleanView(EntityFX effect, CallbackInfo callbackInfo) {
        if (CleanView.using && effect instanceof EntitySpellParticleFX) {
            callbackInfo.cancel();
        }
    }
}
