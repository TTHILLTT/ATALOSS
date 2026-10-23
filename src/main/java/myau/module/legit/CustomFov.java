package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

/** 固定 FOV（对应 FPSMaster 的 CustomFOV）：开启时把视场角锁在设定值，关闭时还原 */
public class CustomFov extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty fov = new IntProperty("fov", 90, 30, 170);
    private float previousFov = -1.0F;

    public CustomFov() {
        super("CustomFov", false, true, true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (this.previousFov < 0.0F) {
            this.previousFov = mc.gameSettings.fovSetting;
        }
        mc.gameSettings.fovSetting = (float) this.fov.getValue();
    }

    @Override
    public void onDisabled() {
        if (this.previousFov > 0.0F) {
            mc.gameSettings.fovSetting = this.previousFov;
            this.previousFov = -1.0F;
        }
    }
}
