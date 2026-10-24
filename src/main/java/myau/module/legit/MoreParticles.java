package myau.module.legit;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.util.EnumParticleTypes;

/** 命中/击杀的额外粒子特效（对应 FPSMaster 的 MoreParticles，纯客户端） */
public class MoreParticles extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty sharpness = new IntProperty("sharpness", 2, 0, 30);
    public final BooleanProperty alwaysSharpness = new BooleanProperty("always-sharpness", false);
    public final IntProperty crit = new IntProperty("crit", 2, 0, 30);
    public final BooleanProperty alwaysCrit = new BooleanProperty("always-crit", false);
    public final ModeProperty special = new ModeProperty("special", 0, new String[]{"NONE", "HEART", "FLAME", "BLOOD"});
    public final ModeProperty killEffect = new ModeProperty("kill-effect", 0, new String[]{"NONE", "LIGHTNING", "EXPLOSION"});

    private Entity target;
    private Entity lastEffect;

    public MoreParticles() {
        super("MoreParticles", false, true, true);
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || event.getTarget() == null || !event.getTarget().isEntityAlive()) {
            return;
        }
        Entity attacked = event.getTarget();
        this.target = attacked;
        boolean sharp = this.alwaysSharpness.getValue();
        if (!sharp && mc.thePlayer.inventory.getCurrentItem() != null) {
            sharp = mc.thePlayer.inventory.getCurrentItem().getEnchantmentTagList() != null
                    && !mc.thePlayer.inventory.getCurrentItem().getEnchantmentTagList().hasNoTags()
                    && mc.thePlayer.inventory.getCurrentItem().getEnchantmentTagList().toString().contains("id:16s");
        }
        for (int i = 0; i < this.crit.getValue(); i++) {
            mc.effectRenderer.emitParticleAtEntity(attacked, EnumParticleTypes.CRIT);
        }
        if (sharp) {
            for (int i = 0; i < this.sharpness.getValue(); i++) {
                mc.effectRenderer.emitParticleAtEntity(attacked, EnumParticleTypes.CRIT_MAGIC);
            }
        }
        switch (this.special.getValue()) {
            case 1:
                mc.effectRenderer.emitParticleAtEntity(attacked, EnumParticleTypes.HEART);
                break;
            case 2:
                mc.effectRenderer.emitParticleAtEntity(attacked, EnumParticleTypes.FLAME);
                break;
            case 3:
                if (mc.objectMouseOver != null && mc.objectMouseOver.hitVec != null && attacked.hurtResistantTime <= 10) {
                    mc.theWorld.playSound(mc.objectMouseOver.hitVec.xCoord, mc.objectMouseOver.hitVec.yCoord,
                            mc.objectMouseOver.hitVec.zCoord, "dig.stone", 1.0F, 1.0F, false);
                    mc.theWorld.spawnParticle(EnumParticleTypes.REDSTONE,
                            mc.objectMouseOver.hitVec.xCoord, mc.objectMouseOver.hitVec.yCoord,
                            mc.objectMouseOver.hitVec.zCoord, 0.0, 0.0, 0.0);
                }
                break;
            default:
                break;
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.PRE) {
            return;
        }
        if (!(this.target instanceof EntityLivingBase) || this.target == this.lastEffect) {
            return;
        }
        EntityLivingBase entity = (EntityLivingBase) this.target;
        if (entity.getHealth() > 0.0F && entity.isEntityAlive()) {
            return;
        }
        int effect = this.killEffect.getValue();
        if (effect == 1 && mc.theWorld != null) {
            mc.theWorld.addWeatherEffect(new EntityLightningBolt(mc.theWorld, entity.posX, entity.posY, entity.posZ));
            mc.theWorld.playSound(entity.posX, entity.posY, entity.posZ, "random.explode", 1.0F, 1.0F, false);
        } else if (effect == 2) {
            mc.effectRenderer.emitParticleAtEntity(entity, EnumParticleTypes.EXPLOSION_LARGE);
            if (mc.theWorld != null) {
                mc.theWorld.playSound(entity.posX, entity.posY, entity.posZ, "random.explode", 1.0F, 1.0F, false);
            }
        }
        this.lastEffect = entity;
        this.target = null;
    }
}
