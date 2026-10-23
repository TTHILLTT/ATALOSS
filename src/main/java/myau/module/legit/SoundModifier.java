package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;

/** 主音量调节（对应 FPSMaster 的 SoundModifier），关闭时还原原音量 */
public class SoundModifier extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentProperty volume = new PercentProperty("volume", 100);
    private float previousVolume = -1.0F;

    public SoundModifier() {
        super("SoundModifier", false, true, true);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (this.previousVolume < 0.0F) {
            this.previousVolume = mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
        }
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, (float) this.volume.getValue() / 100.0F);
    }

    @Override
    public void onDisabled() {
        if (this.previousVolume >= 0.0F) {
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, this.previousVolume);
            this.previousVolume = -1.0F;
        }
    }
}
