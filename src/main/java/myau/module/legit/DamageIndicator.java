package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.Render3DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** 命中伤害数字（对应 FPSMaster 的 DamageIndicator） */
public class DamageIndicator extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int MAX_INDICATORS = 64;

    public final BooleanProperty crit = new BooleanProperty("crit-only", false);

    private final List<Indicator> indicators = new ArrayList<>();
    private EntityLivingBase lastAttack;
    private float lastHealth;
    private long lastTick;

    public DamageIndicator() {
        super("DamageIndicator", false, true, true);
    }

    @Override
    public void onDisabled() {
        this.indicators.clear();
        this.lastAttack = null;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || !(event.getTarget() instanceof EntityLivingBase)) {
            return;
        }
        this.lastAttack = (EntityLivingBase) event.getTarget();
        this.lastHealth = this.lastAttack.getHealth();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE || this.lastAttack == null) {
            return;
        }
        float health = this.lastAttack.getHealth();
        float delta = this.lastHealth - health;
        if (delta != 0.0F) {
            if (!this.crit.getValue() || this.lastAttack.hurtTime > 0) {
                while (this.indicators.size() >= MAX_INDICATORS) {
                    this.indicators.remove(0);
                }
                this.indicators.add(new Indicator(delta, this.lastAttack.posX, this.lastAttack.posY, this.lastAttack.posZ));
            }
            this.lastHealth = health;
        }
        this.lastTick = System.currentTimeMillis();
        if (!this.lastAttack.isEntityAlive() || this.lastAttack.getHealth() <= 0.0F) {
            this.lastAttack = null;
        }
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || this.indicators.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        this.indicators.removeIf(indicator -> now - indicator.birth > 1000L);
        for (Indicator indicator : this.indicators) {
            this.render(indicator, now);
        }
    }

    private void render(Indicator indicator, long now) {
        float progress = Math.min(1.0F, (float) (now - indicator.birth) / 1000.0F);
        int alpha = (int) (255.0F * (1.0F - progress));
        String text = String.format("%.2f", Math.abs(indicator.damage));
        FontRenderer font = mc.fontRendererObj;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GlStateManager.translate(
                indicator.x - mc.getRenderManager().viewerPosX,
                indicator.y - mc.getRenderManager().viewerPosY + 1.6 + progress * 0.6,
                indicator.z - mc.getRenderManager().viewerPosZ);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        float scale = 0.022F;
        GlStateManager.scale(-scale, -scale, scale);
        Color color = indicator.damage > 0.0F ? new Color(224, 41, 41, alpha) : new Color(50, 255, 50, alpha);
        font.drawStringWithShadow(text, -font.getStringWidth(text) / 2.0F, 0.0F, color.getRGB());
        GlStateManager.popMatrix();
        GL11.glPopAttrib();
    }

    private static final class Indicator {
        final float damage;
        final double x;
        final double y;
        final double z;
        final long birth = System.currentTimeMillis();

        Indicator(float damage, double x, double y, double z) {
            this.damage = damage;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
