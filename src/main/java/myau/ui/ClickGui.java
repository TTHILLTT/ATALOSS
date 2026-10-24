
package myau.ui;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.lwjgl.input.Mouse;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import myau.ATALOSS;
import myau.module.Module;
import myau.module.modules.AimAssist;
import myau.module.modules.AntiAFK;
import myau.module.modules.AntiDebuff;
import myau.module.modules.AntiFireball;
import myau.module.modules.AntiObbyTrap;
import myau.module.modules.AntiObfuscate;
import myau.module.modules.AntiVoid;
import myau.module.modules.AutoAnduril;
import myau.module.modules.AutoBlockIn;
import myau.module.modules.AutoClicker;
import myau.module.modules.AutoHeal;
import myau.module.modules.AutoTool;
import myau.module.modules.BedESP;
import myau.module.modules.BedNuker;
import myau.module.modules.BedTracker;
import myau.module.modules.Blink;
import myau.module.modules.Chams;
import myau.module.modules.ChestESP;
import myau.module.modules.ChestStealer;
import myau.module.modules.ESP;
import myau.module.modules.Eagle;
import myau.module.modules.FastPlace;
import myau.module.modules.Fly;
import myau.module.modules.Freeze;
import myau.module.modules.FullBright;
import myau.module.modules.GhostHand;
import myau.module.modules.GuiModule;
import myau.module.modules.HUD;
import myau.module.modules.HitBox;
import myau.module.modules.HitSelect;
import myau.module.modules.Indicators;
import myau.module.modules.InvManager;
import myau.module.modules.InvWalk;
import myau.module.modules.InventoryClicker;
import myau.module.modules.ItemESP;
import myau.module.modules.Jesus;
import myau.module.modules.KeepSprint;
import myau.module.modules.KillAura;
import myau.module.modules.LagRange;
import myau.module.modules.LightningTracker;
import myau.module.modules.LongJump;
import myau.module.modules.MCF;
import myau.module.modules.MoreKB;
import myau.module.modules.NameTags;
import myau.module.modules.NickHider;
import myau.module.modules.NoFall;
import myau.module.modules.NoHitDelay;
import myau.module.modules.NoHurtCam;
import myau.module.modules.NoJumpDelay;
import myau.module.modules.NoRotate;
import myau.module.modules.NoSlow;
import myau.module.modules.Radar;
import myau.module.modules.Reach;
import myau.module.modules.Refill;
import myau.module.modules.SafeWalk;
import myau.module.modules.Scaffold;
import myau.module.modules.Spammer;
import myau.module.modules.Speed;
import myau.module.modules.SpeedMine;
import myau.module.modules.Sprint;
import myau.module.modules.TargetHUD;
import myau.module.modules.TargetStrafe;
import myau.module.modules.Telly;
import myau.module.modules.Tracers;
import myau.module.modules.Trajectories;
import myau.module.modules.Velocity;
import myau.module.modules.ViewClip;
import myau.module.modules.Wtap;
import myau.module.modules.Xray;
import myau.ui.components.CategoryComponent;
import net.minecraft.client.gui.GuiScreen;

public class ClickGui extends GuiScreen {
    private static ClickGui instance;
    private final File configFile = new File("./config/ATALOSS/", "clickgui.txt");
    private final ArrayList<CategoryComponent> categoryList;
    /** Legit 菜单的分类（只含 isLegit() 的模块） */
    private final ArrayList<CategoryComponent> legitCategoryList;
    /** 顶部 Cheat / Legit 菜单切换按钮 */
    private static final int MENU_SWITCH_W = 150;
    private static final int MENU_SWITCH_H = 14;

