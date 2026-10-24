package myau.ui.liquid;

import myau.ATALOSS;
import myau.module.Module;
import myau.module.legit.LegitHudModule;
import myau.module.modules.*;
import myau.ui.WardrobeContent;
import myau.ui.WardrobePreview;
import myau.ui.WardrobeRenderer;
import myau.ui.WardrobeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Apple Liquid Glass ClickGUI.
 *
 * Backdrop behaves like real liquid glass:
 *  - backdrop-filter blur (two-stage FBO downsample ~ 20-40px equivalent)
 *  - saturate(180%) via GLSL shader
 *  - large continuous-curvature corners (24-32px)
 *  - soft diffuse shadows
 * Panels enter with a staggered fade-in-up; hover grows/brightens;
 * press squashes (200ms spring ease-out).
 */
public class LiquidClickGui extends GuiScreen {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static LiquidClickGui instance;

    private final List<GlassPanel> panels = new ArrayList<>();
    /** Legit 菜单的面板（只含 isLegit() 的模块，移植自 FPSMaster 的非作弊功能） */
    private final List<GlassPanel> legitPanels = new ArrayList<>();
    /** 衣柜页面是否展开 */
    private boolean wardrobeOpen;
    private final StringBuilder search = new StringBuilder();
    private boolean searchFocused;
    private int searchW = 220;
    private int searchX = 10;
    /** 顶部 Cheat / Legit 菜单切换胶囊 */
    private static final int MENU_SWITCH_W = 154;
    private static final int MENU_SWITCH_H = 20;
    private static final int MENU_SWITCH_Y = 8;

    // backdrop blur pipeline: blurA holds the (full-res) frame copy and
    // later the final blurred result; blurB is the intermediate horizontal
    // pass. Two separable gaussian passes (H then V) give the smooth
    // Windows Terminal acrylic look — no pixelation, no ghosting.
    private Framebuffer blurA;
    private Framebuffer blurB;
    private boolean blurFailed;
    private String blurError;
    // private int lastWheel = 0;        // [DEBUG]
    // private boolean wheelEventSeen;   // [DEBUG]

    public LiquidClickGui() {
        instance = this;
        int i = 0;
        panels.add(new GlassPanel("Combat", category(AimAssist.class, AutoClicker.class, KillAura.class, Wtap.class,
                Velocity.class, Freeze.class, Reach.class, TargetStrafe.class, NoHitDelay.class, AntiFireball.class,
                LagRange.class, HitBox.class, MoreKB.class, Refill.class, HitSelect.class), i++));
        panels.add(new GlassPanel("Movement", category(AntiAFK.class, Fly.class, Speed.class, LongJump.class, Sprint.class,
                SafeWalk.class, Jesus.class, Blink.class, NoFall.class, NoSlow.class, KeepSprint.class, Eagle.class,
                NoJumpDelay.class, AntiVoid.class), i++));
        panels.add(new GlassPanel("Render", category(ESP.class, Chams.class, FullBright.class, Tracers.class, NameTags.class,
                Xray.class, TargetHUD.class, Indicators.class, BedESP.class, ItemESP.class, ViewClip.class, NoHurtCam.class,
                HUD.class, GuiModule.class, ChestESP.class, Trajectories.class, Radar.class), i++));
        panels.add(new GlassPanel("Player", category(AutoHeal.class, AutoTool.class, ChestStealer.class, InvManager.class,
                InvWalk.class, Scaffold.class, Telly.class, AutoBlockIn.class, SpeedMine.class, FastPlace.class,
                GhostHand.class, MCF.class, AntiDebuff.class), i++));
        panels.add(new GlassPanel("Misc", category(Spammer.class, BedNuker.class, BedTracker.class, LightningTracker.class,
                NoRotate.class, NickHider.class, AntiObbyTrap.class, AntiObfuscate.class, AutoAnduril.class,
                InventoryClicker.class), i));

        int px = 14;
        for (GlassPanel p : panels) {
            p.setLocation(px, 50);
            px += p.getWidth() + 12;
        }

        // ===== Legit 菜单面板：HUD 信息类 / 其它 legit 工具 =====
        List<Module> legitHud = new ArrayList<>();
        List<Module> legitMisc = new ArrayList<>();
        for (Module module : ATALOSS.moduleManager.modules.values()) {
            if (!module.isLegit()) {
                continue;
            }
            if (module instanceof LegitHudModule) {
                legitHud.add(module);
            } else {
                legitMisc.add(module);
            }
        }
        legitHud.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        legitMisc.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        if (!legitHud.isEmpty()) {
            this.legitPanels.add(new GlassPanel("Legit HUD", legitHud, 0));
        }
        if (!legitMisc.isEmpty()) {
            this.legitPanels.add(new GlassPanel("Legit Util", legitMisc, this.legitPanels.size()));
        }
        int lx = 14;
        for (GlassPanel p : this.legitPanels) {
            p.setLocation(lx, 50);
            lx += p.getWidth() + 12;
        }
    }

