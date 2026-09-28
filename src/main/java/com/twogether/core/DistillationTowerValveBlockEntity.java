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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A valve slotted into the Distillation Tower wall wherever the player wants
 * IO (matches Mekanism's own valve concept on the Thermal Evaporation Plant).
 * Pure proxy with no configuration: once the controller has validated the
 * structure it stamps its own position into every valve, and from then on any
 * valve accepts pitched_wort or fermentable items in, gives beer out, and
 * carries Mekanism heat through to the controller's capacitor - so thermal
 * conductors connect here rather than to the controller block.
 */
public class DistillationTowerValveBlockEntity extends BlockEntity implements IMekanismHeatHandler {

    private static final int INPUT_TANK = 0;
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
        this.controllerPos = controllerPos;
    }

    @Nullable
    public DistillationTowerControllerBlockEntity getController() {
        if (controllerPos == null || level == null) return null;
        if (level.getBlockEntity(controllerPos) instanceof DistillationTowerControllerBlockEntity controller) {
            return controller;
        }
        return null;
    }

    private final IFluidHandler fluidCapability = new ValveFluidHandler();
    private final IItemHandler itemCapability = new ValveItemHandler();

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public IItemHandler getItemCapability() {
        return itemCapability;
    }

    // ---- IMekanismHeatHandler: heat piped into any valve lands in the controller's capacitor ----

    @Override
    public List<IHeatCapacitor> getHeatCapacitors(Direction side) {
        DistillationTowerControllerBlockEntity controller = getController();
        return controller == null ? List.of() : controller.getHeatCapacitors(side);
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    /**
     * Both tanks are exposed, so a pipe pushing pitched_wort fills the input and a pipe
     * pulling takes beer from the output - no side or mode to configure.
     */
    private final class ValveFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return FluidStack.EMPTY;
            return tank == OUTPUT_TANK ? controller.getOutputTank().getFluid() : controller.getInputTank().getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return 0;
            return tank == OUTPUT_TANK ? controller.getOutputTank().getCapacity() : controller.getInputTank().getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return false;
            return tank == OUTPUT_TANK ? controller.getOutputTank().isFluidValid(stack) : controller.getInputTank().isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return 0;
            return controller.getInputTank().fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return FluidStack.EMPTY;
            return controller.getOutputTank().drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return FluidStack.EMPTY;
            return controller.getOutputTank().drain(maxDrain, action);
        }
    }

    private final class ValveItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return getController() != null ? 1 : 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            DistillationTowerControllerBlockEntity controller = getController();
            return controller == null ? ItemStack.EMPTY : controller.getInputSlot().getStackInSlot(0);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return stack;
            return controller.getInputSlot().insertItem(0, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            DistillationTowerControllerBlockEntity controller = getController();
            if (controller == null) return false;
            return controller.getInputSlot().isItemValid(0, stack);
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
