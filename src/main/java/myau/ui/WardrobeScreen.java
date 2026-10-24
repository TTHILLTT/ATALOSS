package myau.ui;

import myau.ATALOSS;
import myau.module.legit.CustomCape;
import myau.module.legit.CustomSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 衣柜：快速切换 {@code config/ATALOSS/capes/} 与 {@code config/ATALOSS/skins/} 里的 png。
 *
 * <p>点一行即套用（顺便开启对应模块），"None" 关闭对应模块。ESC 回到打开它的那个界面。
 */
public class WardrobeScreen extends GuiScreen {
    private static final int ROW_HEIGHT = 14;
    private static final int MAX_ROWS = 12;
    private static final int PANEL_WIDTH = 190;
    private static final int ACCENT = 0xFF22C55E;
    private static final int TEXT_MAIN = 0xFFF2F4F8;
    private static final int TEXT_DIM = 0xFF8A92A6;

    private final GuiScreen parent;
    private final List<File> capes = new ArrayList<>();
    private final List<File> skins = new ArrayList<>();

    public WardrobeScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.capes.clear();
        this.skins.clear();
        listPng(new File(mc.mcDataDir, "config/ATALOSS/capes"), this.capes);
        listPng(new File(mc.mcDataDir, "config/ATALOSS/skins"), this.skins);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, this.width, this.height, 0xB4000000);

        int leftX = this.width / 2 - PANEL_WIDTH - 10;
        int rightX = this.width / 2 + 10;
        int top = 34;

        String title = "Wardrobe";
        mc.fontRendererObj.drawStringWithShadow(
                title, this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2, 12, ACCENT);
        String hint = "Click to apply  |  ESC to go back";
        mc.fontRendererObj.drawStringWithShadow(
                hint, this.width / 2 - mc.fontRendererObj.getStringWidth(hint) / 2, 22, TEXT_DIM);

        this.drawColumn("Capes", leftX, top, this.capes, this.capePath(), mouseX, mouseY, true);
        this.drawColumn("Skins", rightX, top, this.skins, this.skinPath(), mouseX, mouseY, false);
    }

    private void drawColumn(String name, int x, int y, List<File> files, String applied,
                            int mouseX, int mouseY, boolean cape) {
        drawRect(x, y, x + PANEL_WIDTH, y + 18 + (Math.min(files.size(), MAX_ROWS) + 1) * ROW_HEIGHT + 6, 0x66000000);
        mc.fontRendererObj.drawStringWithShadow(name, x + 6, y + 5, ACCENT);
        String count = files.size() + " file(s)";
        mc.fontRendererObj.drawStringWithShadow(count, x + PANEL_WIDTH - 6 - mc.fontRendererObj.getStringWidth(count), y + 5, TEXT_DIM);

        int rowY = y + 18;
        this.drawRow(x, rowY, "None", applied == null, mouseX, mouseY);
        rowY += ROW_HEIGHT;

        for (int i = 0; i < files.size() && i < MAX_ROWS; i++) {
            File file = files.get(i);
            String label = file.getName();
            if (label.length() > 26) {
                label = label.substring(0, 23) + "...";
            }
            boolean selected = applied != null && applied.equals(relativePath(cape, file));
            this.drawRow(x, rowY, label, selected, mouseX, mouseY);
            rowY += ROW_HEIGHT;
        }
        if (files.size() > MAX_ROWS) {
            mc.fontRendererObj.drawStringWithShadow("... +" + (files.size() - MAX_ROWS) + " more",
                    x + 6, rowY + 2, TEXT_DIM);
        }
    }

    private void drawRow(int x, int y, String label, boolean selected, int mouseX, int mouseY) {
        boolean hover = mouseX >= x + 2 && mouseX <= x + PANEL_WIDTH - 2 && mouseY >= y && mouseY <= y + ROW_HEIGHT - 1;
        if (selected) {
            drawRect(x + 2, y, x + PANEL_WIDTH - 2, y + ROW_HEIGHT - 1, 0x3322C55E);
        } else if (hover) {
            drawRect(x + 2, y, x + PANEL_WIDTH - 2, y + ROW_HEIGHT - 1, 0x22FFFFFF);
        }
        mc.fontRendererObj.drawStringWithShadow(
                (selected ? "> " : "  ") + label, x + 6, y + 3, selected ? ACCENT : (hover ? TEXT_MAIN : TEXT_DIM));
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        int leftX = this.width / 2 - PANEL_WIDTH - 10;
        int rightX = this.width / 2 + 10;
        int top = 34;

        if (this.handleColumn(mouseX, mouseY, leftX, top, this.capes, true)) {
            return;
        }
        this.handleColumn(mouseX, mouseY, rightX, top, this.skins, false);
    }

    private boolean handleColumn(int mouseX, int mouseY, int x, int y, List<File> files, boolean cape) {
        int rowY = y + 18;
        if (this.inside(mouseX, mouseY, x, rowY)) {
            this.clearEntry(cape);
            return true;
        }
        rowY += ROW_HEIGHT;
        for (int i = 0; i < files.size() && i < MAX_ROWS; i++) {
            if (this.inside(mouseX, mouseY, x, rowY)) {
                this.apply(cape, files.get(i));
                return true;
            }
            rowY += ROW_HEIGHT;
        }
        return false;
    }

    private boolean inside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x + 2 && mouseX <= x + PANEL_WIDTH - 2 && mouseY >= y && mouseY <= y + ROW_HEIGHT - 1;
    }

    private void apply(boolean cape, File file) {
        String relative = relativePath(cape, file);
        if (cape) {
            CustomCape module = (CustomCape) ATALOSS.moduleManager.modules.get(CustomCape.class);
            if (module == null) {
                return;
            }
            if (module.isEnabled()) {
                module.path.setValue(relative);
            } else {
                module.path.setValue(relative);
                module.setEnabled(true);
            }
        } else {
            CustomSkin module = (CustomSkin) ATALOSS.moduleManager.modules.get(CustomSkin.class);
            if (module == null) {
                return;
            }
            if (module.isEnabled()) {
                module.path.setValue(relative);
            } else {
                module.path.setValue(relative);
                module.setEnabled(true);
            }
        }
    }

    private void clearEntry(boolean cape) {
        if (cape) {
            CustomCape module = (CustomCape) ATALOSS.moduleManager.modules.get(CustomCape.class);
            if (module != null) {
                module.setEnabled(false);
            }
        } else {
            CustomSkin module = (CustomSkin) ATALOSS.moduleManager.modules.get(CustomSkin.class);
            if (module != null) {
                module.setEnabled(false);
            }
        }
    }

    private String capePath() {
        CustomCape module = (CustomCape) ATALOSS.moduleManager.modules.get(CustomCape.class);
        return module == null || !module.isEnabled() ? null : module.path.getValue();
    }

    private String skinPath() {
        CustomSkin module = (CustomSkin) ATALOSS.moduleManager.modules.get(CustomSkin.class);
        return module == null || !module.isEnabled() ? null : module.path.getValue();
    }

    private static String relativePath(boolean cape, File file) {
        return "config/ATALOSS/" + (cape ? "capes/" : "skins/") + file.getName();
    }

    private static void listPng(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            String name = file.getName().toLowerCase();
            if (file.isFile() && name.endsWith(".png")) {
                out.add(file);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_W) {
            Minecraft.getMinecraft().displayGuiScreen(this.parent);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