    public ClickGui() {
        instance = this;

        List<Module> combatModules = new ArrayList<>();
        combatModules.add(ATALOSS.moduleManager.getModule(AimAssist.class));
        combatModules.add(ATALOSS.moduleManager.getModule(AutoClicker.class));
        combatModules.add(ATALOSS.moduleManager.getModule(KillAura.class));
        combatModules.add(ATALOSS.moduleManager.getModule(Wtap.class));
        combatModules.add(ATALOSS.moduleManager.getModule(Velocity.class));
        combatModules.add(ATALOSS.moduleManager.getModule(Freeze.class));
        combatModules.add(ATALOSS.moduleManager.getModule(Reach.class));
        combatModules.add(ATALOSS.moduleManager.getModule(TargetStrafe.class));
        combatModules.add(ATALOSS.moduleManager.getModule(NoHitDelay.class));
        combatModules.add(ATALOSS.moduleManager.getModule(AntiFireball.class));
        combatModules.add(ATALOSS.moduleManager.getModule(LagRange.class));
        combatModules.add(ATALOSS.moduleManager.getModule(HitBox.class));
        combatModules.add(ATALOSS.moduleManager.getModule(MoreKB.class));
        combatModules.add(ATALOSS.moduleManager.getModule(Refill.class));
        combatModules.add(ATALOSS.moduleManager.getModule(HitSelect.class));

        List<Module> movementModules = new ArrayList<>();
        movementModules.add(ATALOSS.moduleManager.getModule(AntiAFK.class));
        movementModules.add(ATALOSS.moduleManager.getModule(Fly.class));
        movementModules.add(ATALOSS.moduleManager.getModule(Speed.class));
        movementModules.add(ATALOSS.moduleManager.getModule(LongJump.class));
        movementModules.add(ATALOSS.moduleManager.getModule(Sprint.class));
        movementModules.add(ATALOSS.moduleManager.getModule(SafeWalk.class));
        movementModules.add(ATALOSS.moduleManager.getModule(Jesus.class));
        movementModules.add(ATALOSS.moduleManager.getModule(Blink.class));
        movementModules.add(ATALOSS.moduleManager.getModule(NoFall.class));
        movementModules.add(ATALOSS.moduleManager.getModule(NoSlow.class));
        movementModules.add(ATALOSS.moduleManager.getModule(KeepSprint.class));
        movementModules.add(ATALOSS.moduleManager.getModule(Eagle.class));
        movementModules.add(ATALOSS.moduleManager.getModule(NoJumpDelay.class));
        movementModules.add(ATALOSS.moduleManager.getModule(AntiVoid.class));

        List<Module> renderModules = new ArrayList<>();
        renderModules.add(ATALOSS.moduleManager.getModule(ESP.class));
        renderModules.add(ATALOSS.moduleManager.getModule(Chams.class));
        renderModules.add(ATALOSS.moduleManager.getModule(FullBright.class));
        renderModules.add(ATALOSS.moduleManager.getModule(Tracers.class));
        renderModules.add(ATALOSS.moduleManager.getModule(NameTags.class));
        renderModules.add(ATALOSS.moduleManager.getModule(Xray.class));
        renderModules.add(ATALOSS.moduleManager.getModule(TargetHUD.class));
        renderModules.add(ATALOSS.moduleManager.getModule(Indicators.class));
        renderModules.add(ATALOSS.moduleManager.getModule(BedESP.class));
        renderModules.add(ATALOSS.moduleManager.getModule(ItemESP.class));
        renderModules.add(ATALOSS.moduleManager.getModule(ViewClip.class));
        renderModules.add(ATALOSS.moduleManager.getModule(NoHurtCam.class));
        renderModules.add(ATALOSS.moduleManager.getModule(HUD.class));
        renderModules.add(ATALOSS.moduleManager.getModule(GuiModule.class));
        renderModules.add(ATALOSS.moduleManager.getModule(ChestESP.class));
        renderModules.add(ATALOSS.moduleManager.getModule(Trajectories.class));
        renderModules.add(ATALOSS.moduleManager.getModule(Radar.class));

        List<Module> playerModules = new ArrayList<>();
        playerModules.add(ATALOSS.moduleManager.getModule(AutoHeal.class));
        playerModules.add(ATALOSS.moduleManager.getModule(AutoTool.class));
        playerModules.add(ATALOSS.moduleManager.getModule(ChestStealer.class));
        playerModules.add(ATALOSS.moduleManager.getModule(InvManager.class));
        playerModules.add(ATALOSS.moduleManager.getModule(InvWalk.class));
        playerModules.add(ATALOSS.moduleManager.getModule(Scaffold.class));
        playerModules.add(ATALOSS.moduleManager.getModule(Telly.class));
        playerModules.add(ATALOSS.moduleManager.getModule(AutoBlockIn.class));
        playerModules.add(ATALOSS.moduleManager.getModule(SpeedMine.class));
        playerModules.add(ATALOSS.moduleManager.getModule(FastPlace.class));
        playerModules.add(ATALOSS.moduleManager.getModule(GhostHand.class));
        playerModules.add(ATALOSS.moduleManager.getModule(MCF.class));
        playerModules.add(ATALOSS.moduleManager.getModule(AntiDebuff.class));

        List<Module> miscModules = new ArrayList<>();
        miscModules.add(ATALOSS.moduleManager.getModule(Spammer.class));
        miscModules.add(ATALOSS.moduleManager.getModule(BedNuker.class));
        miscModules.add(ATALOSS.moduleManager.getModule(BedTracker.class));
        miscModules.add(ATALOSS.moduleManager.getModule(LightningTracker.class));
        miscModules.add(ATALOSS.moduleManager.getModule(NoRotate.class));
        miscModules.add(ATALOSS.moduleManager.getModule(NickHider.class));
        miscModules.add(ATALOSS.moduleManager.getModule(AntiObbyTrap.class));
        miscModules.add(ATALOSS.moduleManager.getModule(AntiObfuscate.class));
        miscModules.add(ATALOSS.moduleManager.getModule(AutoAnduril.class));
        miscModules.add(ATALOSS.moduleManager.getModule(InventoryClicker.class));

        Comparator<Module> comparator = Comparator.comparing(m -> m.getName().toLowerCase());
        combatModules.sort(comparator);
        movementModules.sort(comparator);
        renderModules.sort(comparator);
        playerModules.sort(comparator);
        miscModules.sort(comparator);

        // Legit 菜单的模块（isLegit()：HUD 信息 / 视觉辅助等非作弊功能）
        List<Module> legitModules = new ArrayList<>();
        for (Module module : ATALOSS.moduleManager.modules.values()) {
            if (module.isLegit()) {
                legitModules.add(module);
            }
        }
        legitModules.sort(comparator);

        Set<Module> registered = new HashSet<>();
        registered.addAll(combatModules);
        registered.addAll(movementModules);
        registered.addAll(renderModules);
        registered.addAll(playerModules);
        registered.addAll(miscModules);
        registered.addAll(legitModules);

        for (Module module : ATALOSS.moduleManager.modules.values()) {
            if (!registered.contains(module)) {
                throw new RuntimeException(module.getClass().getName() + " is unregistered to click gui.");
            }
        }

        this.categoryList = new ArrayList<>();
        int topOffset = 5;


        CategoryComponent combat = new CategoryComponent("Combat", combatModules);
        combat.setY(topOffset);
        categoryList.add(combat);
        topOffset += 20;

        CategoryComponent movement = new CategoryComponent("Movement", movementModules);
        movement.setY(topOffset);
        categoryList.add(movement);
        topOffset += 20;

        CategoryComponent render = new CategoryComponent("Render", renderModules);
        render.setY(topOffset);
        categoryList.add(render);
        topOffset += 20;

        CategoryComponent player = new CategoryComponent("Player", playerModules);
        player.setY(topOffset);
        categoryList.add(player);
        topOffset += 20;

        CategoryComponent misc = new CategoryComponent("Misc", miscModules);
        misc.setY(topOffset);
        categoryList.add(misc);

        // ===== Legit 菜单分类 =====
        this.legitCategoryList = new ArrayList<>();
        if (!legitModules.isEmpty()) {
            CategoryComponent legit = new CategoryComponent("Legit", legitModules);
            legit.setY(5);
            this.legitCategoryList.add(legit);
        }

        loadPositions();
    }

