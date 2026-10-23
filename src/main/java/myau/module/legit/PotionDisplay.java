package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.property.properties.BooleanProperty;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** 药水效果列表（对应 FPSMaster 的 PotionDisplay） */
public class PotionDisplay extends LegitHudModule {
    public final BooleanProperty showDuration = new BooleanProperty("duration", true);

    public PotionDisplay() {
        super("Potions", 4, 88);
    }

    @Override
    protected String[] getLines() {
        if (mc.thePlayer == null) {
            return null;
        }
        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects == null || effects.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (PotionEffect effect : effects) {
            if (effect == null || effect.getPotionID() < 0 || effect.getPotionID() >= Potion.potionTypes.length) {
                continue;
            }
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) {
                continue;
            }
            String line = I18n.format(potion.getName());
            if (effect.getAmplifier() > 0) {
                line = line + " " + roman(effect.getAmplifier() + 1);
            }
            if (this.showDuration.getValue()) {
                line = line + " " + duration(effect.getDuration());
            }
            lines.add(line);
        }
        if (lines.isEmpty()) {
            return null;
        }
        Collections.sort(lines);
        return lines.toArray(new String[0]);
    }

    private static String roman(int value) {
        switch (value) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            default:
                return String.valueOf(value);
        }
    }

    /** ticks → m:ss */
    private static String duration(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        this.renderLines();
    }
}
