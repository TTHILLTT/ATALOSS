package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

/**
 * 潜行锁定（对应 FPSMaster 的 ToggleSneak）：按一次 Shift 一直潜行，再按一次解除。
 *
 * <p>纯客户端实现：不改按键绑定，只在每 tick 末尾用 {@link KeyBinding#setKeyBindState} 把潜行键
 * 写回锁定状态 —— 原版在同一个 tick 里会按真实按键把状态重置掉，所以必须放在 POST。
 */
public class ToggleSneak extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean sneaking;

    public ToggleSneak() {
        super("ToggleSneak", false, true, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (!this.isEnabled() || mc.gameSettings == null) {
            return;
        }
        if (event.getKey() == mc.gameSettings.keyBindSneak.getKeyCode()) {
            this.sneaking = !this.sneaking;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.gameSettings == null) {
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), this.sneaking);
    }

    @Override
    public void onDisabled() {
        this.sneaking = false;
        if (mc.gameSettings != null) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }
}