    /** 当前菜单要显示的面板集合（Cheat / Legit） */
    private List<GlassPanel> activePanels() {
        return GuiModule.isLegitMenu() ? this.legitPanels : this.panels;
    }

    public static LiquidClickGui getInstance() {
        return instance;
    }

    /** Blurred backdrop texture id for panels to draw as their frosted-glass background. */
    public static int getBlurTexture() {
        // 轻量 GUI 模式下不提供玻璃纹理，面板会自动走纯色回退分支
        if (!GuiModule.useLiquidGui()) {
            return -1;
        }
        return instance != null && !instance.blurFailed && instance.blurA != null
                ? instance.blurA.framebufferTexture : -1;
    }

    /**
     * 释放玻璃管线占用的 FBO（切到轻量 GUI 时由 GuiModule 调用）。
     * 不销毁实例本身，重新打开时会由 initGui 重新分配。
     */
    public static void shutdown() {
        if (instance != null) {
            instance.disposeFramebuffers();
        }
    }

    private void disposeFramebuffers() {
        if (this.blurA != null) {
            this.blurA.deleteFramebuffer();
            this.blurA = null;
        }
        if (this.blurB != null) {
            this.blurB.deleteFramebuffer();
            this.blurB = null;
        }
    }

    private static List<Module> category(Class<?>... classes) {
        List<Module> list = new ArrayList<>();
        for (Class<?> c : classes) {
            Module m = ATALOSS.moduleManager.getModule(c);
            if (m != null) list.add(m);
        }
        list.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
        return list;
    }

