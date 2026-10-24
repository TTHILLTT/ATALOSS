package myau.init;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.Map;

/**
 * 真正的 FML core plugin（清单里以 {@code FMLCorePlugin} 声明）。
 *
 * <p>以前 ATALOSS 只靠清单里的 {@code TweakClass: MixinTweaker} 让 FML 顺带注入 Mixin，
 * 而 FML 1.8.9 对"没有 core plugin 的普通 jar"的 {@code TweakClass} 处理并不可靠，
 * 失败时是**静默**的（游戏正常启动，但 mixin 一个都没应用，客户端等于不存在）。
 *
 * <p>这里显式接管：初始化 Mixin 并注册配置，失败会直接抛异常并在日志里留下
 * {@code [ATALOSS]} 前缀，便于定位。
 */
@IFMLLoadingPlugin.MCVersion("1.8.9")
@IFMLLoadingPlugin.TransformerExclusions({"myau."})
public class ATALOSSCorePlugin implements IFMLLoadingPlugin {
    public ATALOSSCorePlugin() {
        System.out.println("[ATALOSS] core plugin start (mixins.ataloss.json)");
        try {
            MixinBootstrap.init();
        } catch (Throwable t) {
            System.out.println("[ATALOSS] MixinBootstrap.init failed (continuing): " + t);
            t.printStackTrace();
        }
        try {
            MixinEnvironment.getDefaultEnvironment().addConfiguration("mixins.ataloss.json");
            System.out.println("[ATALOSS] mixin config registered by core plugin");
        } catch (Throwable t) {
            // 清单里的 MixinConfigs/TweakClass 可能已经注册过同一份配置，这里不算失败
            System.out.println("[ATALOSS] addConfiguration skipped: " + t);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
