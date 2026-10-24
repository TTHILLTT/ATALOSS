package myau.module.legit;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;
import myau.util.RenderUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

/**
 * 准星指向方块的填充/描边（对应 FPSMaster 的 BlockOverlay）。
 *
 * <p>两个 ATALOSS 特有约定：
 * <ol>
 *   <li>{@code Render3DEvent} 里是**摄像机相对坐标**，世界坐标要自己减 {@code renderManager.viewerPos*}；</li>
 *   <li>绘制前必须 {@code RenderUtil.enableRenderState()}，否则纹理/深度都开着，什么都看不到。</li>
 * </ol>
 * 另外 {@link ColorProperty} 只存 RGB（写配置是 {@code %06X}、读回来还 {@code substring(0,6)}），
 * 把 ARGB 塞进去 alpha 会被截断成 0，所以透明度单独用 {@link PercentProperty}。
 */
public class BlockOverlay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /** MixinRenderGlobal 用它决定要不要取消原版的方块选择框 */
    public static boolean using = false;

    public final BooleanProperty fill = new BooleanProperty("fill", true);
    public final BooleanProperty outline = new BooleanProperty("outline", true);
    public final BooleanProperty throughBlock = new BooleanProperty("through-block", false);
    public final FloatProperty width = new FloatProperty("width", 1.5F, 0.1F, 10.0F, this.outline::getValue);
    public final ColorProperty fillColor = new ColorProperty("fill-color", 0xFFFFFF, this.fill::getValue);
    public final PercentProperty fillAlpha = new PercentProperty("fill-alpha", 22, 0, 100, this.fill::getValue);
    public final ColorProperty outlineColor = new ColorProperty("outline-color", 0xFFFFFF, this.outline::getValue);
    public final PercentProperty outlineAlpha = new PercentProperty("outline-alpha", 100, 0, 100, this.outline::getValue);

    public BlockOverlay() {
        super("BlockOverlay", false, true, true);
    }

    @Override
    public void onEnabled() {
        using = true;
    }

    @Override
    public void onDisabled() {
        using = false;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!this.isEnabled() || mc.theWorld == null || mc.thePlayer == null || mc.objectMouseOver == null) {
            return;
        }
        if (mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null) {
            return;
        }
        IBlockState state = mc.theWorld.getBlockState(pos);
        if (state == null || state.getBlock() == null || state.getBlock().getMaterial().isReplaceable()) {
            return;
        }
        double x = (double) pos.getX() - mc.getRenderManager().viewerPosX;
        double y = (double) pos.getY() - mc.getRenderManager().viewerPosY;
        double z = (double) pos.getZ() - mc.getRenderManager().viewerPosZ;
        // 填充略微外扩避免和方块表面 z-fighting；描边再多扩一点，防止被填充面/方块表面盖住
        AxisAlignedBB fillBox = new AxisAlignedBB(
                x - 0.002, y - 0.002, z - 0.002,
                x + 1.002, y + 1.002, z + 1.002);
        AxisAlignedBB outlineBox = new AxisAlignedBB(
                x - 0.02, y - 0.02, z - 0.02,
                x + 1.02, y + 1.02, z + 1.02);

        RenderUtil.enableRenderState();
        if (!this.throughBlock.getValue()) {
            GlStateManager.enableDepth();
        }
        if (this.fill.getValue()) {
            this.drawFill(fillBox, this.fillColor.getValue(), this.fillAlpha.getValue());
        }
        if (this.outline.getValue()) {
            GlStateManager.depthMask(false);
            this.drawOutlineBars(outlineBox, this.outlineColor.getValue(), this.outlineAlpha.getValue(), this.width.getValue());
            GlStateManager.depthMask(true);
        }
        GlStateManager.enableDepth();
        RenderUtil.disableRenderState();
    }

    /** 6 个面 */
    private void drawFill(AxisAlignedBB box, int rgb, int alphaPercent) {
        int a = alpha(rgb, alphaPercent);
        if (a <= 0) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        this.cuboid(wr, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, r(rgb), g(rgb), b(rgb), a);
        tessellator.draw();
    }

    /** 12 条棱画成细长方体（复用 quad 路径，绕开 GL_LINES） */
    private void drawOutlineBars(AxisAlignedBB box, int rgb, int alphaPercent, float lineWidth) {
        int a = alpha(rgb, alphaPercent);
        if (a <= 0) {
            return;
        }
        int red = r(rgb);
        int green = g(rgb);
        int blue = b(rgb);
        double t = Math.max(0.012D, (double) lineWidth * 0.01D);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
        // 底面
        this.edge(wr, minX, minY, minZ, maxX, minY, minZ, t, red, green, blue, a);
        this.edge(wr, maxX, minY, minZ, maxX, minY, maxZ, t, red, green, blue, a);
        this.edge(wr, maxX, minY, maxZ, minX, minY, maxZ, t, red, green, blue, a);
        this.edge(wr, minX, minY, maxZ, minX, minY, minZ, t, red, green, blue, a);
        // 顶面
        this.edge(wr, minX, maxY, minZ, maxX, maxY, minZ, t, red, green, blue, a);
        this.edge(wr, maxX, maxY, minZ, maxX, maxY, maxZ, t, red, green, blue, a);
        this.edge(wr, maxX, maxY, maxZ, minX, maxY, maxZ, t, red, green, blue, a);
        this.edge(wr, minX, maxY, maxZ, minX, maxY, minZ, t, red, green, blue, a);
        // 竖棱
        this.edge(wr, minX, minY, minZ, minX, maxY, minZ, t, red, green, blue, a);
        this.edge(wr, maxX, minY, minZ, maxX, maxY, minZ, t, red, green, blue, a);
        this.edge(wr, maxX, minY, maxZ, maxX, maxY, maxZ, t, red, green, blue, a);
        this.edge(wr, minX, minY, maxZ, minX, maxY, maxZ, t, red, green, blue, a);
        tessellator.draw();
    }

    private static int r(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

    private static int g(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

    private static int b(int rgb) {
        return rgb & 0xFF;
    }

    private static int alpha(int rgb, int alphaPercent) {
        return (int) (255.0F * ((float) Math.min(100, Math.max(0, alphaPercent)) / 100.0F));
    }

    /** 一条棱：沿轴方向取全长，另外两轴各加 t 的厚度 */
    private void edge(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2,
                      double t, int red, int green, int blue, int a) {
        double hx = Math.abs(x2 - x1) > 1.0E-6D ? 0.0D : t;
        double hy = Math.abs(y2 - y1) > 1.0E-6D ? 0.0D : t;
        double hz = Math.abs(z2 - z1) > 1.0E-6D ? 0.0D : t;
        this.cuboid(wr,
                Math.min(x1, x2) - hx, Math.min(y1, y2) - hy, Math.min(z1, z2) - hz,
                Math.max(x1, x2) + hx, Math.max(y1, y2) + hy, Math.max(z1, z2) + hz,
                red, green, blue, a);
    }

    private void cuboid(WorldRenderer wr, double minX, double minY, double minZ,
                        double maxX, double maxY, double maxZ, int red, int green, int blue, int a) {
        // 上
        wr.pos(minX, maxY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(minX, maxY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, maxY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, maxY, minZ).color(red, green, blue, a).endVertex();
        // 下
        wr.pos(minX, minY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, minY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, minY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(minX, minY, maxZ).color(red, green, blue, a).endVertex();
        // 北
        wr.pos(minX, minY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(minX, maxY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, maxY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, minY, minZ).color(red, green, blue, a).endVertex();
        // 南
        wr.pos(minX, minY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, minY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, maxY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(minX, maxY, maxZ).color(red, green, blue, a).endVertex();
        // 西
        wr.pos(minX, minY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(minX, minY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(minX, maxY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(minX, maxY, minZ).color(red, green, blue, a).endVertex();
        // 东
        wr.pos(maxX, minY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, maxY, minZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, maxY, maxZ).color(red, green, blue, a).endVertex();
        wr.pos(maxX, minY, maxZ).color(red, green, blue, a).endVertex();
    }
}