    @Override
    public void initGui() {
        super.initGui();
        // 轻量 GUI 模式：不分配任何 FBO，玻璃管线完全不启动
        if (!GuiModule.useLiquidGui()) {
            return;
        }
        searchX = width / 2 - searchW / 2;
        if (!blurFailed) {
            try {
                if (blurA != null) blurA.deleteFramebuffer();
                if (blurB != null) blurB.deleteFramebuffer();
                // Full-resolution buffers: the two-pass separable gaussian
                // does the blurring, so there is zero downsampling and no
                // pixelation (Windows 11 Terminal acrylic look).
                blurA = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
                blurB = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
                blurA.setFramebufferColor(0, 0, 0, 0);
                blurB.setFramebufferColor(0, 0, 0, 0);
            } catch (Throwable t) {
                blurFailed = true;
                blurError = "LiquidGlass FBO init failed: " + t.getClass().getSimpleName() + " - " + t.getMessage();
                System.out.println("[ATALOSS] LiquidGlass FBO init failed: " + t);
                t.printStackTrace();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 硬保证：轻量 GUI 生效时玻璃界面一个像素都不画（连整帧背景拷贝也不做）
        if (!GuiModule.useLiquidGui()) {
            return;
        }
        renderBlurBackdrop();

        // subtle ambient glow blobs over the glass (reflected color/light)
        GlassRenderer.drawRoundedRect(width * 0.1F, height * 0.08F, width * 0.3F, height * 0.26F, 130, 0x1022C55E);
        GlassRenderer.drawRoundedRect(width * 0.6F, height * 0.6F, width * 0.32F, height * 0.3F, 150, 0x0C7DD3FC);

        // header — liquid glass title pill (true capsule)
        drawGlassCapsuleBg(12, 6, 118, 34, 1.0F);
        mc.fontRendererObj.drawStringWithShadow("ATALOSS", 20, 12, GlassRenderer.ACCENT);
        mc.fontRendererObj.drawStringWithShadow("Dev TTHILLTT", 20, 24, GlassRenderer.TEXT_FAINT);

        // 顶部菜单切换：Cheat / Legit + 衣柜
        this.drawMenuSwitch(mouseX, mouseY);
        this.drawWardrobeButton(mouseX, mouseY);
        if (this.wardrobeOpen) {
            this.drawWardrobePage(mouseX, mouseY);
            return;
        }
        if (this.activePanels().isEmpty()) {
            // 只有 Legit 页可能为空（配置里被裁掉）→ 退回占位卡片
            this.drawLegitPlaceholder();
            return;
        }

        // search bar — same liquid glass refraction as the panels
        int sy = 12;
        int searchH = 20;
        int blurTex = getBlurTexture();
        if (blurTex > 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
            GlStateManager.bindTexture(blurTex);
            GlStateManager.enableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 1);
            GlassRenderer.LiquidGlassShader.INSTANCE.use();
            GlassRenderer.LiquidGlassShader.INSTANCE.setPanelParams(
                    0.5F, 0.5F * searchH / searchW,
                    Math.min(12.0F / searchW, Math.min(0.5F, 0.5F * searchH / searchW)),
                    searchW / (float) sw, searchH / (float) sh,
                    searchX / (float) sw, 1 - sy / (float) sh);
            GlassRenderer.LiquidGlassShader.INSTANCE.setCapsule(true);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(searchX, sy);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(searchX + searchW, sy);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(searchX + searchW, sy + searchH);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(searchX, sy + searchH);
            GL11.glEnd();
            GlassRenderer.LiquidGlassShader.INSTANCE.stop();
            GlStateManager.color(1, 1, 1, 1);
        }
        // soft shadow + hairline (true capsule)
        GlassRenderer.drawCapsuleRect(searchX, sy + 2, searchW, searchH, 0x15000000);
        GlassRenderer.drawCapsuleRect(searchX, sy + 4, searchW, searchH, 0x0C000000);
        GlassRenderer.drawCapsuleRect(searchX, sy + 6, searchW, searchH, 0x05000000);
        GlassRenderer.drawCapsuleOutline(searchX + 0.5F, sy + 0.5F, searchW - 1, searchH - 1, 1.0F, GlassRenderer.GLASS_OUTLINE);
        String hint = search.length() == 0 ? "Search modules..." : search.toString();
        int hintColor = search.length() == 0 ? GlassRenderer.TEXT_FAINT : GlassRenderer.TEXT_MAIN;
        mc.fontRendererObj.drawString(hint, searchX + 10, sy + 6, hintColor);
        if (searchFocused) {
            GlassRenderer.drawRoundedRect(searchX + 10 + mc.fontRendererObj.getStringWidth(hint), sy + 5, 1.2F, 10, 0.6F, 0x9922C55E);
        }
        drawSearchIcon(searchX + searchW - 18, sy + 6);

        // panels（按当前菜单选 Cheat / Legit 面板集合，必须和事件处理用同一套）
        String filter = search.toString();
        for (GlassPanel p : this.activePanels()) {
            p.setFilter(filter);
            p.draw(mouseX, mouseY);
            p.handleDrag(mouseX, mouseY);
        }

        // [DEBUG] scroll diagnostics
        // mc.fontRendererObj.drawStringWithShadow(
        //         "wheel=" + lastWheel + " seen=" + wheelEventSeen, 14, height - 40, 0xFFFFFF55);
        // int dbgY = 60;
        // for (GlassPanel p : panels) {
        //     mc.fontRendererObj.drawStringWithShadow(
        //             p.name + " scroll=" + p.getScroll() + "/" + p.getMaxScroll()
        //                     + " anim=" + p.getAnimScroll()
        //                     + " content=" + p.getContentHeight() + " | " + p.getLastScrollDbg(),
        //             14, dbgY, 0xFFFFFF55);
        //     dbgY += 10;
        // }

        // [DEBUG] backdrop status diagnostics (temporary, for testing)
        // if (blurFailed) {
        //     mc.fontRendererObj.drawStringWithShadow(blurError == null ? "LiquidGlass blur disabled" : blurError,
        //             14, height - 24, 0xFFFF5555);
        // } else if (blurA != null) {
        //     mc.fontRendererObj.drawStringWithShadow("LiquidGlass blur: OK",
        //             14, height - 24, 0xFF55FF55);
        //     if (blurError != null) {
        //         mc.fontRendererObj.drawStringWithShadow(blurError, 14, height - 12, 0xFFFFFF55);
        //     }
        // } else {
        //     mc.fontRendererObj.drawStringWithShadow("LiquidGlass blur: not initialized",
        //             14, height - 24, 0xFFFFFF55);
        // }

        // [DEBUG] vanilla drawRect vs GlassRenderer.drawRect comparison
        // net.minecraft.client.gui.Gui.drawRect(10, 10, 110, 60, 0xFFFF0000);           // vanilla red (top-left)
        // GlassRenderer.drawRect(width - 120, 10, 110, 50, 0xFF0000FF);                  // glass blue (top-right)
        // GlassRenderer.drawRoundedRect(width / 2 - 60, 10, 120, 50, 12, 0xFF00FF00);    // glass green (top-center)
    }

    /**
     * Liquid-glass backdrop: copy the frame to blurA, then run a separable
     * gaussian (horizontal pass blurA -> blurB, vertical pass blurB -> blurA)
     * — the smooth Windows Terminal acrylic blur. Panels sample blurA and
     * only apply the refraction there.
     */
    private void renderBlurBackdrop() {
        if (blurFailed || blurA == null || blurB == null || mc.getFramebuffer() == null
                || mc.getFramebuffer().framebufferTexture == 0) {
            GlassRenderer.drawRect(0, 0, width, height, 0x99050508);
            // [DEBUG] show the failure reason right on screen (no console needed)
            // if (blurError != null) {
            //     mc.fontRendererObj.drawStringWithShadow(blurError, 14, height - 24, 0xFFFF5555);
            // }
            return;
        }
        int srcTex = mc.getFramebuffer().framebufferTexture;

        try {
            // CRITICAL: do NOT touch the projection matrix. We draw in the
            // existing scaled GUI coordinates (0..width, 0..height) — a
            // fullscreen quad there covers the whole viewport regardless of
            // its size. Bind FBOs with raw GL30 calls + explicit viewport:
            // Framebuffer.bindFramebuffer() silently no-ops when isNotInit.
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GlStateManager.disableBlend();
            GlStateManager.disableAlpha();

            // copy frame -> blurA (full resolution, NO gaussian blur — the
            // liquid glass look here is pure edge refraction, like the
            // original shuding liquid-glass effect)
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, blurA.framebufferObject);
            GL11.glViewport(0, 0, blurA.framebufferWidth, blurA.framebufferHeight);
            GlassRenderer.drawTextureQuad(srcTex, width, height);

            // Back to the main framebuffer. The glass is drawn per-panel
            // sampling blurA; the backdrop outside stays untouched.
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
            GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
            // int err3 = GL11.glGetError(); // [DEBUG]

            // [DEBUG] red test bar at bottom (scaled coords)
            // GlStateManager.enableBlend();
            // GlassRenderer.drawRect(0, height - 30, 150, 20, 0x80FF0000);
            // int errRed = GL11.glGetError();
            // [DEBUG] blurA content preview, top-left (scaled coords)
            // GlStateManager.enableBlend();
            // GlassRenderer.drawTextureQuad(blurA.framebufferTexture, 100, 56);
            // blurError = "err1=" + err1 + " err2=" + err2 + " err3=" + err3
            //         + " redbar=" + errRed
            //         + " texA=" + blurA.framebufferTexture + " texB=" + blurB.framebufferTexture
            //         + " mainFbo=" + mc.getFramebuffer().framebufferObject;
        } catch (Throwable t) {
            blurFailed = true;
            blurError = "LiquidGlass blur disabled: " + t.getClass().getSimpleName() + " - " + t.getMessage();
            System.out.println("[ATALOSS] LiquidGlass blur pipeline failed: " + t);
            t.printStackTrace();
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mc.getFramebuffer().framebufferObject);
            GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        } finally {
            // restore default GUI GL state (vanilla text needs all of these)
            GlStateManager.enableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    private int menuSwitchX() {
        return this.width - MENU_SWITCH_W - 12;
    }

    /** 顶部菜单切换胶囊：左半 Cheat、右半 Legit，点击哪半就切到哪个菜单 */
    private void drawMenuSwitch(int mouseX, int mouseY) {
        int bx = this.menuSwitchX();
        int by = MENU_SWITCH_Y;
        int half = MENU_SWITCH_W / 2;
        boolean legit = GuiModule.isLegitMenu();
        boolean hover = mouseX >= bx && mouseX <= bx + MENU_SWITCH_W && mouseY >= by && mouseY <= by + MENU_SWITCH_H;
        drawGlassCapsuleBg(bx, by, MENU_SWITCH_W, MENU_SWITCH_H, hover ? 1.0F : 0.85F);
        // 选中段高亮
        int activeX = legit ? bx + half : bx;
        GlassRenderer.drawCapsuleRect(activeX + 1, by + 1, half - 2, MENU_SWITCH_H - 2, GlassRenderer.ACTIVE_FILL);
        String cheat = "Cheat";
        String legitText = "Legit";
        mc.fontRendererObj.drawString(
                cheat,
                bx + half / 2 - mc.fontRendererObj.getStringWidth(cheat) / 2,
                by + 6,
                legit ? GlassRenderer.TEXT_DIM : GlassRenderer.TEXT_MAIN
        );
        mc.fontRendererObj.drawString(
                legitText,
                bx + half + half / 2 - mc.fontRendererObj.getStringWidth(legitText) / 2,
                by + 6,
                legit ? GlassRenderer.TEXT_MAIN : GlassRenderer.TEXT_DIM
        );
    }

    /** @return 是否命中菜单切换按钮（命中则不再把点击传给面板） */
    private boolean handleMenuSwitchClick(int mouseX, int mouseY, int mouseButton) {
        int bx = this.menuSwitchX();
        int by = MENU_SWITCH_Y;
        if (mouseButton != 0 || mouseX < bx || mouseX > bx + MENU_SWITCH_W || mouseY < by || mouseY > by + MENU_SWITCH_H) {
            return false;
        }
        GuiModule.setMenu(mouseX < bx + MENU_SWITCH_W / 2 ? GuiModule.MENU_CHEAT : GuiModule.MENU_LEGIT);
        return true;
    }

    /** Legit 菜单占位页：先做壳（玻璃卡片 + 标题），后续再加功能 */
    private void drawLegitPlaceholder() {
        int cardW = 300;
        int cardH = 132;
        int x = this.width / 2 - cardW / 2;
        int y = this.height / 2 - cardH / 2;
        int blurTex = getBlurTexture();
        if (blurTex > 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
            GlStateManager.bindTexture(blurTex);
            GlStateManager.enableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.color(1, 1, 1, 0.72F);
            GlassRenderer.LiquidGlassShader.INSTANCE.use();
            GlassRenderer.LiquidGlassShader.INSTANCE.setPanelParams(
                    0.5F, 0.5F * cardH / cardW,
                    Math.min(24.0F / cardW, Math.min(0.5F, 0.5F * cardH / cardW)),
                    cardW / (float) sw, cardH / (float) sh,
                    x / (float) sw, 1 - y / (float) sh);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(x + cardW, y);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(x + cardW, y + cardH);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(x, y + cardH);
            GL11.glEnd();
            GlassRenderer.LiquidGlassShader.INSTANCE.stop();
            GlStateManager.color(1, 1, 1, 1);
        }
        GlassRenderer.drawRoundedRect(x, y + 3, cardW, cardH, 24, 0x15000000);
        GlassRenderer.drawRoundedRect(x, y + 6, cardW, cardH, 24, 0x0C000000);
        GlassRenderer.drawRoundedOutline(x + 0.5F, y + 0.5F, cardW - 1, cardH - 1, 24, 1.0F, GlassRenderer.GLASS_OUTLINE);
        String title = "Legit Menu";
        String hint = "No features yet";
        mc.fontRendererObj.drawStringWithShadow(
                title, this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2, y + 40, GlassRenderer.ACCENT);
        mc.fontRendererObj.drawString(
                hint, this.width / 2 - mc.fontRendererObj.getStringWidth(hint) / 2, y + 60, GlassRenderer.TEXT_DIM);
    }

    private void drawSearchIcon(float cx, float cy) {
        GL11Helper.drawCircle(cx, cy, 2.6F, 0x9922C55E);
        GL11Helper.drawCircle(cx + 4.2F, cy + 4.2F, 1.2F, 0x9922C55E);
    }

    /** Small liquid-glass capsule backdrop: refraction + shadow + hairline,
     *  true semicircle ends (tank tread) — capsule radius = bh / 2. */
    private void drawGlassCapsuleBg(float bx, float by, float bw, float bh, float alpha) {
        int blurTex = getBlurTexture();
        if (blurTex <= 0) {
            GlassRenderer.drawCapsuleRect(bx, by, bw, bh, 0x59FFFFFF);
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
        GlStateManager.bindTexture(blurTex);
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, alpha);
        GlassRenderer.LiquidGlassShader.INSTANCE.use();
        GlassRenderer.LiquidGlassShader.INSTANCE.setPanelParams(
                0.5F, 0.5F * bh / bw,
                Math.min(0.5F * bh / bw, Math.min(0.5F, 0.5F * bh / bw)),
                bw / (float) sw, bh / (float) sh,
                bx / (float) sw, 1 - by / (float) sh);
        GlassRenderer.LiquidGlassShader.INSTANCE.setCapsule(true);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(bx, by);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(bx + bw, by);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(bx + bw, by + bh);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(bx, by + bh);
        GL11.glEnd();
        GlassRenderer.LiquidGlassShader.INSTANCE.stop();
        GlStateManager.color(1, 1, 1, 1);
        // soft shadow + hairline — true capsule
        GlassRenderer.drawCapsuleRect(bx, by + 2, bw, bh, 0x15000000);
        GlassRenderer.drawCapsuleRect(bx, by + 4, bw, bh, 0x0C000000);
        GlassRenderer.drawCapsuleRect(bx, by + 6, bw, bh, 0x05000000);
        GlassRenderer.drawCapsuleOutline(bx + 0.5F, by + 0.5F, bw - 1, bh - 1, 1.0F, GlassRenderer.GLASS_OUTLINE);
    }

    @Override
    public void handleMouseInput() throws java.io.IOException {
        // GuiScreen.handleInput() already consumed the current event with
        // Mouse.next(), so read it directly here — an inner while(Mouse.next())
        // would skip the first (and often only) wheel event.
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            // lastWheel = wheel; wheelEventSeen = true; // [DEBUG]
            int dir = wheel > 0 ? -1 : 1;
            int mx = Mouse.getEventX() * width / mc.displayWidth;
            int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            for (GlassPanel p : this.activePanels()) {
                p.onScroll(mx, my, dir);
            }
        }
        // drag forwarding: while the left button is held, every mouse-motion
        // event reaches the sliders, which live-update under the cursor
        // (dragging is set by mouseDown and cleared by mouseReleased)
        if (Mouse.isButtonDown(0)) {
            int mx = Mouse.getEventX() * width / mc.displayWidth;
            int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
            for (GlassPanel p : this.activePanels()) {
                p.mouseDragged(mx, my);
            }
        }
        super.handleMouseInput();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (this.handleWardrobeClick(mouseX, mouseY, mouseButton)) {
            this.wardrobeOpen = !this.wardrobeOpen;
            return;
        }
        if (this.wardrobeOpen) {
            this.clickWardrobePage(mouseX, mouseY, mouseButton);
            return;
        }
        if (this.handleMenuSwitchClick(mouseX, mouseY, mouseButton)) {
            return;
        }
        searchFocused = mouseX >= searchX && mouseX <= searchX + searchW && mouseY >= 12 && mouseY <= 32;
        for (GlassPanel p : this.activePanels()) {
            p.mouseDown(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        for (GlassPanel p : this.activePanels()) {
            p.mouseReleased(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (this.wardrobeOpen) {
                this.wardrobeOpen = false;
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }
        for (GlassPanel p : this.activePanels()) {
            p.keyTyped(typedChar, keyCode);
        }
        if (!searchFocused) return;
        if (keyCode == Keyboard.KEY_BACK && search.length() > 0) {
            search.deleteCharAt(search.length() - 1);
            return;
        }
        if (typedChar >= 32 && typedChar < 127) {
            search.append(typedChar);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        this.disposeFramebuffers();
    }

    /** Minimal GL helper for the decorative icon. */
    private static final class GL11Helper {
        static void drawCircle(float cx, float cy, float r, int color) {
            float a = ((color >> 24) & 255) / 255.0F;
            float red = ((color >> 16) & 255) / 255.0F;
            float green = ((color >> 8) & 255) / 255.0F;
            float blue = (color & 255) / 255.0F;
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.color(red, green, blue, a);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2d(cx, cy);
            for (int i = 0; i <= 16; i++) {
                double ang = Math.toRadians(i * 360.0 / 16);
                GL11.glVertex2d(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r);
            }
            GL11.glEnd();
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    private static final int WARDROBE_W = 74;
    private static final int WARDROBE_H = 20;

    private int wardrobeX() {
        return this.menuSwitchX() - WARDROBE_W - 6;
    }

    /** 顶栏的"Wardrobe"按钮：打开衣柜快速切披风/皮肤 */
    private void drawWardrobeButton(int mouseX, int mouseY) {
        int bx = this.wardrobeX();
        int by = MENU_SWITCH_Y;
        boolean hover = mouseX >= bx && mouseX <= bx + WARDROBE_W && mouseY >= by && mouseY <= by + WARDROBE_H;
        drawGlassCapsuleBg(bx, by, WARDROBE_W, WARDROBE_H, hover || this.wardrobeOpen ? 1.0F : 0.85F);
        String label = "Wardrobe";
        mc.fontRendererObj.drawString(
                label,
                bx + WARDROBE_W / 2 - mc.fontRendererObj.getStringWidth(label) / 2,
                by + 6,
                hover || this.wardrobeOpen ? GlassRenderer.TEXT_MAIN : GlassRenderer.TEXT_DIM);
    }

    private boolean handleWardrobeClick(int mouseX, int mouseY, int mouseButton) {
        int bx = this.wardrobeX();
        int by = MENU_SWITCH_Y;
        return mouseButton == 0 && mouseX >= bx && mouseX <= bx + WARDROBE_W
                && mouseY >= by && mouseY <= by + WARDROBE_H;
    }

    private static final int TILE_W = 84;
    private static final int TILE_H = 92;
    private static final int TILE_GAP = 8;
    private static final int TILE_COLS_MAX = 6;
    private static final int TILE_ROWS_MAX = 2;
    private static final int WARDROBE_TOP = 56;

    /** 一个衣柜方块：固定显示预览 + 文件名 */
    private static final class WardrobeTile {
        final int x;
        final int y;
        final java.io.File file;
        final boolean cape;

        WardrobeTile(int x, int y, java.io.File file, boolean cape) {
            this.x = x;
            this.y = y;
            this.file = file;
            this.cape = cape;
        }

        boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX <= this.x + TILE_W
                    && mouseY >= this.y && mouseY <= this.y + TILE_H;
        }
    }

    private List<WardrobeTile> buildWardrobeTiles() {
        List<WardrobeTile> tiles = new ArrayList<>();
        int cols = Math.max(1, Math.min(TILE_COLS_MAX, (this.width - 40) / (TILE_W + TILE_GAP)));
        int startX = this.width / 2 - (cols * TILE_W + (cols - 1) * TILE_GAP) / 2;
        int y = this.appendTiles(tiles, WardrobeContent.capes(), true, startX, WARDROBE_TOP + 14, cols);
        this.appendTiles(tiles, WardrobeContent.skins(), false, startX, y + 22, cols);
        return tiles;
    }

    private int appendTiles(List<WardrobeTile> out, List<java.io.File> files, boolean cape,
                            int startX, int y, int cols) {
        int max = cols * TILE_ROWS_MAX;
        int shown = Math.min(files.size(), max);
        for (int i = 0; i < shown; i++) {
            int col = i % cols;
            int row = i / cols;
            out.add(new WardrobeTile(startX + col * (TILE_W + TILE_GAP),
                    y + row * (TILE_H + TILE_GAP), files.get(i), cape));
        }
        int rows = Math.max(1, (shown + cols - 1) / cols);
        return y + rows * (TILE_H + TILE_GAP);
    }

    private void drawWardrobePage(int mouseX, int mouseY) {
        String title = "Wardrobe";
        mc.fontRendererObj.drawStringWithShadow(
                title, this.width / 2 - mc.fontRendererObj.getStringWidth(title) / 2, 34, GlassRenderer.ACCENT);
        String hint = "Capes / Skins  -  click to apply, ESC to close";
        mc.fontRendererObj.drawStringWithShadow(
                hint, this.width / 2 - mc.fontRendererObj.getStringWidth(hint) / 2, 44, GlassRenderer.TEXT_DIM);

        int cols = Math.max(1, Math.min(TILE_COLS_MAX, (this.width - 40) / (TILE_W + TILE_GAP)));
        int startX = this.width / 2 - (cols * TILE_W + (cols - 1) * TILE_GAP) / 2;

        // 分区标题（Capes / Skins）+ 右侧的 None 按钮
        this.drawWardrobeSectionHeader("Capes", startX, WARDROBE_TOP, WardrobeContent.capes().size(),
                WardrobeContent.currentCape() == null ? "None is active" : "None", mouseX, mouseY);
        int skinsHeaderY = WARDROBE_TOP + 14 + Math.max(1,
                (Math.min(WardrobeContent.capes().size(), cols * TILE_ROWS_MAX) + cols - 1) / cols)
                * (TILE_H + TILE_GAP) + 8;
        this.drawWardrobeSectionHeader("Skins", startX, skinsHeaderY, WardrobeContent.skins().size(),
                WardrobeContent.currentSkin() == null ? "None is active" : "None", mouseX, mouseY);

        for (WardrobeTile tile : this.buildWardrobeTiles()) {
            this.drawWardrobeTile(tile, mouseX, mouseY);
        }
    }

    private void drawWardrobeSectionHeader(String name, int x, int y, int count, String noneLabel,
                                           int mouseX, int mouseY) {
        mc.fontRendererObj.drawStringWithShadow(name, x + 2, y, GlassRenderer.ACCENT);
        String files = count + " file(s)";
        mc.fontRendererObj.drawStringWithShadow(files, x + TILE_W - mc.fontRendererObj.getStringWidth(files),
                y, GlassRenderer.TEXT_FAINT);
        int noneWidth = mc.fontRendererObj.getStringWidth(noneLabel) + 12;
        int noneX = x + 2 * TILE_W + TILE_GAP - noneWidth;
        boolean hover = mouseX >= noneX && mouseX <= noneX + noneWidth && mouseY >= y - 1 && mouseY <= y + 9;
        GlassRenderer.drawRoundedRect(noneX, y - 1, noneWidth, 10, 4, hover ? 0x55FFFFFF : 0x33FFFFFF);
        mc.fontRendererObj.drawStringWithShadow(noneLabel, noneX + 6, y,
                hover ? GlassRenderer.TEXT_MAIN : GlassRenderer.TEXT_DIM);
    }

    private void drawWardrobeTile(WardrobeTile tile, int mouseX, int mouseY) {
        boolean selected = this.isTileSelected(tile);
        boolean hover = tile.contains(mouseX, mouseY);
        this.drawRefractionCard(tile.x, tile.y, TILE_W, TILE_H, 10.0F);
        if (selected) {
            GlassRenderer.drawRoundedRect(tile.x, tile.y, TILE_W, TILE_H, 10.0F, GlassRenderer.ACTIVE_FILL);
        } else if (hover) {
            GlassRenderer.drawRoundedRect(tile.x, tile.y, TILE_W, TILE_H, 10.0F, GlassRenderer.HOVER_FILL);
        }

        // 常驻预览：披风按 2:1、皮肤按 1:1 居中塞进预览区
        int previewW = TILE_W - 12;
        int previewH = TILE_H - 30;
        int boxX = tile.x + 6;
        int boxY = tile.y + 4;
        this.draw3DPreview(tile, boxX, boxY, previewW, previewH);

        String label = tile.file.getName();
        if (label.length() > 14) {
            label = label.substring(0, 12) + "..";
        }
        mc.fontRendererObj.drawStringWithShadow(
                label, tile.x + TILE_W / 2 - mc.fontRendererObj.getStringWidth(label) / 2,
                tile.y + TILE_H - 12, selected ? GlassRenderer.ACCENT : GlassRenderer.TEXT_DIM);
    }

    private void drawPreview(java.io.File file, int x, int y, int w, int h) {
        ResourceLocation texture = WardrobePreview.get(file);
        if (texture == null) {
            return;
        }
        GlStateManager.bindTexture(mc.getTextureManager().getTexture(texture).getGlTextureId());
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(x + w, y);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(x + w, y + h);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(x, y + h);
        GL11.glEnd();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean isTileSelected(WardrobeTile tile) {
        String applied = tile.cape ? WardrobeContent.currentCape() : WardrobeContent.currentSkin();
        return applied != null && applied.equals(WardrobeContent.relative(tile.cape, tile.file));
    }

    private void clickWardrobePage(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        for (WardrobeTile tile : this.buildWardrobeTiles()) {
            if (!tile.contains(mouseX, mouseY)) {
                continue;
            }
            if (tile.cape) {
                WardrobeContent.applyCape(tile.file);
            } else {
                WardrobeContent.applySkin(tile.file);
            }
            return;
        }
        // None 按钮区域
        int cols = Math.max(1, Math.min(TILE_COLS_MAX, (this.width - 40) / (TILE_W + TILE_GAP)));
        int startX = this.width / 2 - (cols * TILE_W + (cols - 1) * TILE_GAP) / 2;
        int skinsHeaderY = WARDROBE_TOP + 14 + Math.max(1,
                (Math.min(WardrobeContent.capes().size(), cols * TILE_ROWS_MAX) + cols - 1) / cols)
                * (TILE_H + TILE_GAP) + 8;
        if (this.insideNoneButton(startX, WARDROBE_TOP, mouseX, mouseY, "None")) {
            WardrobeContent.clearCape();
        } else if (this.insideNoneButton(startX, skinsHeaderY, mouseX, mouseY, "None")) {
            WardrobeContent.clearSkin();
        }
    }

    private boolean insideNoneButton(int x, int y, int mouseX, int mouseY, String label) {
        int noneWidth = mc.fontRendererObj.getStringWidth(label) + 12;
        int noneX = x + 2 * TILE_W + TILE_GAP - noneWidth;
        return mouseX >= noneX && mouseX <= noneX + noneWidth && mouseY >= y - 1 && mouseY <= y + 9;
    }

    /** 折射玻璃卡片：和搜索栏/面板用同一个 LiquidGlassShader，而不是纯深色填充 */
    private void drawRefractionCard(float x, float y, float w, float h, float radius) {
        int blurTex = getBlurTexture();
        if (blurTex > 0) {
            ScaledResolution sr = new ScaledResolution(mc);
            int sw = sr.getScaledWidth(), sh = sr.getScaledHeight();
            GlStateManager.bindTexture(blurTex);
            GlStateManager.enableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlassRenderer.LiquidGlassShader.INSTANCE.use();
            GlassRenderer.LiquidGlassShader.INSTANCE.setPanelParams(
                    0.5F, 0.5F * h / w,
                    Math.min(radius / w, Math.min(0.5F, 0.5F * h / w)),
                    w / (float) sw, h / (float) sh,
                    x / (float) sw, 1.0F - y / (float) sh);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0.0F, 0.0F);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1.0F, 0.0F);
            GL11.glVertex2f(x + w, y);
            GL11.glTexCoord2f(1.0F, 1.0F);
            GL11.glVertex2f(x + w, y + h);
            GL11.glTexCoord2f(0.0F, 1.0F);
            GL11.glVertex2f(x, y + h);
            GL11.glEnd();
            GlassRenderer.LiquidGlassShader.INSTANCE.stop();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            // 模糊管线不可用时的兜底：淡一点的半透明底，避免文字糊在场景上
            GlassRenderer.drawRoundedRect(x, y, w, h, radius, 0x66151519);
        }
        // 只保留柔光阴影 + 发丝描边，不再叠深色玻璃体
        GlassRenderer.drawRoundedRect(x, y + 2.0F, w, h, radius, 0x15000000);
        GlassRenderer.drawRoundedRect(x, y + 4.0F, w, h, radius, 0x0C000000);
        GlassRenderer.drawRoundedOutline(x + 0.5F, y + 0.5F, w - 1.0F, h - 1.0F, radius, 1.0F,
                GlassRenderer.GLASS_OUTLINE);
    }

    /** 3D 预览：皮肤画小人、披风画弯折的 cape 模型；失败时回退到平铺贴图 */
    private void draw3DPreview(WardrobeTile tile, int x, int y, int w, int h) {
        ResourceLocation texture = WardrobePreview.get(tile.file);
        if (texture == null) {
            return;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();
        float centerX = x + w / 2.0F;
        // 皮肤模型高 1.5 单位、披风 1.0 单位；由方块高度反推缩放，留 8px 边距
        float modelHeight = tile.cape ? 1.0F : 1.5F;
        float size = (h - 8.0F) / modelHeight;
        // 模型自身从头顶(-0.5)到脚(+1.5)，中点 0.5：让中点落在方块中心
        float centerY = y + h / 2.0F - 0.5F * size;
        try {
            WardrobeRenderer.begin(x, y, w, h, scaleFactor);
            if (tile.cape) {
                WardrobeRenderer.renderCape(texture, centerX, centerY, size, 160.0F, 10.0F);
            } else {
                WardrobeRenderer.renderSkin(texture, centerX, centerY, size, 165.0F, 6.0F);
            }
            WardrobeRenderer.end();
        } catch (Throwable t) {
            WardrobeRenderer.end();
            this.drawPreview(tile.file, x, y, w, h);
        }
    }
}
