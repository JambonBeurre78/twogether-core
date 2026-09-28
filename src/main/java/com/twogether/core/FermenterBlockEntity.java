package com.twogether.core;

import mekanism.api.heat.HeatAPI;
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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.Set;

/**
 * Single-block prototype: heats via Mekanism heat (any Mekanism heat source
 * touching it, e.g. a Fuelwood Heater, can push heat in), consumes heat while
 * running, and turns either the existing Brewery beer items OR the new
 * pitched_wort fluid (end of the houblonnage -> empatage -> ensemencement
 * chain) into twogethercore:beer fluid over time. Multiblock shell is the
 * next step once this works.
 */
public class FermenterBlockEntity extends BlockEntity implements IMekanismHeatHandler {

    private static final Set<String> FERMENTABLE_ITEMS = Set.of(
            "brewery:beer_barley", "brewery:beer_haley", "brewery:beer_hops",
            "brewery:beer_nettle", "brewery:beer_oat", "brewery:beer_wheat");

    // See SimpleHeatCapacitor's clamp + DistillationTowerControllerBlockEntity for why this isn't
    // a tiny value: a real Mekanism heat network pushes heat calibrated for their own much larger
    // machines, which would spike a small capacitor's temperature absurdly high in seconds.
    private static final double HEAT_CAPACITY = 2000.0;
    private static final double FERMENT_MIN_TEMP = HeatAPI.AMBIENT_TEMP + 50.0;
    private static final double HEAT_CONSUMED_PER_TICK = 0.02;
    private static final double PASSIVE_COOLING_PER_TICK = 0.01;
    private static final int FERMENT_TIME_TICKS = 200; // 10s per item at 20 tps
    private static final int FERMENT_OUTPUT_MB = 250;
    private static final int FERMENT_INPUT_MB = 250; // pitched_wort consomme par cycle
    private static final int TANK_CAPACITY_MB = 4000;

    private final SimpleHeatCapacitor heatCapacitor = new SimpleHeatCapacitor(HEAT_CAPACITY, 1.0, 5.0, this::setChanged);
    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFermentable(stack);
        }
    };
    private final FluidTank inputTank = new FluidTank(TANK_CAPACITY_MB, fluidStack -> fluidStack.getFluid() == TwoGetherCoreMod.PITCHED_WORT_STILL.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY_MB, fluidStack -> fluidStack.getFluid() == TwoGetherCoreMod.BEER_STILL.get()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidCapability = new CombinedFluidHandler();

    private int progress;

    public FermenterBlockEntity(BlockPos pos, BlockState state) {
        super(TwoGetherCoreMod.FERMENTER_BE.get(), pos, state);
    }

    private static boolean isFermentable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return FERMENTABLE_ITEMS.contains(stack.getItem().builtInRegistryHolder().key().location().toString());
    }

    public ItemStackHandler getInputSlot() {
        return inputSlot;
    }

    public FluidTank getInputTank() {
        return inputTank;
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    public double getTemperature() {
        return heatCapacitor.getTemperature();
    }

    public int getProgress() {
        return progress;
    }

    public void serverTick() {
        heatCapacitor.coolTowardAmbient(PASSIVE_COOLING_PER_TICK);

        ItemStack input = inputSlot.getStackInSlot(0);
        boolean hot = heatCapacitor.getTemperature() >= FERMENT_MIN_TEMP;
        boolean hasRoom = outputTank.getFluidAmount() + FERMENT_OUTPUT_MB <= outputTank.getCapacity();
        // La chaine houblonnage -> empatage -> ensemencement remplit inputTank ;
        // on garde aussi l'ancien chemin par item (recettes Create existantes) en repli.
        boolean useFluid = inputTank.getFluidAmount() >= FERMENT_INPUT_MB;
        boolean useItem = !useFluid && isFermentable(input);
        boolean validInput = useFluid || useItem;

        if (hot && validInput && hasRoom) {
            heatCapacitor.handleHeat(-HEAT_CONSUMED_PER_TICK * heatCapacitor.getHeatCapacity());
            progress++;
            if (progress >= FERMENT_TIME_TICKS) {
                progress = 0;
                if (useFluid) {
                    inputTank.drain(FERMENT_INPUT_MB, IFluidHandler.FluidAction.EXECUTE);
                } else {
                    input.shrink(1);
                }
                outputTank.fill(new FluidStack(TwoGetherCoreMod.BEER_STILL.get(), FERMENT_OUTPUT_MB), IFluidHandler.FluidAction.EXECUTE);
            }
            setChanged();
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    /**
     * Une seule capability FluidHandler exposee sur le bloc : remplissage cote
     * inputTank (pitched_wort), soutirage cote outputTank (beer). Evite de
     * choisir un seul FluidTank pour registerBlockEntity alors qu'il en faut deux.
     */
    private final class CombinedFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 2;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? inputTank.getFluid() : outputTank.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? inputTank.getCapacity() : outputTank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 ? inputTank.isFluidValid(stack) : outputTank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return inputTank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return outputTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return outputTank.drain(maxDrain, action);
        }
    }

    // ---- IMekanismHeatHandler ----

    @Override
    public List<IHeatCapacitor> getHeatCapacitors(Direction side) {
        return List.of(heatCapacitor);
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", inputSlot.serializeNBT(registries));
        tag.put("inputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("tank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("heat", heatCapacitor.serializeNBT(registries));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("input")) inputSlot.deserializeNBT(registries, tag.getCompound("input"));
        if (tag.contains("inputTank")) inputTank.readFromNBT(registries, tag.getCompound("inputTank"));
        if (tag.contains("tank")) outputTank.readFromNBT(registries, tag.getCompound("tank"));
        if (tag.contains("heat")) heatCapacitor.deserializeNBT(registries, tag.getCompound("heat"));
        progress = tag.getInt("progress");
    }
}
