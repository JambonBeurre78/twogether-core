package com.twogether.core;

import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IMekanismHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A valve slotted into a multiblock wall wherever the player wants IO. Shared by the
 * Distillation Tower and the Mixer, so it talks to its controller only through
 * MultiblockController. Pure proxy with no configuration: once a controller has validated its
 * structure it stamps its position here, and from then on the valve takes fluids, items, heat
 * and power in and gives the output fluid back.
 */
public class DistillationTowerValveBlockEntity extends BlockEntity implements IMekanismHeatHandler {

    private static final int OUTPUT_TANK = 1;

    @Nullable
    private BlockPos controllerPos;

    public DistillationTowerValveBlockEntity(BlockPos pos, BlockState state) {
        super(TwoGetherCoreMod.DISTILLATION_VALVE_BE.get(), pos, state);
    }

    @Nullable
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void setControllerPos(@Nullable BlockPos controllerPos) {
        if (Objects.equals(this.controllerPos, controllerPos)) return;
        this.controllerPos = controllerPos;
        setChanged();
        // Neighbours (cables, pipes) cache what this block exposes; make them look again now
        // that there is a machine behind it.
        if (level != null) level.invalidateCapabilities(worldPosition);
    }

    @Nullable
    public MultiblockController getController() {
        if (controllerPos == null || level == null) return null;
        return level.getBlockEntity(controllerPos) instanceof MultiblockController controller ? controller : null;
    }

    private final IFluidHandler fluidCapability = new ValveFluidHandler();
    private final IItemHandler itemCapability = new ValveItemHandler();
    private final IEnergyStorage energyCapability = new ValveEnergyStorage();

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public IItemHandler getItemCapability() {
        return itemCapability;
    }

    public IEnergyStorage getEnergyCapability() {
        return energyCapability;
    }

    // ---- IMekanismHeatHandler: heat piped into any valve lands in the controller's capacitor ----

    @Override
    public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        MultiblockController controller = getController();
        return controller == null ? List.of() : controller.getHeatCapacitors(side);
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    /**
     * Both tanks are exposed, so a pipe pushing an ingredient fills the input and a pipe pulling
     * takes the product from the output - no side or mode to configure.
     */
    private final class ValveFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            MultiblockController controller = getController();
            if (controller == null) return FluidStack.EMPTY;
            return tank == OUTPUT_TANK ? controller.getOutputTank().getFluid() : controller.getInputTank().getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            MultiblockController controller = getController();
            if (controller == null) return 0;
            return tank == OUTPUT_TANK ? controller.getOutputTank().getCapacity() : controller.getInputTank().getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            MultiblockController controller = getController();
            if (controller == null) return false;
            return tank == OUTPUT_TANK ? controller.getOutputTank().isFluidValid(stack) : controller.getInputTank().isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            MultiblockController controller = getController();
            return controller == null ? 0 : controller.getInputTank().fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            MultiblockController controller = getController();
            return controller == null ? FluidStack.EMPTY : controller.getOutputTank().drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            MultiblockController controller = getController();
            return controller == null ? FluidStack.EMPTY : controller.getOutputTank().drain(maxDrain, action);
        }
    }

    /** Insert-only window onto the controller's input slots. */
    private final class ValveItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            MultiblockController controller = getController();
            return controller == null ? 0 : controller.getInputItems().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            MultiblockController controller = getController();
            return controller == null ? ItemStack.EMPTY : controller.getInputItems().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            MultiblockController controller = getController();
            return controller == null ? stack : controller.getInputItems().insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            MultiblockController controller = getController();
            return controller == null ? 0 : controller.getInputItems().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            MultiblockController controller = getController();
            return controller != null && controller.getInputItems().isItemValid(slot, stack);
        }
    }

    /** Receive-only window onto the controller's power buffer; inert on machines without one. */
    private final class ValveEnergyStorage implements IEnergyStorage {
        @Nullable
        private IEnergyStorage target() {
            MultiblockController controller = getController();
            return controller == null ? null : controller.getEnergyStorage();
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            IEnergyStorage target = target();
            return target == null ? 0 : target.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            IEnergyStorage target = target();
            return target == null ? 0 : target.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            IEnergyStorage target = target();
            return target == null ? 0 : target.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            IEnergyStorage target = target();
            return target != null && target.canReceive();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) tag.putLong("controller", controllerPos.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        controllerPos = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
    }
}
