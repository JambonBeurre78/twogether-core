package com.twogether.core;

/**
 * Plain wall block of the Distillation Tower - purely structural, no state,
 * no block entity. IO lives on DistillationTowerValveBlock instead (matches
 * Mekanism's own Thermal Evaporation Plant split: plain casing vs valve).
 */
public class DistillationTowerCasingBlock extends net.minecraft.world.level.block.Block {
    public DistillationTowerCasingBlock(Properties properties) {
        super(properties);
    }
}
