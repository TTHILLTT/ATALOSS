package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;

/** 客户端时间锁定（对应 FPSMaster 的 TimeChanger）：只改本地世界时间，不动服务端 */
public class TimeChanger extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty time = new IntProperty("time", 6000, 0, 24000);

    public TimeChanger() {
        super("TimeChanger", false, true, true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || mc.theWorld == null) {
            return;
        }
        mc.theWorld.setWorldTime((long) this.time.getValue());
    }
}
