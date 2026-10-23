package myau.ui.liquid;

import myau.module.Module;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * A module row inside a glass panel.
 * - Hover: brightens + scales up slightly (spring, 200ms)
 * - Press: gentle squash feedback
 * - Right-click expands its properties
 */
public class GlassModuleEntry {
    private static final Minecraft mc = Minecraft.getMinecraft();
    // small, symmetric scale anchored at the ENTRY centre — scaling from the
    // row centre would push a tall entry's bottom controls (e.g. Bind) down
    // onto the next module's row (KillAura: ~11px overlap)
    private static final float HOVER_SCALE = 0.008F;
    private static final float PRESS_SCALE = 0.02F;

    /** true = toggle switch on the right; false = accent fill + bar + dot */
    public static boolean switchStyle = true;

    public final Module module;
    public final GlassPanel panel;
    private boolean expanded;
    private final List<GlassComponent> controls = new ArrayList<>();
    private int y;
    private int w;

    // animation state
    private boolean lastHovered;
    private long hoverStart;
    private boolean pressed;
    private long pressStart;
    private long releaseStart;
    private boolean lastEnabled;   // for the toggle switch knob animation
    private long switchStart;      // clock of the last enable/disable change
    private boolean lastExpanded;  // for the unfold/fold height animation
    private long expandStart;      // clock of the last expand/collapse change

    public GlassModuleEntry(Module module, GlassPanel panel, int width) {
        this.module = module;
        this.panel = panel;
        this.w = width;
        this.controls.addAll(GlassControls.buildFor(module, width));
        long now = System.currentTimeMillis();
        this.hoverStart = now;
        this.pressStart = now;
        this.releaseStart = now;
        this.lastEnabled = module.isEnabled();
        this.lastExpanded = false;
        // settled in the past so the first frame is already at rest
        this.switchStart = now - GlassRenderer.DURATION;
        this.expandStart = now - GlassRenderer.DURATION;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int w) {
        this.w = w;
    }

    /** Exponential chase rate (/s) — ~0.12 per frame at 60fps, like the sliders. */
    private static final float EXPAND_RATE = 8.0F;

    /**
     * Unfold/fold progress shared by getHeight() and draw(): 1 = fully
     * expanded, 0 = collapsed. Exponential approach — the row moves fast when
     * far from its target height and slows down as it closes in, the same
     * dynamic-speed pursuit as the sliders (no spring overshoot). Clock-based
     * so it is deterministic no matter how many times per frame it is read.
     */
    private float expandProgress(long now) {
        if (expanded != lastExpanded) {
            lastExpanded = expanded;
            expandStart = now;
        }
        float t = Math.max(0, (now - expandStart) / 1000.0F);
        float p = 1 - (float) Math.exp(-EXPAND_RATE * t);
        if (t > 0.8F) p = 1;   // snap when essentially settled (99.8% there)
        return expanded ? p : 1 - p;
    }

    /** Full height of all visible controls plus their 4px gaps. */
    private float controlsHeight() {
        float h = 0;
        for (GlassComponent c : controls) {
            if (c.isVisible()) {
                h += c.getHeight() + 4;
            }
        }
        return h;
    }

    public int getHeight() {
        // 22 is the row itself; the controls unfold/fold with the exponential
        // chase so the rows below glide down/up instead of jumping
        long now = System.currentTimeMillis();
        return Math.max(0, (int) (22 + controlsHeight() * expandProgress(now)));
    }

