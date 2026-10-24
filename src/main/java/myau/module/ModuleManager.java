package myau.module;

import myau.ATALOSS;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.modules.GuiModule;
import myau.module.modules.HUD;
import myau.util.ChatUtil;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;
import myau.util.SoundUtil;

import java.util.LinkedHashMap;

public class ModuleManager {
    private boolean sound = false;
    /** 鼠标按键边沿检测（索引 = LWJGL button） */
    private final boolean[] mouseDown = new boolean[8];
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String string) {
        return this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public Module getModule(Class<?> clazz){
        return this.modules.get(clazz);
    }

    public void playSound() {
        this.sound = true;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        this.handleBind(event.getKey());
    }

    /** 处理一次"按键绑定被按下"（键盘事件与鼠标轮询共用） */
    public void handleBind(int keyCode) {
        for (Module module : this.modules.values()) {
            if (module.getKey() != keyCode || module.usesKeyAsInput()) {
                continue;
            }
            boolean shouldNotify = module.toggle();
            HUD hud = (HUD) this.modules.get(HUD.class);
            if (hud != null && shouldNotify) {
                shouldNotify = hud.toggleAlerts.getValue();
            }
            if(module instanceof GuiModule){
                shouldNotify = false;
            }
            if (shouldNotify) {
                String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                String message = String.format("%s%s: %s&r", ATALOSS.clientName, module.getName(), status);
                ChatUtil.sendFormatted(message);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }
        if (this.sound) {
            this.sound = false;
            SoundUtil.playSound("random.click");
        }
        // 鼠标绑定：中键与侧键（跳过左右键，避免和攻击/使用打架）
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) {
            for (int i = 0; i < this.mouseDown.length; i++) {
                this.mouseDown[i] = false;
            }
            return;
        }
        int count = Math.min(this.mouseDown.length, Mouse.getButtonCount());
        for (int button = 2; button < count; button++) {
            boolean down = Mouse.isButtonDown(button);
            if (down && !this.mouseDown[button]) {
                this.handleBind(KeyBindUtil.mouseKey(button));
            }
            this.mouseDown[button] = down;
        }
    }
}
