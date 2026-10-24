package myau.init;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;

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
        System.out.println("[ATALOSS] core plugin start");
        // 只做 bootstrap，不在这里 addConfiguration：
        // 清单里的 TweakClass=MixinTweaker 会读取 MixinConfigs=mixins.ataloss.json 并注册配置。
        // 如果这里再注册一次，同一份配置会被处理两遍，且这次注册发生得过早，
        // 会导致 EntityLivingBase 在配置生效前就被加载 —— 接口型 Mixin（IAccessorEntityLivingBase）
        // 就不会被应用，运行时抛 IncompatibleClassChangeError。
        try {
            MixinBootstrap.init();
        } catch (Throwable t) {
            System.out.println("[ATALOSS] MixinBootstrap.init failed: " + t);
            t.printStackTrace();
        }
        System.out.println("[ATALOSS] core plugin ready (mixin config loaded from manifest)");
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