    public void draw(int mouseX, int mouseY, boolean hovered, int alpha) {
        long now = System.currentTimeMillis();

        // hover animation
        if (hovered != lastHovered) {
            hoverStart = now;
            lastHovered = hovered;
        }
        float hoverEase = GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(hoverStart, now)));
        // press squash
        float pressEase;
        if (pressed) {
            pressEase = GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(pressStart, now)));
        } else {
            pressEase = 1 - GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(releaseStart, now)));
        }

        // y is an absolute screen coordinate set by the panel
        int rowY = y;
        float scale = 1 + hoverEase * HOVER_SCALE - pressEase * PRESS_SCALE;
        float midX = panel.getX() + w / 2.0F;
        float midY = rowY + getHeight() / 2.0F;  // entry centre as anchor

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.translate(midX, midY, 0);
        net.minecraft.client.renderer.GlStateManager.scale(scale, scale, 1);
        net.minecraft.client.renderer.GlStateManager.translate(-midX, -midY, 0);

        // row background — tank-tread pill, no accent fill: the on/off state
        // is shown by the toggle switch on the right instead
        if (hoverEase > 0.02F) {
            GlassRenderer.drawCapsuleRect(panel.getX() + 4, rowY, w - 8, 16,
                    GlassRenderer.lerpColor(0x00000000, GlassRenderer.HOVER_FILL, hoverEase));
        }
        // name
        int baseColor = module.isEnabled() ? GlassRenderer.TEXT_MAIN : GlassRenderer.TEXT_DIM;
        int textColor = (baseColor & 0xFFFFFF) | (alpha << 24);
        mc.fontRendererObj.drawString(module.getName(), panel.getX() + 10, rowY + 5, textColor);
        // state indicator: toggle switch OR accent fill + bar + dot (switchable)
        if (switchStyle) {
            // springy knob — ease-out-back overshoot so it bounces elastically
            boolean enabled = module.isEnabled();
            if (enabled != lastEnabled) {
                lastEnabled = enabled;
                switchStart = now;
            }
            float spring = GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(switchStart, now)));
            float kp = enabled ? spring : 1 - spring;
            GlassRenderer.drawCapsule(panel.getX() + w - 30, rowY + 2, 24, 12, kp, GlassRenderer.ACCENT);
        } else if (module.isEnabled()) {
            GlassRenderer.drawCapsuleRect(panel.getX() + 4, rowY, w - 8, 16, GlassRenderer.ACTIVE_FILL);
            GlassRenderer.drawCapsuleRect(panel.getX() + 4, rowY + 4, 2.5F, 8, GlassRenderer.ACCENT);
            GlassRenderer.drawRoundedRect(panel.getX() + w - 13, rowY + 6, 4, 4, 2, GlassRenderer.ACCENT);
        }

        // controls — drawn while the unfold/fold animation runs. Each control
        // is gated to the row's animated height: it only draws once the row
        // has unfolded past its bottom edge, so controls pop in as the row
        // grows and tuck away as it shrinks — they can never spill over the
        // row below, no scissor needed
        float progress = expandProgress(now);
        if (progress > 0.02F) {
            int cy = rowY + 22;
            float rowBottom = rowY + 22 + controlsHeight() * progress;
            for (GlassComponent c : controls) {
                if (!c.isVisible()) continue;
                int ch = c.getHeight();
                if (cy + ch <= rowBottom) {
                    c.setBounds(panel.getX() + 8, cy, w - 16, ch);
                    c.draw(mouseX, mouseY);
                }
                cy += ch + 4;
            }
        }
        net.minecraft.client.renderer.GlStateManager.popMatrix();
    }

    public void mouseDown(int mouseX, int mouseY, int button) {
        int rowY = y;
        if (mouseX >= panel.getX() + 4 && mouseX <= panel.getX() + w - 4 && mouseY >= rowY && mouseY <= rowY + 18) {
            pressed = true;
            pressStart = System.currentTimeMillis();
            if (button == 0) {
                module.toggle();
            } else if (button == 1) {
                expanded = !expanded;
            }
            return;
        }
        if (expanded) {
            for (GlassComponent c : controls) {
                if (c.isVisible()) {
                    c.mouseDown(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseDragged(int mouseX, int mouseY) {
        if (expanded) {
            for (GlassComponent c : controls) {
                if (c.isVisible()) {
                    c.mouseDragged(mouseX, mouseY);
                }
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (pressed) {
            pressed = false;
            releaseStart = System.currentTimeMillis();
        }
        if (expanded) {
            for (GlassComponent c : controls) {
                if (c.isVisible()) {
                    c.mouseReleased(mouseX, mouseY, button);
                }
            }
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (GlassComponent c : controls) {
                if (c.isVisible()) {
                    c.keyTyped(typedChar, keyCode);
                }
            }
        }
    }
}