    /** 当前菜单要显示的分类（Cheat / Legit） */
    private ArrayList<CategoryComponent> activeCategories() {
        return GuiModule.isLegitMenu() && !this.legitCategoryList.isEmpty() ? this.legitCategoryList : this.categoryList;
    }

    /** 两套分类都要持久化位置 */
    private ArrayList<CategoryComponent> allCategories() {
        ArrayList<CategoryComponent> all = new ArrayList<>(this.categoryList);
        all.addAll(this.legitCategoryList);
        return all;
    }

    public static ClickGui getInstance() {
        return instance;
    }

    public void initGui() {
        super.initGui();
    }

    private int menuSwitchX() {
        return this.width - MENU_SWITCH_W - 4;
    }

    /** 顶部菜单切换按钮：左半 Cheat、右半 Legit，点哪半切到哪个菜单 */
    private void drawMenuSwitch(int mouseX, int mouseY) {
        int sx = this.menuSwitchX();
        int sy = 4;
        int half = MENU_SWITCH_W / 2;
        boolean legit = GuiModule.isLegitMenu();
        boolean hover = mouseX >= sx && mouseX <= sx + MENU_SWITCH_W && mouseY >= sy && mouseY <= sy + MENU_SWITCH_H;
        drawRect(sx, sy, sx + MENU_SWITCH_W, sy + MENU_SWITCH_H, hover ? 0xE61E1E24 : 0xD218181D);
        int accent = new Color(60, 162, 253).getRGB();
        if (legit) {
            drawRect(sx + half, sy, sx + MENU_SWITCH_W, sy + MENU_SWITCH_H, accent);
        } else {
            drawRect(sx, sy, sx + half, sy + MENU_SWITCH_H, accent);
        }
        String cheat = "Cheat";
        String legitText = "Legit";
        mc.fontRendererObj.drawStringWithShadow(
                cheat,
                sx + half / 2 - mc.fontRendererObj.getStringWidth(cheat) / 2,
                sy + 3,
                legit ? 0xFF9A9AA5 : 0xFFFFFFFF
        );
        mc.fontRendererObj.drawStringWithShadow(
                legitText,
                sx + half + half / 2 - mc.fontRendererObj.getStringWidth(legitText) / 2,
                sy + 3,
                legit ? 0xFFFFFFFF : 0xFF9A9AA5
        );
    }

