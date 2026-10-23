package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.IntProperty;
import myau.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Mouse;

/** WASD + 左右键按键显示（对应 FPSMaster 的 Keystrokes） */
public class Keystrokes extends LegitHudModule {
    public final IntProperty keySize = new IntProperty("key-size", 14, 8, 40);
    public final IntProperty gap = new IntProperty("gap", 2, 0, 10);
    public final BooleanProperty showMouse = new BooleanProperty("mouse", true);
    public final ColorProperty activeColor = new ColorProperty("active-color", 0x22C55E);
    public final ColorProperty inactiveColor = new ColorProperty("inactive-color", 0x101014);

    public Keystrokes() {
        super("Keystrokes", 80, 4);
    }

    @Override
    protected String[] getLines() {
        return null;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || mc.gameSettings.showDebugInfo) {
            return;
        }
        float scaleFactor = this.scale.getValue();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFactor, scaleFactor, 0.0F);
        float x = (float) this.offsetX.getValue() / scaleFactor;
        float y = (float) this.offsetY.getValue() / scaleFactor;
        float size = (float) this.keySize.getValue();
        float spacing = size + (float) this.gap.getValue();
        GlStateManager.disableDepth();
        this.drawKey(x + spacing, y, size, "W", mc.gameSettings.keyBindForward.isKeyDown());
        this.drawKey(x, y + spacing, size, "A", mc.gameSettings.keyBindLeft.isKeyDown());
        this.drawKey(x + spacing, y + spacing, size, "S", mc.gameSettings.keyBindBack.isKeyDown());
        this.drawKey(x + spacing * 2.0F, y + spacing, size, "D", mc.gameSettings.keyBindRight.isKeyDown());
        if (this.showMouse.getValue()) {
            this.drawKey(x, y + spacing * 2.0F, size, "LMB", Mouse.isButtonDown(0));
            this.drawKey(x + spacing, y + spacing * 2.0F, size, "RMB", Mouse.isButtonDown(1));
        }
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void drawKey(float x, float y, float size, String label, boolean down) {
        int background = down
                ? 0xFF000000 | this.activeColor.getValue()
                : 0xB0000000 | (this.inactiveColor.getValue() & 0xFFFFFF);
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x, y, x + size, y + size, background);
        RenderUtil.disableRenderState();
        int textColor = down ? 0xFF101010 : 0xFFE6E6EE;
        mc.fontRendererObj.drawString(
                label,
                x + (size - (float) mc.fontRendererObj.getStringWidth(label)) / 2.0F,
                y + (size - (float) mc.fontRendererObj.FONT_HEIGHT) / 2.0F + 1.0F,
                textColor,
                false
        );
    }
}
