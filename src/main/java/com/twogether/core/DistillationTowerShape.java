package com.twogether.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Fixed footprint of the Distillation Tower: an octagon-ish ring (radius 3,
 * 7x7 bounding box) repeated 1-8 times for the body, sitting on a solid
 * radius-3 floor and closed off by a radius-2 then radius-1 solid cap.
 * Designed from the player's own in-world prototype (screenshot), not
 * hooked into Mekanism's internal multiblock system - see FermenterBlockEntity
 * javadoc for why (no public API for that).
 */
final class DistillationTowerShape {

    record Offset(int dx, int dz) {
    }

    static final Set<Offset> FULL_R3 = octagon(3);
    static final Set<Offset> WALL_R3 = wallOf(FULL_R3);
    static final Set<Offset> FULL_R2 = octagon(2);
    static final Set<Offset> FULL_R1 = square(1);

    /** The 4 cardinal "center of face" wall cells - only place the controller can go, any valve can go anywhere else on WALL_R3. */
    static final Set<Offset> CONTROLLER_CELLS = controllerCellsOf(WALL_R3);

    static final int MIN_BODY_LAYERS = 1;
    static final int MAX_BODY_LAYERS = 8;
    static final int INTERIOR_CELLS_PER_LAYER = FULL_R3.size() - WALL_R3.size();

    private DistillationTowerShape() {
    }

    private static Set<Offset> octagon(int radius) {
        Set<Offset> set = new HashSet<>();
        for (int dz = -radius; dz <= radius; dz++) {
            int cut = Math.max(0, Math.abs(dz) - 1);
            int half = radius - cut;
            for (int dx = -half; dx <= half; dx++) {
                set.add(new Offset(dx, dz));
            }
        }
        return set;
    }

    private static Set<Offset> square(int radius) {
        Set<Offset> set = new HashSet<>();
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                set.add(new Offset(dx, dz));
            }
        }
        return set;
    }

    private static Set<Offset> controllerCellsOf(Set<Offset> wall) {
        Set<Offset> centers = new HashSet<>();
        for (Offset o : wall) {
            if (o.dx() == 0 || o.dz() == 0) centers.add(o);
        }
        return centers;
    }

    private static Set<Offset> wallOf(Set<Offset> full) {
        Set<Offset> wall = new HashSet<>();
        for (Offset o : full) {
            boolean surrounded = full.contains(new Offset(o.dx() - 1, o.dz()))
                    && full.contains(new Offset(o.dx() + 1, o.dz()))
                    && full.contains(new Offset(o.dx(), o.dz() - 1))
                    && full.contains(new Offset(o.dx(), o.dz() + 1));
            if (!surrounded) wall.add(o);
        }
        return wall;
    }
}
