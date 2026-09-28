package com.twogether.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Exists only so the rotor can have a renderer. Holds the blade angle, which is purely visual
 * and client-side, so nothing is saved.
 */
public class MixerRotorBlockEntity extends BlockEntity {

    /** Current blade angle in degrees, advanced by the renderer while the rotor is active. */
    float angle;
    /** Game time of the last frame drawn, so the spin speed does not depend on the frame rate. */
    float lastRenderTime = -1;

    public MixerRotorBlockEntity(BlockPos pos, BlockState state) {
        super(TwoGetherCoreMod.MIXER_ROTOR_BE.get(), pos, state);
    }
}
