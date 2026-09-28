package com.twogether.core;

import mekanism.api.fluid.IExtendedFluidTank;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Client-side view of a tank whose contents arrive through container data, so Mekanism's
 * GuiFluidGauge can draw it. Display only: the mutators are no-ops.
 */
final class ClientFluidTankView implements IExtendedFluidTank {

    private final Supplier<Fluid> fluid;
    private final IntSupplier amountSupplier;
    private final IntSupplier capacitySupplier;

    ClientFluidTankView(Supplier<Fluid> fluid, IntSupplier amountSupplier, IntSupplier capacitySupplier) {
        this.fluid = fluid;
        this.amountSupplier = amountSupplier;
        this.capacitySupplier = capacitySupplier;
    }

    /** Fluids are synced by registry id; this turns one back into the fluid. */
    static Fluid fluidById(int id) {
        return BuiltInRegistries.FLUID.byId(id);
    }

    @Override
    public FluidStack getFluid() {
        int amount = amountSupplier.getAsInt();
        Fluid contents = fluid.get();
        return amount <= 0 || contents == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(contents, amount);
    }

    @Override
    public int getCapacity() {
        return Math.max(1, capacitySupplier.getAsInt());
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return stack.getFluid() == fluid.get();
    }

    @Override
    public void setStack(FluidStack stack) {
    }

    @Override
    public void setStackUnchecked(FluidStack stack) {
    }

    @Override
    public void onContentsChanged() {
    }
}
