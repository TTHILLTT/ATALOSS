package myau.module.modules;

import myau.ATALOSS;
import myau.enums.BlinkModules;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;

public class Blink extends Module {
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "PULSE"});
    public final IntProperty ticks = new IntProperty("ticks", 20, 0, 1200);

    public Blink() {
        super("Blink", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (!ATALOSS.blinkManager.getBlinkingModule().equals(BlinkModules.BLINK)) {
                this.setEnabled(false);
            } else {
                if (this.ticks.getValue() > 0 && ATALOSS.blinkManager.countMovement() > (long) this.ticks.getValue()) {
                    switch (this.mode.getValue()) {
                        case 0:
                            this.setEnabled(false);
                            break;
                        case 1:
                            ATALOSS.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                            ATALOSS.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.setEnabled(false);
    }

    @Override
    public void onEnabled() {
        ATALOSS.blinkManager.setBlinkState(false, ATALOSS.blinkManager.getBlinkingModule());
        ATALOSS.blinkManager.setBlinkState(true, BlinkModules.BLINK);
    }

    @Override
    public void onDisabled() {
        ATALOSS.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }
}
