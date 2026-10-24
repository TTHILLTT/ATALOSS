package myau.util;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.AxisAlignedBB;

/**
 * 用 quad 拼出立方体的 12 条棱。
 *
 * <p>为什么不直接用 {@code GL_LINES}：线的光栅化结果依赖驱动/状态组合，而且和面共面时
 * 很容易整段被深度测试吃掉；用 quad 走的是和填充完全一样的通道，行为可预期。
 * 调用方负责 {@code RenderUtil.enableRenderState()} / {@code disableRenderState()}。
 */
public final class BoxBarRenderer {
    private BoxBarRenderer() {
    }

    public static void drawBars(AxisAlignedBB box, float lineWidth, int red, int green, int blue, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double t = Math.max(0.012D, (double) lineWidth * 0.01D);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
        edge(wr, minX, minY, minZ, maxX, minY, minZ, t, red, green, blue, alpha);
        edge(wr, maxX, minY, minZ, maxX, minY, maxZ, t, red, green, blue, alpha);
        edge(wr, maxX, minY, maxZ, minX, minY, maxZ, t, red, green, blue, alpha);
        edge(wr, minX, minY, maxZ, minX, minY, minZ, t, red, green, blue, alpha);
        edge(wr, minX, maxY, minZ, maxX, maxY, minZ, t, red, green, blue, alpha);
        edge(wr, maxX, maxY, minZ, maxX, maxY, maxZ, t, red, green, blue, alpha);
        edge(wr, maxX, maxY, maxZ, minX, maxY, maxZ, t, red, green, blue, alpha);
        edge(wr, minX, maxY, maxZ, minX, maxY, minZ, t, red, green, blue, alpha);
        edge(wr, minX, minY, minZ, minX, maxY, minZ, t, red, green, blue, alpha);
        edge(wr, maxX, minY, minZ, maxX, maxY, minZ, t, red, green, blue, alpha);
        edge(wr, maxX, minY, maxZ, maxX, maxY, maxZ, t, red, green, blue, alpha);
        edge(wr, minX, minY, maxZ, minX, maxY, maxZ, t, red, green, blue, alpha);
        tessellator.draw();
    }

    private static void edge(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2,
                             double t, int red, int green, int blue, int alpha) {
        double hx = Math.abs(x2 - x1) > 1.0E-6D ? 0.0D : t;
        double hy = Math.abs(y2 - y1) > 1.0E-6D ? 0.0D : t;
        double hz = Math.abs(z2 - z1) > 1.0E-6D ? 0.0D : t;
        cuboid(wr,
                Math.min(x1, x2) - hx, Math.min(y1, y2) - hy, Math.min(z1, z2) - hz,
                Math.max(x1, x2) + hx, Math.max(y1, y2) + hy, Math.max(z1, z2) + hz,
                red, green, blue, alpha);
    }

    private static void cuboid(WorldRenderer wr, double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ, int red, int green, int blue, int alpha) {
        wr.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        wr.pos(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
    }
}