    /** @return 是否命中菜单切换按钮（命中则不再把点击传给分类面板） */
    private boolean handleMenuSwitchClick(int mouseX, int mouseY, int mouseButton) {
        int sx = this.menuSwitchX();
        int sy = 4;
        if (mouseButton != 0 || mouseX < sx || mouseX > sx + MENU_SWITCH_W || mouseY < sy || mouseY > sy + MENU_SWITCH_H) {
            return false;
        }
        GuiModule.setMenu(mouseX < sx + MENU_SWITCH_W / 2 ? GuiModule.MENU_CHEAT : GuiModule.MENU_LEGIT);
        return true;
    }

    /** Legit 菜单占位页：先做壳，后续再加功能 */
    private void drawLegitPlaceholder() {
        int cardW = 220;
        int cardH = 52;
        int x = this.width / 2 - cardW / 2;
        int y = this.height / 2 - cardH / 2;
        drawRect(x, y, x + cardW, y + cardH, new Color(20, 20, 25, 220).getRGB());
        drawRect(x, y, x + 1, y + cardH, new Color(60, 162, 253).getRGB());
        String title = "Legit Menu";
        String hint = "No features yet";
        mc.fontRendererObj.drawStringWithShadow(
                title, this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2, y + 12, new Color(60, 162, 253).getRGB());
        mc.fontRendererObj.drawString(
                hint, this.width / 2 - mc.fontRendererObj.getStringWidth(hint) / 2, y + 28, 0xFF9A9AA5);
    }

