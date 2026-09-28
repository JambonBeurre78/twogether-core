package com.twogether.core;

/**
 * Geometry of the Mixer: the same radius-3 octagon as the Distillation Tower, so the two read
 * as one production line, but squat - a solid floor, one or two body rings, and a solid lid.
 * That makes it 3 or 4 blocks tall. The centre column carries the rotors, topped by the drive
 * block set into the lid.
 */
final class MixerShape {

    static final int MIN_BODY_LAYERS = 1;
    static final int MAX_BODY_LAYERS = 2;

    /** Body cells minus the centre, which the rotor column takes. */
    static final int FLUID_CELLS_PER_LAYER = DistillationTowerShape.INTERIOR_CELLS_PER_LAYER - 1;

    /** Two blades per rotor, as on Mekanism's turbine rotors. */
    static final int MAX_BLADES_PER_ROTOR = 2;

    private MixerShape() {
    }
}
