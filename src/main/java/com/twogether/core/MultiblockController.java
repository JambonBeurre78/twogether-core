package com.twogether.core;

import mekanism.api.heat.IHeatCapacitor;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * The machine's state for a ComputerCraft computer, as plain values (strings, numbers,
     * booleans, nested maps) so the compat layer passes it to Lua untouched.
     */
    Map<String, Object> getComputerState();

    /** A tank as {fluid, amount, capacity}; fluid is nil when the tank is empty. */
    static Map<String, Object> describeTank(FluidTank tank) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!tank.getFluid().isEmpty()) {
            out.put("fluid", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).toString());
        }
        out.put("amount", tank.getFluidAmount());
        out.put("capacity", tank.getCapacity());
        return out;
    }

    /** Null when the machine runs on heat alone. */
    @Nullable
    default IEnergyStorage getEnergyStorage() {
        return null;
    }
}
