package com.twogether.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Draws the Mixer's contents inside the vat, so they show through Structural Glass. Built from
 * Mekanism's own pieces - Model3D prepared for the fluid, and renderObject - so it looks like the
 * fluid in their tanks. The octagonal interior is filled as one box per row, with the faces
 * between neighbouring rows hidden so the surface reads as a single body of liquid.
 */
public class MixerFluidRenderer implements BlockEntityRenderer<MixerControllerBlockEntity> {

    /** Keeps the fluid off the walls so their faces do not flicker against it. */
    private static final float INSET = 0.01F;

    /** Interior rows as {dz, minDx, maxDx}, north to south. */
    private static final List<int[]> ROWS = interiorRows();

    public MixerFluidRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static List<int[]> interiorRows() {
        Map<Integer, int[]> byRow = new TreeMap<>();
        for (DistillationTowerShape.Offset o : DistillationTowerShape.FULL_R3) {
            if (DistillationTowerShape.WALL_R3.contains(o)) continue;
            byRow.merge(o.dz(), new int[]{o.dz(), o.dx(), o.dx()},
                    (a, b) -> new int[]{a[0], Math.min(a[1], b[1]), Math.max(a[2], b[2])});
        }
        return new ArrayList<>(byRow.values());
    }

    @Override
    public void render(MixerControllerBlockEntity controller, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!controller.isRenderFormed() || controller.getLevel() == null) return;
        FluidStack fluid = controller.getRenderFluid();
        float fill = Math.min(1, controller.getRenderFill());
        if (fluid.isEmpty() || fill <= 0) return;

        BlockPos origin = controller.getBlockPos();
        BlockPos bottom = controller.getRenderInteriorBottom();
        float baseY = bottom.getY() - origin.getY();
        float topY = baseY + fill * controller.getRenderLayers();
        int centerX = bottom.getX() - origin.getX();
        int centerZ = bottom.getZ() - origin.getZ();

        VertexConsumer buffer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        int color = MekanismRenderer.getColorARGB(fluid);
        // The controller is a solid wall block, so its own light is dark: light the fluid with
        // what reaches the inside of the vat instead.
        int interiorLight = LevelRenderer.getLightColor(controller.getLevel(), bottom);
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        for (int i = 0; i < ROWS.size(); i++) {
            int[] row = ROWS.get(i);
            Model3D model = new Model3D()
                    .prepFlowing(fluid)
                    .xBounds(centerX + row[1] + INSET, centerX + row[2] + 1 - INSET)
                    .yBounds(baseY + INSET, topY - INSET)
                    .zBounds(centerZ + row[0] + INSET, centerZ + row[0] + 1 - INSET)
                    .setSideRender(Direction.NORTH, i == 0)
                    .setSideRender(Direction.SOUTH, i == ROWS.size() - 1);
            MekanismRenderer.renderObject(model, pose, buffer, color, interiorLight, overlay, FaceDisplay.FRONT, camera, origin);
        }
    }

    // The vat reaches three blocks past its centre in every direction, and the controller sits on
    // its edge: widen the box to the whole structure or the fluid is culled as you turn.
    @Override
    public AABB getRenderBoundingBox(MixerControllerBlockEntity controller) {
        return new AABB(controller.getBlockPos()).inflate(7);
    }

    @Override
    public boolean shouldRenderOffScreen(MixerControllerBlockEntity controller) {
        return true;
    }
}
