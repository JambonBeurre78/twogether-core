package com.twogether.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mekanism.generators.client.model.ModelTurbine;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws a rotor's blades exactly the way Mekanism draws a turbine rotor's: their own
 * ModelTurbine, with RenderTurbineRotor's transforms - a lower blade offset by -1, an upper one by
 * -0.5, each given its index up the shaft (two per rotor) so the model staggers them into a helix.
 * They spin only while the Mixer is running.
 */
public class MixerRotorRenderer implements BlockEntityRenderer<MixerRotorBlockEntity> {

    private static final float DEGREES_PER_TICK = 18.0F;

    private final ModelTurbine model;

    public MixerRotorRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new ModelTurbine(context.getModelSet());
    }

    @Override
    public void render(MixerRotorBlockEntity rotor, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockState state = rotor.getBlockState();
        int blades = state.getValue(MixerRotorBlock.BLADES);
        if (blades == 0 || rotor.getLevel() == null) return;

        advance(rotor, state.getValue(MixerRotorBlock.ACTIVE), partialTick);
        int baseIndex = positionInColumn(rotor.getLevel(), rotor.getBlockPos()) * 2;
        VertexConsumer buffer = model.getBuffer(buffers);

        pose.pushPose();
        pose.translate(0.5, -1.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(rotor.angle));
        model.render(pose, buffer, light, overlay, baseIndex);
        pose.popPose();

        if (blades == 2) {
            pose.pushPose();
            pose.translate(0.5, -0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(rotor.angle));
            model.render(pose, buffer, light, overlay, baseIndex + 1);
            pose.popPose();
        }
    }

    /** Turns the blades by elapsed game time, not by frame, so the speed is the same at any FPS. */
    private static void advance(MixerRotorBlockEntity rotor, boolean active, float partialTick) {
        float now = rotor.getLevel().getGameTime() + partialTick;
        if (active && rotor.lastRenderTime >= 0) {
            rotor.angle = (rotor.angle + (now - rotor.lastRenderTime) * DEGREES_PER_TICK) % 360.0F;
        }
        rotor.lastRenderTime = now;
    }

    /** How many rotors sit below this one, so blades further up the shaft get their own index. */
    private static int positionInColumn(Level level, BlockPos pos) {
        int position = 0;
        BlockPos below = pos.below();
        while (level.getBlockState(below).getBlock() instanceof MixerRotorBlock) {
            position++;
            below = below.below();
        }
        return position;
    }

    // Blades reach well past the shaft's own block, so widen the box or they vanish at the edges.
    @Override
    public AABB getRenderBoundingBox(MixerRotorBlockEntity rotor) {
        return new AABB(rotor.getBlockPos()).inflate(2);
    }

    @Override
    public boolean shouldRenderOffScreen(MixerRotorBlockEntity rotor) {
        return true;
    }
}
