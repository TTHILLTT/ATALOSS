package myau.module.modules;

import myau.ATALOSS;
import myau.module.Module;
import myau.property.properties.ModeProperty;
import myau.ui.ClickGui;
import myau.ui.liquid.LiquidClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /** 0 = OpenMyau 原版轻量 GUI（纯 2D 绘制，开销低） */
    public static final int STYLE_LIGHTWEIGHT = 0;
    /** 1 = Liquid Glass GUI（全屏 FBO + 着色器，开销高） */
    public static final int STYLE_LIQUID = 1;
    public final ModeProperty style = new ModeProperty("style", STYLE_LIQUID, new String[]{"LIGHTWEIGHT", "LIQUID"});
    private ClickGui lightweightGui;
    private LiquidClickGui liquidGui;

    public GuiModule() {
        super("ClickGui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    /**
     * 当前是否使用 Liquid Glass GUI。
     * <p>{@link LiquidClickGui} 的 initGui / drawScreen / getBlurTexture 都以它为准，
     * 因此轻量 GUI 生效时，玻璃 GUI 既不会分配 FBO 也不会绘制任何像素 —— 两套界面天然互斥。
     */
    public static boolean useLiquidGui() {
        if (ATALOSS.moduleManager == null) {
            return false;
        }
        GuiModule module = (GuiModule) ATALOSS.moduleManager.modules.get(GuiModule.class);
        return module != null && module.style.getValue() == STYLE_LIQUID;
    }

    @Override
    public void onEnabled() {
        // 这个模块只当"动作开关"用：打开界面后立刻回到 OFF
        setEnabled(false);
        if (this.style.getValue() == STYLE_LIQUID) {
            if (mc.currentScreen instanceof ClickGui) {
                mc.displayGuiScreen(null);
            }
            if (this.liquidGui == null) {
                this.liquidGui = new LiquidClickGui();
            }
            mc.displayGuiScreen(this.liquidGui);
        } else {
            // 轻量模式：先把玻璃管线的 FBO 释放掉，保证它不占显存、不参与渲染
            LiquidClickGui.shutdown();
            if (mc.currentScreen instanceof LiquidClickGui) {
                mc.displayGuiScreen(null);
            }
            if (this.lightweightGui == null) {
                this.lightweightGui = new ClickGui();
            }
            mc.displayGuiScreen(this.lightweightGui);
        }
    }

    @Override
    public void verifyValue(String string) {
        if (!"style".equals(string)) {
            return;
        }
        if (this.style.getValue() != STYLE_LIQUID) {
            LiquidClickGui.shutdown();
        }
        // 切换样式时关掉当前界面，下次打开即按新样式渲染，避免旧界面用旧样式继续画
        if (mc.currentScreen instanceof ClickGui || mc.currentScreen instanceof LiquidClickGui) {
            mc.displayGuiScreen(null);
        }
    }
}
