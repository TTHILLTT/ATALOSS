package myau.ui.liquid;

import myau.module.Module;
import myau.property.Property;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.property.properties.TextProperty;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** All Liquid Glass property widgets in one place. */
public final class GlassControls {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat DF = new DecimalFormat("0.00", new java.text.DecimalFormatSymbols(Locale.US));

    private GlassControls() {
    }

    public static List<GlassComponent> buildFor(Module module, int width) {
        List<GlassComponent> out = new ArrayList<>();
        List<Property<?>> props = null;
        try {
            props = myau.ATALOSS.propertyManager.properties.get(module.getClass());
        } catch (Exception ignored) {
        }
        if (props != null) {
            for (Property<?> p : props) {
                if (p instanceof BooleanProperty) {
                    out.add(new CheckBox((BooleanProperty) p));
                } else if (p instanceof FloatProperty) {
                    FloatProperty fp = (FloatProperty) p;
                    out.add(new Slider(p.getName(), p::getValue, v -> fp.setValue(v.floatValue()),
                            fp.getMinimum(), fp.getMaximum()));
                } else if (p instanceof IntProperty) {
                    IntProperty ip = (IntProperty) p;
                    out.add(new Slider(p.getName(), p::getValue, v -> ip.setValue(v.intValue()),
                            ip.getMinimum().floatValue(), ip.getMaximum().floatValue()));
                } else if (p instanceof PercentProperty) {
                    PercentProperty pp = (PercentProperty) p;
                    // PercentProperty is Property<Integer> — must set an
                    // Integer, a Float here would CCE in the validator
                    out.add(new Slider(p.getName(), p::getValue, v -> pp.setValue(v.intValue()),
                            pp.getMinimum().floatValue(), pp.getMaximum().floatValue()));
                } else if (p instanceof ModeProperty) {
                    out.add(new ModeCycle((ModeProperty) p));
                } else if (p instanceof ColorProperty) {
                    out.add(new ColorRow((ColorProperty) p));
                } else if (p instanceof TextProperty) {
                    out.add(new TextField((TextProperty) p));
                }
            }
        }
        out.add(new BindButton(module));
        return out;
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /** Toggle switch (BooleanProperty) with a springy animated knob. */
    public static class CheckBox extends GlassComponent {
        private final BooleanProperty prop;
        private long switchStart;   // clock of the last state change
        private boolean lastOn;     // previous value — external toggles re-trigger

        public CheckBox(BooleanProperty prop) {
            this.prop = prop;
            this.h = 14;
            this.lastOn = prop.getValue();
            // settled in the past so the first frame is already at rest
            this.switchStart = System.currentTimeMillis() - GlassRenderer.DURATION;
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            long now = System.currentTimeMillis();
            boolean on = prop.getValue();
            if (on != lastOn) {
                lastOn = on;
                switchStart = now;
            }
            // knob progress with a springy ease-out-back overshoot (~200ms):
            // the knob glides across and bounces elastically before settling
            float spring = GlassRenderer.spring(GlassRenderer.easeOutCubic(GlassRenderer.animate(switchStart, now)));
            float kp = on ? spring : 1 - spring;
            mc.fontRendererObj.drawString(prop.getName(), x + 2, y + 3, on ? GlassRenderer.TEXT_MAIN : GlassRenderer.TEXT_DIM);
            GlassRenderer.drawCapsule(x + w - 26, y + 1, 24, 12, kp, GlassRenderer.ACCENT);
        }

        @Override
        public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                prop.setValue(!prop.getValue());
            }
        }
    }

    /** Rounded track slider (Float / Int / Percent). */
    public static class Slider extends GlassComponent {
        private final String name;
        private final java.util.function.Supplier<Object> getter;
        private final java.util.function.Consumer<Float> setter;
        private final float min, max;
        private float animT;        // animated knob/fill position 0..1
        private float dragTargetT;  // cursor position while dragging
        private boolean dragging;
        private boolean typing;
        private String typed = "";

        public Slider(String name, java.util.function.Supplier<Object> getter,
                      java.util.function.Consumer<Float> setter, float min, float max) {
            this.name = name;
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.h = 16;
            Object v = getter.get();
            float fv = v instanceof Number ? ((Number) v).floatValue() : 0;
            this.animT = (max - min) <= 0 ? 0 : clamp01((fv - min) / (max - min));
        }

        private float value() {
            Object v = getter.get();
            return v instanceof Number ? ((Number) v).floatValue() : 0;
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            float v = value();
            float t = (max - min) <= 0 ? 0 : clamp01((v - min) / (max - min));
            if (dragging) {
                // dynamic-speed pursuit: proportional easing makes the knob
                // fly when it is far from the cursor and ease to a stop as
                // it approaches — it chases the mouse, never jumps to it
                animT += (dragTargetT - animT) * 0.20F;
                if (Math.abs(dragTargetT - animT) < 0.001F) animT = dragTargetT;
                setter.accept(min + (max - min) * animT);   // value follows the knob
            } else {
                // calm glide toward values set by a click or by the module
                animT += (t - animT) * 0.12F;
                if (Math.abs(t - animT) < 0.0005F) animT = t;
            }
            String label = name + ": " + DF.format(v);
            mc.fontRendererObj.drawString(label, x + 2, y + 1, GlassRenderer.TEXT_DIM);
            // track
            int ty = y + h - 4;
            GlassRenderer.drawRoundedRect(x + 2, ty, w - 4, 3, 1.5F, 0x40FFFFFF);
            // fill
            float fillW = (w - 4) * animT;
            if (fillW > 0.5F) {
                GlassRenderer.drawRoundedRect(x + 2, ty, fillW, 3, 1.5F, GlassRenderer.ACCENT);
            }
            // knob (5px tall so it never pokes below the control's height)
            float kx = x + 2 + fillW;
            GlassRenderer.drawRoundedRect(kx - 3, ty - 1.5F, 6, 5, 2.5F, dragging ? 0xFFFFFFFF : 0xE6FFFFFF);
        }

        @Override
        public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                dragging = true;
                // no instant jump — the knob pursues the cursor in draw()
                dragTargetT = clamp01((mouseX - (x + 2)) / (float) (w - 4));
            } else if (button == 1 && isHovered(mouseX, mouseY)) {
                typing = !typing;
                typed = "";
            }
        }

        @Override
        public void mouseDragged(int mouseX, int mouseY) {
            // track the cursor; the knob chases it with dynamic speed in draw()
            if (dragging) {
                dragTargetT = clamp01((mouseX - (x + 2)) / (float) (w - 4));
            }
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY, int button) {
            dragging = false;
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            if (!typing) return;
            if (keyCode == Keyboard.KEY_RETURN) {
                try {
                    setter.accept(Float.parseFloat(typed));
                } catch (Exception ignored) {
                }
                typing = false;
                return;
            }
            if (keyCode == Keyboard.KEY_BACK) {
                if (!typed.isEmpty()) typed = typed.substring(0, typed.length() - 1);
                return;
            }
            if (Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-') {
                typed += typedChar;
            }
        }

        public boolean isTyping() {
            return typing;
        }

        public String getTyped() {
            return typed;
        }
    }

    /** Mode cycle chip (ModeProperty): click to advance. */
    public static class ModeCycle extends GlassComponent {
        private final ModeProperty prop;

        public ModeCycle(ModeProperty prop) {
            this.prop = prop;
            this.h = 14;
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            String name = prop.getName();
            String mode = prop.getModeString();
            int textW = mc.fontRendererObj.getStringWidth(mode);
            mc.fontRendererObj.drawString(name, x + 2, y + 3, GlassRenderer.TEXT_DIM);
            // mode chip (right side, shrink if needed)
            int chipW = Math.min(w - mc.fontRendererObj.getStringWidth(name) - 12, textW + 12);
            if (chipW > 20) {
                GlassRenderer.drawRoundedRect(x + w - chipW - 2, y + 1, chipW, 12, 6,
                        isHovered(mouseX, mouseY) ? 0x33FFFFFF : 0x1FFFFFFF);
                mc.fontRendererObj.drawString(mode, x + w - chipW + 4, y + 3, GlassRenderer.ACCENT);
            } else {
                mc.fontRendererObj.drawString(mode, x + w - textW - 2, y + 3, GlassRenderer.ACCENT);
            }
        }

        @Override
        public void mouseDown(int mouseX, int mouseY, int button) {
            if (isHovered(mouseX, mouseY)) {
                if (button == 0) prop.nextMode();
                else if (button == 1) prop.previousMode();
            }
        }
    }

    /** Keybind button: click then press a key. */
    public static class BindButton extends GlassComponent {
        private final Module module;
        private boolean binding;

        public BindButton(Module module) {
            this.module = module;
            this.h = 14;
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            String bind = module.getKey() == 0 ? "None" : KeyBindUtil.getKeyName(module.getKey());
            String label = binding ? "Press a key..." : "Bind: " + bind;
            mc.fontRendererObj.drawString(label, x + 2, y + 3, binding ? GlassRenderer.ACCENT : GlassRenderer.TEXT_DIM);
        }

        @Override
        public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                binding = !binding;
            } else if (button == 1 && isHovered(mouseX, mouseY)) {
                module.setKey(0);
                binding = false;
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            if (!binding) return;
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                module.setKey(0);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
        }
    }

    /** Compact color picker (ColorProperty): RGB sliders inside one row-expanding group. */
    public static class ColorRow extends GlassComponent {
        private final ColorProperty prop;
        private boolean expanded;
        private final Slider r, g, b;

        public ColorRow(ColorProperty prop) {
            this.prop = prop;
            this.h = 14;
            this.r = new Slider("R", () -> (prop.getValue() >> 16) & 255,
                    v -> setChannel(16, v.intValue()), 0, 255);
            this.g = new Slider("G", () -> (prop.getValue() >> 8) & 255,
                    v -> setChannel(8, v.intValue()), 0, 255);
            this.b = new Slider("B", () -> prop.getValue() & 255,
                    v -> setChannel(0, v.intValue()), 0, 255);
        }

        private void setChannel(int shift, int value) {
            int rgb = prop.getValue();
            int mask = 255 << shift;
            rgb = (rgb & ~mask) | ((value & 255) << shift);
            prop.setValue(rgb);
        }

        @Override
        public int getHeight() {
            // swatch row (14) + R/G/B sliders at y+16/32/48, 16px each -> 64
            return expanded ? 64 : 14;
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            int rgb = prop.getValue() & 0xFFFFFF;
            mc.fontRendererObj.drawString(prop.getName(), x + 2, y + 3, GlassRenderer.TEXT_DIM);
            // color swatch
            GlassRenderer.drawRoundedRect(x + w - 22, y + 1, 20, 12, 4, 0xFF000000 | rgb);
            GlassRenderer.drawRoundedOutline(x + w - 22, y + 1, 20, 12, 4, 1, 0x40FFFFFF);
            if (expanded) {
                r.setBounds(x + 6, y + 16, w - 12, 16);
                g.setBounds(x + 6, y + 32, w - 12, 16);
                b.setBounds(x + 6, y + 48, w - 12, 16);
                r.draw(mouseX, mouseY);
                g.draw(mouseX, mouseY);
                b.draw(mouseX, mouseY);
            }
        }

        @Override
        public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                if (mouseY < y + 14) {
                    expanded = !expanded;
                    return;
                }
            }
            if (expanded) {
                r.mouseDown(mouseX, mouseY, button);
                g.mouseDown(mouseX, mouseY, button);
                b.mouseDown(mouseX, mouseY, button);
            }
        }

        @Override
        public void mouseDragged(int mouseX, int mouseY) {
            if (expanded) {
                r.mouseDragged(mouseX, mouseY);
                g.mouseDragged(mouseX, mouseY);
                b.mouseDragged(mouseX, mouseY);
            }
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY, int button) {
            if (expanded) {
                r.mouseReleased(mouseX, mouseY, button);
                g.mouseReleased(mouseX, mouseY, button);
                b.mouseReleased(mouseX, mouseY, button);
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            if (expanded) {
                r.keyTyped(typedChar, keyCode);
                g.keyTyped(typedChar, keyCode);
                b.keyTyped(typedChar, keyCode);
            }
        }
    }

    /** Inline text field (TextProperty). */
    public static class TextField extends GlassComponent {
        private final TextProperty prop;
        private boolean focused;

        public TextField(TextProperty prop) {
            this.prop = prop;
            this.h = 14;
        }

        @Override
        public void draw(int mouseX, int mouseY) {
            String name = prop.getName();
            String value = prop.getValue();
            mc.fontRendererObj.drawString(name, x + 2, y + 3, GlassRenderer.TEXT_DIM);
            int nameW = mc.fontRendererObj.getStringWidth(name) + 6;
            String shown = focused ? value + "_" : value;
            mc.fontRendererObj.drawString(shown, x + nameW + 2, y + 3, focused ? GlassRenderer.TEXT_MAIN : GlassRenderer.ACCENT);
        }

        @Override
        public void mouseDown(int mouseX, int mouseY, int button) {
            if (button == 0 && isHovered(mouseX, mouseY)) {
                focused = true;
            }
        }

        @Override
        public void keyTyped(char typedChar, int keyCode) {
            if (!focused) return;
            if (keyCode == Keyboard.KEY_BACK) {
                String v = prop.getValue();
                if (!v.isEmpty()) prop.setValue(v.substring(0, v.length() - 1));
                return;
            }
            if (keyCode == Keyboard.KEY_RETURN) {
                focused = false;
                return;
            }
            if (typedChar >= 32) {
                prop.setValue(prop.getValue() + typedChar);
            }
        }
    }
}
