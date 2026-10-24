package myau.mixin;

import myau.module.legit.BlockOverlay;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版准星指向方块的黑色细线框（{@code RenderGlobal.drawSelectionBox}）。
 *
 * <p>BlockOverlay 打开时把它取消掉，避免和自定义填充/描边叠在一起。
 * 只对 BLOCK 命中生效，瞄准实体的原版高亮不受影响。
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @Inject(method = {"drawSelectionBox"}, at = {@At("HEAD")}, cancellable = true)
    private void ataloss$blockOverlay(EntityPlayer player, MovingObjectPosition target, int subID, float partialTicks,
                                      CallbackInfo callbackInfo) {
        if (BlockOverlay.using && target != null
                && target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            callbackInfo.cancel();
        }
    }
}
