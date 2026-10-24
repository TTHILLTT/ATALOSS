package myau.module.legit;

import myau.module.Module;

/**
 * 隐藏药水粒子（对应 FPSMaster 的 CleanView）。
 *
 * <p>上游做法是直接取消玩家的 {@code EntityLivingBase.updatePotionEffects}，但那会连药水
 * 计时/到期逻辑一起停掉；这里改成在 {@code EffectRenderer.addEffect} 处只拦掉
 * {@code EntitySpellParticleFX}（药水漩涡粒子），不影响任何游戏逻辑。
 */
public class CleanView extends Module {
    public static boolean using = false;

    public CleanView() {
        super("CleanView", false, true, true);
    }

    @Override
    public void onEnabled() {
        using = true;
    }

    @Override
    public void onDisabled() {
        using = false;
    }
}
