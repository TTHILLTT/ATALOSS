package myau.ui;

import myau.ATALOSS;
import myau.config.Config;
import myau.module.Module;
import myau.module.legit.LegitHudModule;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Legit HUD 的拖拽编辑器。
 *
 * <p>左键拖动定位，右键关闭该元素，ESC 退出并把位置写回配置。
 * 打开时 HUD 元素本身仍由 {@code Render2DEvent} 正常绘制（HUD 在 GUI 之下），
 * 这里只在其上叠加高亮框，所以拖动时是"所见即所得"。
 */
public class HudEditorScreen extends GuiScreen {
    private static final int ACCENT = 0xFF22C55E;
    private static final int ACCENT_DIM = 0x8022C55E;
    private static final int TEXT_DIM = 0xFFA0A0AA;

    private final List<LegitHudModule> elements = new ArrayList<>();
    private LegitHudModule dragging;
    private int grabOffsetX;
    private int grabOffsetY;

    @Override
    public void initGui() {
        this.elements.clear();
        this.dragging = null;
        for (Module module : ATALOSS.moduleManager.modules.values()) {
            if (module instanceof LegitHudModule && module.isEnabled()) {
                this.elements.add((LegitHudModule) module);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, 0x8C000000);
        for (int x = 0; x < this.width; x += 20) {
            drawRect(x, 0, x + 1, this.height, 0x10FFFFFF);
        }
        for (int y = 0; y < this.height; y += 20) {
            drawRect(0, y, this.width, y + 1, 0x10FFFFFF);
        }

        LegitHudModule hovered = null;
        for (LegitHudModule element : this.elements) {
            int[] bounds = element.getBounds();
            boolean inside = mouseX >= bounds[0] && mouseX <= bounds[0] + bounds[2]
                    && mouseY >= bounds[1] && mouseY <= bounds[1] + bounds[3];
            if (inside && this.dragging == null) {
                hovered = element;
            }
        }
        for (LegitHudModule element : this.elements) {
            int[] bounds = element.getBounds();
            boolean active = element == this.dragging || element == hovered;
            drawRect(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3], active ? 0x40FFFFFF : 0x1AFFFFFF);
            int border = active ? ACCENT : ACCENT_DIM;
            drawRect(bounds[0], bounds[1], bounds[0] + bounds[2], bounds[1] + 1, border);
            drawRect(bounds[0], bounds[1] + bounds[3] - 1, bounds[0] + bounds[2], bounds[1] + bounds[3], border);
            drawRect(bounds[0], bounds[1], bounds[0] + 1, bounds[1] + bounds[3], border);
            drawRect(bounds[0] + bounds[2] - 1, bounds[1], bounds[0] + bounds[2], bounds[1] + bounds[3], border);
            mc.fontRendererObj.drawStringWithShadow(
                    element.getName(), bounds[0], Math.max(2, bounds[1] - 10), active ? ACCENT : TEXT_DIM);
        }

        String title = "HUD Editor";
        mc.fontRendererObj.drawStringWithShadow(
                title, this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2, 8, ACCENT);
        String hint = "Drag to move  |  Right-click to hide  |  ESC to save & exit";
        mc.fontRendererObj.drawStringWithShadow(
                hint, this.width / 2 - mc.fontRendererObj.getStringWidth(hint) / 2, 20, TEXT_DIM);
        if (this.elements.isEmpty()) {
            String empty = "No enabled HUD elements - enable some in Legit Menu first";
            mc.fontRendererObj.drawStringWithShadow(
                    empty, this.width / 2 - mc.fontRendererObj.getStringWidth(empty) / 2, this.height / 2, 0xFFFF6666);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        // 后画的在上层，所以倒序命中
        for (int i = this.elements.size() - 1; i >= 0; i--) {
            LegitHudModule element = this.elements.get(i);
            int[] bounds = element.getBounds();
            if (mouseX < bounds[0] - 1 || mouseX > bounds[0] + bounds[2] + 1
                    || mouseY < bounds[1] - 1 || mouseY > bounds[1] + bounds[3] + 1) {
                continue;
            }
            if (mouseButton == 0) {
                this.dragging = element;
                this.grabOffsetX = mouseX - bounds[0];
                this.grabOffsetY = mouseY - bounds[1];
            } else if (mouseButton == 1) {
                element.setEnabled(false);
                this.elements.remove(i);
            }
            return;
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.dragging != null) {
            this.dragging.setPosition(mouseX - this.grabOffsetX, mouseY - this.grabOffsetY);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        // 原版 GuiScreen.handleMouseInput 不会回调 mouseClickMove（拖动是各屏幕自己转发的），
        // 所以这里必须在 super 之前取出当前事件坐标写到元素上
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (this.dragging != null && Mouse.isButtonDown(0)) {
            this.dragging.setPosition(mouseX - this.grabOffsetX, mouseY - this.grabOffsetY);
        }
        super.handleMouseInput();
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        this.dragging = null;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        try {
            // 存回"当前正在用的配置"（.config load 过的档案），而不是永远写 default
            String active = Config.lastConfig == null ? "default" : Config.lastConfig;
            new Config(active, false).save();
        } catch (Throwable t) {
            System.out.println("[ATALOSS] HUD editor: saving config failed: " + t);
        }
    }
}
