package myau.module.legit;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Legit 菜单里文本型 HUD 模块的基类（对应 FPSMaster 的 TextComponent 组件）。
 *
 * <p>子类需要：
 * <ol>
 *   <li>实现 {@link #getLines()} 返回要显示的行；</li>
 *   <li>自己声明一个 {@code @EventTarget public void onRender2D(Render2DEvent)} 并调用 {@link #renderLines()}
 *       —— 事件系统只扫描「本类声明」的方法，继承来的带注解方法不会被注册。</li>
 * </ol>
 *
 * <p>位置用像素偏移 + 缩放表示（ATALOSS 暂无 HUD 拖拽编辑器，用 offset-x/offset-y 定位）。
 */
public abstract class LegitHudModule extends Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty offsetX;
    public final IntProperty offsetY;
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 2.0F);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final PercentProperty background = new PercentProperty("background", 0);

    protected LegitHudModule(String name, int defaultX, int defaultY) {
        // hidden = true：这些是独立 HUD 元素，不再重复出现在 HUD 的模块列表里
        super(name, false, true, true);
        this.offsetX = new IntProperty("offset-x", defaultX, 0, 2000);
        this.offsetY = new IntProperty("offset-y", defaultY, 0, 2000);
    }

    /** 本 tick 要显示的文本行；返回 null 或空数组表示这一 tick 不显示 */
    protected abstract String[] getLines();

    /** 文本颜色（ARGB） */
    protected int getTextColor() {
        return 0xFFFFFFFF;
    }

    /** 供子类取 FPS（Minecraft.getDebugFPS 是静态方法，放在基类里省得每个类都 import） */
    protected static int getFps() {
        return Minecraft.getDebugFPS();
    }

    /** 统一绘制：背景 + 文本（带缩放与阴影） */
    protected void renderLines() {
        if (!this.isEnabled() || mc.thePlayer == null || mc.gameSettings.showDebugInfo) {
            return;
        }
        String[] lines = this.getLines();
        if (lines == null || lines.length == 0) {
            return;
        }
        float scaleFactor = this.scale.getValue();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFactor, scaleFactor, 0.0F);
        float x = (float) this.offsetX.getValue() / scaleFactor;
        float y = (float) this.offsetY.getValue() / scaleFactor;
        float lineHeight = (float) mc.fontRendererObj.FONT_HEIGHT + 1.0F;
        if (this.background.getValue() > 0) {
            int width = 0;
            for (String line : lines) {
                width = Math.max(width, mc.fontRendererObj.getStringWidth(line));
            }
            int alpha = (int) (255.0F * ((float) this.background.getValue() / 100.0F));
            RenderUtil.enableRenderState();
            RenderUtil.drawRect(x - 2.0F, y - 2.0F, x + (float) width + 2.0F, y + (float) lines.length * lineHeight, alpha << 24);
            RenderUtil.disableRenderState();
        }
        GlStateManager.disableDepth();
        for (int i = 0; i < lines.length; i++) {
            float lineY = y + (float) i * lineHeight;
            if (this.shadow.getValue()) {
                mc.fontRendererObj.drawStringWithShadow(lines[i], x, lineY, this.getTextColor());
            } else {
                mc.fontRendererObj.drawString(lines[i], x, lineY, this.getTextColor(), false);
            }
        }
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }
}