    public void drawScreen(int x, int y, float p) {
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, 100).getRGB());

        mc.fontRendererObj.drawStringWithShadow("ATALOSS " + ATALOSS.version, 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT * 3, new Color(60, 162, 253).getRGB());
        mc.fontRendererObj.drawStringWithShadow("dev, ksyz", 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT * 2, new Color(60, 162, 253).getRGB());
        mc.fontRendererObj.drawStringWithShadow("TTHILLTT Forked", 4, this.height - 3 - mc.fontRendererObj.FONT_HEIGHT, new Color(60, 162, 253).getRGB());

        // 顶部菜单切换：Cheat / Legit + 衣柜
        this.drawMenuSwitch(x, y);
        this.drawWardrobeButton(x, y);
        ArrayList<CategoryComponent> categories = this.activeCategories();
        if (categories.isEmpty()) {
            // 只有 Legit 页可能为空 → 退回占位提示
            this.drawLegitPlaceholder();
            return;
        }

        for (CategoryComponent category : categories) {
            category.render(this.fontRendererObj);
            category.handleDrag(x, y);

            for (Component module : category.getModules()) {
                module.update(x, y);
            }
        }

        int wheel = Mouse.getDWheel();
        if (wheel != 0) {
            int scrollDir = wheel > 0 ? 1 : -1;
            for (CategoryComponent category : categories) {
                category.onScroll(x, y, scrollDir);
            }
        }
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        if (this.handleWardrobeClick(x, y, mouseButton)) {
            this.mc.displayGuiScreen(new WardrobeScreen(this));
            return;
        }
        if (this.handleMenuSwitchClick(x, y, mouseButton)) {
            return;
        }
        Iterator<CategoryComponent> btnCat = this.activeCategories().iterator();
        while (true) {
            CategoryComponent category;
            do {
                do {
                    if (!btnCat.hasNext()) {
                        return;
                    }

                    category = btnCat.next();
                    if (category.insideArea(x, y) && !category.isHovered(x, y) && !category.mousePressed(x, y) && mouseButton == 0) {
                        category.mousePressed(true);
                        category.xx = x - category.getX();
                        category.yy = y - category.getY();
                    }

                    if (category.mousePressed(x, y) && mouseButton == 0) {
                        category.setOpened(!category.isOpened());
                    }

                    if (category.isHovered(x, y) && mouseButton == 0) {
                        category.setPin(!category.isPin());
                    }
                } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component c : category.getModules()) {
                c.mouseDown(x, y, mouseButton);
            }
        }

    }

    public void mouseReleased(int x, int y, int mouseButton) {
        Iterator<CategoryComponent> iterator = this.activeCategories().iterator();

        CategoryComponent categoryComponent;
        while (iterator.hasNext()) {
            categoryComponent = iterator.next();
            if (mouseButton == 0) {
                categoryComponent.mousePressed(false);
            }
        }

        iterator = this.activeCategories().iterator();

        while (true) {
            do {
                do {
                    if (!iterator.hasNext()) {
                        return;
                    }

                    categoryComponent = iterator.next();
                } while (!categoryComponent.isOpened());
            } while (categoryComponent.getModules().isEmpty());

            for (Component component : categoryComponent.getModules()) {
                component.mouseReleased(x, y, mouseButton);
            }
        }
    }

    public void keyTyped(char typedChar, int key) {
        if (key == 1) {
            this.mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> btnCat = this.activeCategories().iterator();

            while (true) {
                CategoryComponent cat;
                do {
                    do {
                        if (!btnCat.hasNext()) {
                            return;
                        }

                        cat = btnCat.next();
                    } while (!cat.isOpened());
                } while (cat.getModules().isEmpty());

                for (Component component : cat.getModules()) {
                    component.keyTyped(typedChar, key);
                }
            }
        }
    }

    public void onGuiClosed() {
        savePositions();
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private void savePositions() {
        JsonObject json = new JsonObject();
        for (CategoryComponent cat : this.allCategories()) {
            JsonObject pos = new JsonObject();
            pos.addProperty("x", cat.getX());
            pos.addProperty("y", cat.getY());
            pos.addProperty("open", cat.isOpened());
            json.add(cat.getName(), pos);
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        if (!configFile.exists()) return;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            for (CategoryComponent cat : this.allCategories()) {
                if (json.has(cat.getName())) {
                    JsonObject pos = json.getAsJsonObject(cat.getName());
                    cat.setX(pos.get("x").getAsInt());
                    cat.setY(pos.get("y").getAsInt());
                    cat.setOpened(pos.get("open").getAsBoolean());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int WARDROBE_W = 72;
    private static final int WARDROBE_H = 14;

    private int wardrobeX() {
        return this.menuSwitchX() - WARDROBE_W - 4;
    }

    private void drawWardrobeButton(int mouseX, int mouseY) {
        int sx = this.wardrobeX();
        int sy = 4;
        boolean hover = mouseX >= sx && mouseX <= sx + WARDROBE_W && mouseY >= sy && mouseY <= sy + WARDROBE_H;
        drawRect(sx, sy, sx + WARDROBE_W, sy + WARDROBE_H, hover ? 0xE61E1E24 : 0xD218181D);
        String label = "Wardrobe";
        mc.fontRendererObj.drawStringWithShadow(
                label, sx + WARDROBE_W / 2 - mc.fontRendererObj.getStringWidth(label) / 2, sy + 3,
                hover ? 0xFFFFFFFF : 0xFF9A9AA5);
    }

    private boolean handleWardrobeClick(int mouseX, int mouseY, int mouseButton) {
        int sx = this.wardrobeX();
        int sy = 4;
        return mouseButton == 0 && mouseX >= sx && mouseX <= sx + WARDROBE_W
                && mouseY >= sy && mouseY <= sy + WARDROBE_H;
    }
}
