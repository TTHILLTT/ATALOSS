package myau.mixin;

import myau.ATALOSS;
import myau.data.Box;
import myau.event.EventManager;
import myau.events.PickEvent;
import myau.events.RaytraceEvent;
import myau.events.Render3DEvent;
import myau.module.legit.CustomFog;
import myau.module.legit.FreeLook;
import myau.module.legit.MinimizedBobbing;
import myau.module.legit.OldAnimations;
import myau.module.modules.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@SideOnly(Side.CLIENT)
@Mixin(value = {EntityRenderer.class}, priority = 9999)
public abstract class MixinEntityRenderer {
    @Unique
    private Box<Integer> slot = null;
    @Unique
    private Box<ItemStack> using = null;
    @Unique
    private Box<Integer> useCount = null;
    @Shadow
    private Minecraft mc;
    @Shadow
    private float thirdPersonDistance;

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At("HEAD")}
    )
    private void updateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        if (this.mc.thePlayer != null) {
            Scaffold scaffold = (Scaffold) ATALOSS.moduleManager.modules.get(Scaffold.class);
            if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
                int slot = scaffold.getSlot();
                if (slot >= 0) {
                    this.slot = new Box<>(this.mc.thePlayer.inventory.currentItem);
                    this.mc.thePlayer.inventory.currentItem = slot;
                }
            }
            KillAura killAura = (KillAura) ATALOSS.moduleManager.modules.get(KillAura.class);
            if (killAura.isEnabled() && killAura.isBlocking()) {
                this.using = new Box<>(((IAccessorEntityPlayer) this.mc.thePlayer).getItemInUse());
                ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUse(this.mc.thePlayer.inventory.getCurrentItem());
                this.useCount = new Box<>(((IAccessorEntityPlayer) this.mc.thePlayer).getItemInUseCount());
                ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUseCount(69000);
            }
        }
    }

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At("RETURN")}
    )
    private void postUpdateCameraAndRender(float float1, long long2, CallbackInfo callbackInfo) {
        if (this.slot != null) {
            this.mc.thePlayer.inventory.currentItem = this.slot.value;
            this.slot = null;
        }
        if (this.using != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUse(this.using.value);
            this.using = null;
        }
        if (this.useCount != null) {
            ((IAccessorEntityPlayer) this.mc.thePlayer).setItemInUseCount(this.useCount.value);
            this.useCount = null;
        }
    }

    @Inject(
            method = {"updateRenderer"},
            at = {@At("HEAD")}
    )
    private void updateRenderer(CallbackInfo callbackInfo) {
        Scaffold scaffold = (Scaffold) ATALOSS.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled() && scaffold.itemSpoof.getValue()) {
            int slot = scaffold.getSlot();
            if (slot >= 0) {
                this.slot = new Box<>(this.mc.thePlayer.inventory.currentItem);
                this.mc.thePlayer.inventory.currentItem = slot;
            }
        }

        AutoBlockIn autoBlockIn = (AutoBlockIn) ATALOSS.moduleManager.modules.get(AutoBlockIn.class);
        if (autoBlockIn.isEnabled() && autoBlockIn.itemSpoof.getValue()) {
            int slot = autoBlockIn.getSlot();
            if (slot >= 0) {
                this.slot = new Box<>(this.mc.thePlayer.inventory.currentItem);
                this.mc.thePlayer.inventory.currentItem = slot;
            }
        }
    }

    @Inject(
            method = {"updateRenderer"},
            at = {@At("RETURN")}
    )
    private void postUpdateRenderer(CallbackInfo callbackInfo) {
        if (this.slot != null) {
            this.mc.thePlayer.inventory.currentItem = this.slot.value;
            this.slot = null;
        }
    }

    @Inject(
            method = {"renderWorldPass"},
            at = {@At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;renderHand:Z",
                    shift = At.Shift.BEFORE
            )}
    )
    private void renderWorldPass(int integer, float float2, long long3, CallbackInfo callbackInfo) {
        EventManager.call(new Render3DEvent(float2));
    }

    @ModifyConstant(
            method = {"hurtCameraEffect"},
            constant = {@Constant(
                    floatValue = 14.0F,
                    ordinal = 0
            )}
    )
    private float hurtCameraEffect(float float1) {
        if (ATALOSS.moduleManager == null) {
            return float1;
        } else {
            NoHurtCam noHurtCam = (NoHurtCam) ATALOSS.moduleManager.modules.get(NoHurtCam.class);
            return noHurtCam.isEnabled() ? float1 * (float) noHurtCam.multiplier.getValue().intValue() / 100.0F : float1;
        }
    }

    @ModifyConstant(
            method = {"getMouseOver"},
            constant = {@Constant(
                    doubleValue = 3.0,
                    ordinal = 1
            )}
    )
    private double getMouseOver(double range) {
        PickEvent event = new PickEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @ModifyVariable(
            method = {"getMouseOver"},
            at = @At("STORE"),
            name = {"d0"}
    )
    private double storeMouseOver(double range) {
        RaytraceEvent event = new RaytraceEvent(range);
        EventManager.call(event);
        return event.getRange();
    }

    @Inject(
            method = {"getMouseOver"},
            at = {@At(
                    value = "INVOKE",
                    target = "Ljava/util/List;size()I",
                    ordinal = 0
            )},
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void a(
            float float1,
            CallbackInfo callbackInfo,
            Entity entity,
            double double4,
            double double5,
            Vec3 vec36,
            boolean boolean7,
            int integer8,
            Vec3 vec39,
            Vec3 vec310,
            Vec3 vec311,
            float float12,
            List<Entity> list,
            double double14,
            int integer15
    ) {
        if (ATALOSS.moduleManager != null) {
            GhostHand event = (GhostHand) ATALOSS.moduleManager.modules.get(GhostHand.class);
            if (event.isEnabled()) {
                list.removeIf(event::shouldSkip);
            }
        }
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Vec3;distanceTo(Lnet/minecraft/util/Vec3;)D"
            )
    )
    private double v(Vec3 vec31, Vec3 vec32) {
        if (ATALOSS.moduleManager == null) {
            return vec31.distanceTo(vec32);
        } else {
            return ATALOSS.moduleManager.modules.get(ViewClip.class).isEnabled() ? (double) this.thirdPersonDistance : vec31.distanceTo(vec32);
        }
    }

    @Redirect(
            method = {"setupFog"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;"
            )
    )
    private Material x(Block block) {
        if (ATALOSS.moduleManager == null) {
            return block.getMaterial();
        } else {
            return ATALOSS.moduleManager.modules.get(ViewClip.class).isEnabled() ? Material.air : block.getMaterial();
        }
    }

    @Redirect(
            method = {"updateFogColor"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean y(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && ATALOSS.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) ATALOSS.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ataloss$hasPotion(entityLivingBase, potion);
    }

    @Redirect(
            method = {"setupFog"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean q(EntityLivingBase entityLivingBase, Potion potion) {
        if (potion == Potion.blindness && ATALOSS.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) ATALOSS.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.blindness.getValue()) {
                return false;
            }
        }
        return ataloss$hasPotion(entityLivingBase, potion);
    }

    @Redirect(
            method = {"setupCameraTransform"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;isPotionActive(Lnet/minecraft/potion/Potion;)Z"
            )
    )
    private boolean c(EntityPlayerSP entityPlayerSP, Potion potion) {
        if (potion == Potion.confusion && ATALOSS.moduleManager != null) {
            AntiDebuff antiDebuff = (AntiDebuff) ATALOSS.moduleManager.modules.get(AntiDebuff.class);
            if (antiDebuff.isEnabled() && antiDebuff.nausea.getValue()) {
                return false;
            }
        }
        return ataloss$hasPotion(entityPlayerSP, potion);
    }

    @Unique
    private float ataloss$partialTicks;

    @Inject(
            method = {"orientCamera"},
            at = {@At("HEAD")}
    )
    private void ataloss$capturePartialTicks(float partialTicks, CallbackInfo callbackInfo) {
        this.ataloss$partialTicks = partialTicks;
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;getEyeHeight()F"
            )
    )
    private float ataloss$eyeHeight(Entity entity) {
        if (OldAnimations.using && entity == this.mc.thePlayer) {
            return OldAnimations.getClientEyeHeight(this.ataloss$partialTicks);
        }
        return entity.getEyeHeight();
    }

    @Redirect(
            method = {"setupCameraTransform"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/settings/GameSettings;viewBobbing:Z"
            )
    )
    private boolean ataloss$bobbing(GameSettings gameSettings) {
        return gameSettings.viewBobbing && !MinimizedBobbing.using;
    }

    @Shadow
    private float fogColorRed;
    @Shadow
    private float fogColorGreen;
    @Shadow
    private float fogColorBlue;
    @Shadow
    private FloatBuffer fogColorBuffer;

    @Unique
    private boolean ataloss$shouldOverrideFog() {
        if (!CustomFog.using || this.mc.thePlayer == null) {
            return false;
        }
        // 失明药水是硬性的视野限制，覆盖它等于给优势
        if (this.mc.thePlayer.isPotionActive(Potion.blindness)) {
            return false;
        }
        CustomFog module = (CustomFog) ATALOSS.moduleManager.modules.get(CustomFog.class);
        if (module == null) {
            return false;
        }
        if (!module.affectWater.getValue() && this.mc.thePlayer.isInsideOfMaterial(Material.water)) {
            return false;
        }
        if (!module.affectLava.getValue() && this.mc.thePlayer.isInsideOfMaterial(Material.lava)) {
            return false;
        }
        return true;
    }

    @Inject(
            method = {"updateFogColor"},
            at = {@At("RETURN")}
    )
    private void ataloss$fogColor(float partialTicks, CallbackInfo callbackInfo) {
        if (!this.ataloss$shouldOverrideFog()) {
            return;
        }
        CustomFog module = (CustomFog) ATALOSS.moduleManager.modules.get(CustomFog.class);
        if (module == null) {
            return;
        }
        int rgb = module.color.getValue();
        this.fogColorRed = (float) ((rgb >> 16) & 0xFF) / 255.0F;
        this.fogColorGreen = (float) ((rgb >> 8) & 0xFF) / 255.0F;
        this.fogColorBlue = (float) (rgb & 0xFF) / 255.0F;
    }

    @Inject(
            method = {"setupFog"},
            at = {@At("RETURN")}
    )
    private void ataloss$fog(int startCoords, float partialTicks, CallbackInfo callbackInfo) {
        if (!this.ataloss$shouldOverrideFog()) {
            return;
        }
        CustomFog module = (CustomFog) ATALOSS.moduleManager.modules.get(CustomFog.class);
        if (module == null) {
            return;
        }
        int rgb = module.color.getValue();
        float red = (float) ((rgb >> 16) & 0xFF) / 255.0F;
        float green = (float) ((rgb >> 8) & 0xFF) / 255.0F;
        float blue = (float) (rgb & 0xFF) / 255.0F;
        this.fogColorBuffer.clear();
        this.fogColorBuffer.put(red).put(green).put(blue).put(1.0F);
        this.fogColorBuffer.flip();
        GL11.glFog(GL11.GL_FOG_COLOR, this.fogColorBuffer);
        if (module.isLinear()) {
            GlStateManager.setFog(GL11.GL_LINEAR);
            float start = module.startDistance.getValue();
            float end = module.endDistance.getValue();
            if (end <= start) {
                end = start + 1.0F;
            }
            GlStateManager.setFogStart(start);
            GlStateManager.setFogEnd(end);
        } else {
            GlStateManager.setFog(GL11.GL_EXP);
            GlStateManager.setFogDensity(1.0F / Math.max(0.1F, module.endDistance.getValue()));
        }
    }

    @Inject(
            method = {"updateCameraAndRender"},
            at = {@At("HEAD")}
    )
    private void ataloss$freeLook(float partialTicks, long nanoTime, CallbackInfo callbackInfo) {
        FreeLook.overrideMouse();
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F")
    )
    private float ataloss$cameraYaw(Entity entity) {
        return FreeLook.using && FreeLook.active ? FreeLook.cameraYaw : entity.rotationYaw;
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationYaw:F")
    )
    private float ataloss$cameraPrevYaw(Entity entity) {
        return FreeLook.using && FreeLook.active ? FreeLook.prevCameraYaw : entity.prevRotationYaw;
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationPitch:F")
    )
    private float ataloss$cameraPitch(Entity entity) {
        return FreeLook.using && FreeLook.active ? FreeLook.cameraPitch : entity.rotationPitch;
    }

    @Redirect(
            method = {"orientCamera"},
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevRotationPitch:F")
    )
    private float ataloss$cameraPrevPitch(Entity entity) {
        return FreeLook.using && FreeLook.active ? FreeLook.prevCameraPitch : entity.prevRotationPitch;
    }

    /** 不依赖 IAccessorEntityLivingBase 的等价判断（避免接口 Mixin 未应用时抛 ICCE） */
    private static boolean ataloss$hasPotion(net.minecraft.entity.EntityLivingBase entity, net.minecraft.potion.Potion potion) {
        for (net.minecraft.potion.PotionEffect effect : entity.getActivePotionEffects()) {
            if (effect.getPotionID() == potion.id) {
                return true;
            }
        }
        return false;
    }
}
