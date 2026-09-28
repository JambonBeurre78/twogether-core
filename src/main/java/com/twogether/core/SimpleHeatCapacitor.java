package com.twogether.core;

import mekanism.api.IContentsListener;
import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * Minimal, hand-rolled IHeatCapacitor - Mekanism's api.heat package has no
 * public builder/basic implementation to reuse (BasicHeatCapacitor lives in
 * their internal mekanism.common package), so this is a small self-contained
 * one just big enough for a single-block heat-consuming machine, not a full
 * heat-network simulation like their boilers/reactors use.
 */
public class SimpleHeatCapacitor implements IHeatCapacitor {

    private final double heatCapacity;
    private final double inverseConduction;
    private final double inverseInsulation;
    private final IContentsListener listener;

    private double heat;

    public SimpleHeatCapacitor(double heatCapacity, double inverseConduction, double inverseInsulation, IContentsListener listener) {
        this.heatCapacity = heatCapacity;
        this.inverseConduction = inverseConduction;
        this.inverseInsulation = inverseInsulation;
        this.listener = listener;
        this.heat = HeatAPI.AMBIENT_TEMP * heatCapacity;
    }

    @Override
    public double getTemperature() {
        return heat / heatCapacity;
    }

    @Override
    public double getInverseConduction() {
        return inverseConduction;
    }

    @Override
    public double getInverseInsulation() {
        return inverseInsulation;
    }

    @Override
    public double getHeatCapacity() {
        return heatCapacity;
    }

    @Override
    public double getHeat() {
        return heat;
    }

    // A real Mekanism heat network transfers heat calibrated for their own (much larger) machine
    // heat capacitors; without a ceiling here, a connected Thermodynamic Conductor pushes far more
    // heat per tick than this small single/multi-block prototype can sanely represent, and since
    // temperature = heat / heatCapacity, that runs the displayed temperature into the hundreds of
    // thousands of Kelvin within seconds. Cap it at a generous but sane value instead.
    private static final double MAX_TEMPERATURE = 5000.0;

    @Override
    public void setHeat(double heat) {
        this.heat = clamp(heat);
        listener.onContentsChanged();
    }

    @Override
    public void handleHeat(double transfer) {
        this.heat = clamp(this.heat + transfer);
        listener.onContentsChanged();
    }

    private double clamp(double newHeat) {
        double maxHeat = MAX_TEMPERATURE * heatCapacity;
        double minHeat = 0;
        return Math.max(minHeat, Math.min(maxHeat, newHeat));
    }

    @Override
    public void onContentsChanged() {
        listener.onContentsChanged();
    }

    public void coolTowardAmbient(double rate) {
        double ambientHeat = HeatAPI.AMBIENT_TEMP * heatCapacity;
        if (heat > ambientHeat) {
            heat = Math.max(ambientHeat, heat - rate);
        } else if (heat < ambientHeat) {
            heat = Math.min(ambientHeat, heat + rate);
        }
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("heat", heat);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains("heat")) {
            // Clamp on load too, so a save file with a pre-existing runaway value (from before
            // this cap existed) gets sanitized automatically instead of staying stuck sky-high.
            heat = clamp(tag.getDouble("heat"));
        }
    }
}
