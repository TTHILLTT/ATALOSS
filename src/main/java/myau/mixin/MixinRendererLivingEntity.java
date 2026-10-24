package myau.mixin;

import myau.ATALOSS;
import myau.event.EventManager;
import myau.event.types.EventType;
import myau.events.RenderLivingEvent;
import myau.module.modules.ESP;
import myau.module.modules.NameTags;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import myau.module.legit.HitColor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import java.nio.FloatBuffer;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(
        value = {RendererLivingEntity.class},
        priority = 9991
)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {
    protected MixinRendererLivingEntity(RenderManager renderManager) {
        super(renderManager);
    }

    @Inject(
            method = {"doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V"},
            at = {@At("HEAD")}
    )
    private void doRender(T entityLivingBase, double double2, double double3, double double4, float float5, float float6, CallbackInfo callbackInfo) {
        EventManager.call(new RenderLivingEvent(EventType.PRE, entityLivingBase));
    }

    @Inject(
            method = {"doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V"},
            at = {@At("RETURN")}
    )
    private void postRender(T entityLivingBase, double double2, double double3, double double4, float float5, float float6, CallbackInfo callbackInfo) {
        EventManager.call(new RenderLivingEvent(EventType.POST, entityLivingBase));
    }

    @Inject(
            method = {"canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void canRenderName(T entityLivingBase, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (ATALOSS.moduleManager != null) {
            NameTags nameTags = (NameTags) ATALOSS.moduleManager.modules.get(NameTags.class);
            if (nameTags.isEnabled() && nameTags.shouldRenderTags(entityLivingBase)) {
                callbackInfoReturnable.setReturnValue(false);
            } else {
                ESP esp = (ESP) ATALOSS.moduleManager.modules.get(ESP.class);
                if (esp.isEnabled() && !esp.isOutlineEnabled()) {
                    callbackInfoReturnable.setReturnValue(false);
                }
            }
        }
    }

    @Shadow
    protected FloatBuffer brightnessBuffer;
    @Shadow
    @Final
    private static DynamicTexture textureBrightness;
    @Shadow
    protected abstract int getColorMultiplier(T entity, float brightness, float partialTicks);

    /** HitColor：只替换 hurt/death 时写进 brightness buffer 的颜色 */
    @Inject(method = {"setBrightness"}, at = {@At("HEAD")}, cancellable = true)
    protected void ataloss$hitColor(T entity, float partialTicks, boolean combineTextures,
                                    CallbackInfoReturnable<Boolean> callbackInfo) {
        if (!HitColor.using) {
            return;
        }
        float brightness = entity.getBrightness(partialTicks);
        int color = this.getColorMultiplier(entity, brightness, partialTicks);
        boolean hasColor = (color >> 24 & 255) > 0;
        boolean hurt = entity.hurtTime > 0 || entity.deathTime > 0;
        if (!hasColor && !hurt) {
            callbackInfo.setReturnValue(false);
            return;
        }
        if (!hasColor && !combineTextures) {
            callbackInfo.setReturnValue(false);
            return;
        }
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, OpenGlHelper.GL_INTERPOLATE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_CONSTANT);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE2_RGB, OpenGlHelper.GL_CONSTANT);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND2_RGB, 770);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        this.brightnessBuffer.position(0);
        if (hurt) {
            this.brightnessBuffer.put(HitColor.red());
            this.brightnessBuffer.put(HitColor.green());
            this.brightnessBuffer.put(HitColor.blue());
            this.brightnessBuffer.put(HitColor.alpha());
        } else {
            float a = (float) (color >> 24 & 255) / 255.0F;
            float r = (float) (color >> 16 & 255) / 255.0F;
            float g = (float) (color >> 8 & 255) / 255.0F;
            float b = (float) (color & 255) / 255.0F;
            this.brightnessBuffer.put(r);
            this.brightnessBuffer.put(g);
            this.brightnessBuffer.put(b);
            this.brightnessBuffer.put(1.0F - a);
        }
        this.brightnessBuffer.flip();
        GL11.glTexEnv(8960, 8705, this.brightnessBuffer);
        GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(textureBrightness.getGlTextureId());
        GL11.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.lightmapTexUnit);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
        GL11.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        callbackInfo.setReturnValue(true);
    }
}
