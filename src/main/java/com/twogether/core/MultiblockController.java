package com.twogether.core;

import mekanism.api.heat.IHeatCapacitor;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * What a valve needs from the controller it is attached to. The steel valves are shared by the
 * Distillation Tower and the Mixer, so they talk to either through this rather than to a
 * concrete controller class.
 */
public interface MultiblockController {

    FluidTank getInputTank();

    FluidTank getOutputTank();

    /** Items pushed in through a valve (hoppers, pipes). */
    IItemHandler getInputItems();

    /** Items a valve can hand out; none on machines that only produce fluid. */
    default IItemHandler getOutputItems() {
        return NO_ITEMS;
    }

    IItemHandler NO_ITEMS = new net.neoforged.neoforge.items.ItemStackHandler(0);

    List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side);

    /** Null when the machine runs on heat alone. */
    @Nullable
    default IEnergyStorage getEnergyStorage() {
        return null;
    }
}
