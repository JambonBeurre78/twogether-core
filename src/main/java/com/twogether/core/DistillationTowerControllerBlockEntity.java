package com.twogether.core;

import mekanism.api.heat.HeatAPI;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IMekanismHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * "Distillation Tower": custom multiblock, not hooked into Mekanism's own
 * multiblock system (that one is entirely internal to mekanism.common, no
 * public API - confirmed by scanning their source + web search before
 * building this). Shape is fixed (see DistillationTowerShape): solid floor,
 * 1-8 hollow octagon-ring body layers, then a solid 2-step taper cap.
 * Bigger body -> more tank capacity and faster throughput, same idea as the
 * single-block Fermenter prototype but as an actual multiblock the player
 * builds and configures IO on freely via the casing blocks.
 */
public class DistillationTowerControllerBlockEntity extends BlockEntity implements IMekanismHeatHandler, MenuProvider, MultiblockController {

    // Bigger than a bare-minimum value on purpose: a real Mekanism heat network (Thermodynamic
    // Conductors) pushes heat calibrated for their own much larger machines, so a small capacity
    // here would spike the displayed temperature absurdly high in seconds (see SimpleHeatCapacitor's
    // clamp for the other half of this fix).
    private static final double HEAT_CAPACITY = 2000.0;
    public static final double FERMENT_MIN_TEMP = HeatAPI.AMBIENT_TEMP + 50.0;
    // Copper (baseline) vs steel (efficient) tier, picked from which controller block was placed.
    private static final double HEAT_CONSUMED_PER_TICK_COPPER = 0.02;
    private static final double HEAT_CONSUMED_PER_TICK_STEEL = 0.014;
    private static final double PASSIVE_COOLING_PER_TICK_COPPER = 0.01;
    private static final double PASSIVE_COOLING_PER_TICK_STEEL = 0.006;
    private static final int FERMENT_TIME_TICKS_COPPER = 200;
    private static final int FERMENT_TIME_TICKS_STEEL = 140;
    private static final double STEEL_TIME_FACTOR = (double) FERMENT_TIME_TICKS_STEEL / FERMENT_TIME_TICKS_COPPER;
    private static final int BASE_TANK_MB = 1000;
    private static final int RESCAN_INTERVAL_TICKS = 40;

    // Why the tower is not producing, so the GUI can say it instead of leaving the player guessing.
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_NOT_FORMED = 1;
    public static final int STATUS_NO_RECIPE = 2;
    public static final int STATUS_NOT_ENOUGH_INPUT = 3;
    public static final int STATUS_TOO_COLD = 4;
    public static final int STATUS_OUTPUT_FULL = 5;

    private final SimpleHeatCapacitor heatCapacitor = new SimpleHeatCapacitor(HEAT_CAPACITY, 1.0, 5.0, this::setChanged);
    private final ItemStackHandler inputSlot = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return FluidUtil.getFluidHandler(stack).isPresent();
        }
    };
    /** Accepts whatever the loaded fermenting recipes take, so adding a recipe is enough to support a new chain. */
    private final FluidTank inputTank = new FluidTank(BASE_TANK_MB, this::isRecipeInput) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    /** Only ever filled by this block entity, so it takes whatever the current recipe produces. */
    private final FluidTank outputTank = new FluidTank(BASE_TANK_MB, fs -> true) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IFluidHandler fluidCapability = new CombinedFluidHandler();

    private boolean formed;
    private int bodyLayers;
    private int throughputMultiplier = 1;
    private int ticksSinceRescan;
    private int progress;
    private Set<BlockPos> shellPositions = new HashSet<>();

    private int activeStatus = STATUS_NOT_FORMED;

    /** Time and yield of the recipe currently loaded in the input tank, for the GUI readout. */
    private int activeTimeTicks = FERMENT_TIME_TICKS_COPPER;
    private int activeOutputPerCraft;

    @Nullable
    private Set<Fluid> acceptedInputs;

    /**
     * Values shown by the screen, in the index order the screen reads them. Wide so tank
     * capacities above 32767 mB are not wrapped by vanilla's short-based sync.
     */
    private final WideContainerData containerData = new WideContainerData(
            () -> level != null && level.isClientSide,
            () -> formed ? 1 : 0,
            () -> bodyLayers,
            () -> throughputMultiplier,
            () -> progress,
            () -> activeTimeTicks,
            () -> (int) Math.round(heatCapacitor.getTemperature()),
            inputTank::getFluidAmount,
            inputTank::getCapacity,
            outputTank::getFluidAmount,
            outputTank::getCapacity,
            // Environment loss in K/t, scaled by 1000 so the tiny per-tick value survives int sync.
            () -> (int) Math.round(passiveCoolingPerTick() / HEAT_CAPACITY * 1000.0),
            () -> activeOutputPerCraft,
            () -> BuiltInRegistries.FLUID.getId(inputTank.getFluid().getFluid()),
            () -> BuiltInRegistries.FLUID.getId(outputTank.getFluid().getFluid()),
            () -> activeStatus);

    public WideContainerData getContainerData() {
        return containerData;
    }

    public DistillationTowerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(TwoGetherCoreMod.DISTILLATION_CONTROLLER_BE.get(), pos, state);
    }

    private static boolean isOpen(BlockState state) {
        if (state.isAir()) return true;
        FluidState fluidState = state.getFluidState();
        return fluidState.isEmpty() && state.canBeReplaced();
    }

    private boolean isSteelTier() {
        return getBlockState().is(TwoGetherCoreMod.DISTILLATION_CONTROLLER_STEEL.get());
    }

    private double heatConsumedPerTick() {
        return isSteelTier() ? HEAT_CONSUMED_PER_TICK_STEEL : HEAT_CONSUMED_PER_TICK_COPPER;
    }

    private double passiveCoolingPerTick() {
        return isSteelTier() ? PASSIVE_COOLING_PER_TICK_STEEL : PASSIVE_COOLING_PER_TICK_COPPER;
    }

    private int fermentTimeTicks() {
        return isSteelTier() ? FERMENT_TIME_TICKS_STEEL : FERMENT_TIME_TICKS_COPPER;
    }

    public int getFermentTimeTicksForDisplay() {
        return fermentTimeTicks();
    }

    public boolean isFormed() {
        return formed;
    }

    public int getBodyLayers() {
        return bodyLayers;
    }

    public int getThroughputMultiplier() {
        return throughputMultiplier;
    }

    public int getProgress() {
        return progress;
    }

    public double getTemperature() {
        return heatCapacitor.getTemperature();
    }

    public ItemStackHandler getInputSlot() {
        return inputSlot;
    }

    @Override
    public IItemHandler getInputItems() {
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

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DistillationTowerControllerMenu(containerId, inventory, this);
    }

    public void serverTick() {
        if (level == null) return;
        ticksSinceRescan++;
        if (!formed || ticksSinceRescan >= RESCAN_INTERVAL_TICKS) {
            ticksSinceRescan = 0;
            tryFormStructure();
            refreshAcceptedInputs();
        }

        // Emptying a bucket into the tank works whether or not the structure is complete: the
        // player can load the tower first and finish building it after.
        drainContainerInSlot();

        if (!formed) {
            activeStatus = STATUS_NOT_FORMED;
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        heatCapacitor.coolTowardAmbient(passiveCoolingPerTick());

        FermentingRecipe recipe = findRecipe();
        if (recipe == null) {
            activeStatus = STATUS_NO_RECIPE;
            activeTimeTicks = isSteelTier() ? FERMENT_TIME_TICKS_STEEL : FERMENT_TIME_TICKS_COPPER;
            activeOutputPerCraft = 0;
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }

        // The tower's size makes it faster, not hungrier: the batch stays the recipe's own amount
        // so a big tower still starts on one bucket, while the throughput per tick is unchanged.
        activeTimeTicks = Math.max(1, recipeTimeTicks(recipe) / throughputMultiplier);
        int neededInput = recipe.input().amount();
        FluidStack result = recipe.result();
        int producedOutput = result.getAmount();
        activeOutputPerCraft = producedOutput;

        boolean hot = heatCapacitor.getTemperature() >= recipe.minTemperature();
        boolean enoughInput = neededInput > 0 && inputTank.getFluidAmount() >= neededInput;
        FluidStack produced = new FluidStack(result.getFluid(), producedOutput);
        boolean hasRoom = outputTank.fill(produced, IFluidHandler.FluidAction.SIMULATE) == producedOutput;

        activeStatus = !enoughInput ? STATUS_NOT_ENOUGH_INPUT
                : !hot ? STATUS_TOO_COLD
                : !hasRoom ? STATUS_OUTPUT_FULL
                : STATUS_RUNNING;

        if (hot && enoughInput && hasRoom) {
            heatCapacitor.handleHeat(-heatConsumedPerTick() * throughputMultiplier * heatCapacitor.getHeatCapacity());
            progress++;
            if (progress >= activeTimeTicks) {
                progress = 0;
                inputTank.drain(neededInput, IFluidHandler.FluidAction.EXECUTE);
                outputTank.fill(produced, IFluidHandler.FluidAction.EXECUTE);
            }
            setChanged();
        } else if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    /**
     * True if some evaporating recipe takes this fluid. The answer is cached and refreshed on the
     * structure rescan, because the tank asks this on every pipe insertion attempt, and it must
     * also survive a datapack reload adding recipes.
     */
    private boolean isRecipeInput(FluidStack stack) {
        if (level == null || stack.isEmpty()) return false;
        if (acceptedInputs == null) refreshAcceptedInputs();
        return acceptedInputs.contains(stack.getFluid());
    }

    private void refreshAcceptedInputs() {
        if (level == null) return;
        Set<Fluid> accepted = new HashSet<>();
        for (RecipeHolder<FermentingRecipe> holder : level.getRecipeManager().getAllRecipesFor(TwoGetherCoreMod.FERMENTING_TYPE.get())) {
            for (FluidStack fluid : holder.value().input().getFluids()) {
                accepted.add(fluid.getFluid());
            }
        }
        acceptedInputs = accepted;
    }

    @Nullable
    private FermentingRecipe findRecipe() {
        if (level == null || inputTank.isEmpty()) return null;
        return level.getRecipeManager()
                .getRecipeFor(TwoGetherCoreMod.FERMENTING_TYPE.get(), new FermentingRecipe.Input(inputTank.getFluid()), level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    /** Steel runs the same recipe faster than copper. */
    private int recipeTimeTicks(FermentingRecipe recipe) {
        double factor = isSteelTier() ? STEEL_TIME_FACTOR : 1.0;
        return Math.max(1, (int) Math.round(recipe.time() * factor));
    }

    /** The machine slot doubles as a bucket slot: a filled container there is emptied into the input tank. */
    private void drainContainerInSlot() {
        ItemStack stack = inputSlot.getStackInSlot(0);
        if (stack.isEmpty()) return;
        FluidUtil.getFluidHandler(stack).ifPresent(handler -> {
            FluidStack drained = handler.drain(inputTank.getSpace(), IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty() || !inputTank.isFluidValid(drained)) return;
            FluidActionResult result = FluidUtil.tryEmptyContainer(stack, inputTank, drained.getAmount(), null, true);
            if (result.isSuccess()) {
                inputSlot.setStackInSlot(0, result.getResult());
                setChanged();
            }
        });
    }

    /**
     * The controller sits somewhere on the ring wall of one body layer. Try
     * every wall offset as a hypothesis for where the tower's central axis
     * is, then verify floor/body-stack/cap around that axis.
     */
    public boolean tryFormStructure() {
        if (level == null) return false;
        for (DistillationTowerShape.Offset hypothesis : DistillationTowerShape.CONTROLLER_CELLS) {
            int cx = worldPosition.getX() - hypothesis.dx();
            int cz = worldPosition.getZ() - hypothesis.dz();
            if (attemptFormAt(cx, cz)) return true;
        }
        setFormed(false);
        return false;
    }

    private void setFormed(boolean newFormed) {
        if (formed == newFormed) return;
        formed = newFormed;
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(DistillationTowerControllerBlock.FORMED)) {
                level.setBlock(worldPosition, state.setValue(DistillationTowerControllerBlock.FORMED, newFormed), 3);
            }
        }
    }

    private boolean attemptFormAt(int cx, int cz) {
        int controllerY = worldPosition.getY();
        if (!layerMatchesRing(cx, cz, controllerY, true)) return false;

        int bottom = controllerY;
        while (layerMatchesRing(cx, cz, bottom - 1, false)) bottom--;
        int top = controllerY;
        while (layerMatchesRing(cx, cz, top + 1, false)) top++;

        int layers = top - bottom + 1;
        if (layers < DistillationTowerShape.MIN_BODY_LAYERS || layers > DistillationTowerShape.MAX_BODY_LAYERS) {
            return false;
        }
        if (!layerMatchesSolid(cx, cz, bottom - 1, DistillationTowerShape.FULL_R3)) return false;
        if (!layerMatchesSolid(cx, cz, top + 1, DistillationTowerShape.FULL_R2)) return false;
        if (!layerMatchesSolid(cx, cz, top + 2, DistillationTowerShape.FULL_R1)) return false;

        Set<BlockPos> shell = new HashSet<>();
        for (int y = bottom; y <= top; y++) {
            for (DistillationTowerShape.Offset o : DistillationTowerShape.WALL_R3) {
                shell.add(new BlockPos(cx + o.dx(), y, cz + o.dz()));
            }
        }

        setFormed(true);
        bodyLayers = layers;
        throughputMultiplier = Math.max(1, layers);
        shellPositions = shell;
        int newCapacity = BASE_TANK_MB * Math.max(1, layers * DistillationTowerShape.INTERIOR_CELLS_PER_LAYER);
        inputTank.setCapacity(newCapacity);
        outputTank.setCapacity(newCapacity);
        for (BlockPos pos : shell) {
            if (pos.equals(worldPosition)) continue;
            if (level.getBlockEntity(pos) instanceof DistillationTowerValveBlockEntity valve) {
                valve.setControllerPos(worldPosition);
            }
        }
        setChanged();
        return true;
    }

    /** Plain casing (either tier) or a valve - both count as structural wall material. */
    private boolean isStructuralBlock(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(TwoGetherCoreMod.DISTILLATION_CASING_COPPER.get()) || state.is(TwoGetherCoreMod.DISTILLATION_CASING_STEEL.get())) {
            return true;
        }
        return level.getBlockEntity(pos) instanceof DistillationTowerValveBlockEntity;
    }

    /** A ring layer: WALL_R3 cells must be casing/valve (or the controller itself), interior cells must be open. */
    private boolean layerMatchesRing(int cx, int cz, int y, boolean allowControllerHere) {
        for (DistillationTowerShape.Offset o : DistillationTowerShape.FULL_R3) {
            BlockPos pos = new BlockPos(cx + o.dx(), y, cz + o.dz());
            boolean isWall = DistillationTowerShape.WALL_R3.contains(o);
            if (isWall) {
                if (allowControllerHere && pos.equals(worldPosition)) continue;
                if (!isStructuralBlock(pos)) return false;
            } else {
                if (!isOpen(level.getBlockState(pos))) return false;
            }
        }
        return true;
    }

    /** A fully solid layer (floor / cap tiers): every cell in shape must be casing/valve. */
    private boolean layerMatchesSolid(int cx, int cz, int y, Set<DistillationTowerShape.Offset> shape) {
        for (DistillationTowerShape.Offset o : shape) {
            BlockPos pos = new BlockPos(cx + o.dx(), y, cz + o.dz());
            if (!isStructuralBlock(pos)) return false;
        }
        return true;
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

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", inputSlot.serializeNBT(registries));
        tag.put("inputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("tank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("heat", heatCapacitor.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("formed", formed);
        tag.putInt("bodyLayers", bodyLayers);
        tag.putInt("throughputMultiplier", throughputMultiplier);
        long[] positions = shellPositions.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("shell", positions);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("input")) inputSlot.deserializeNBT(registries, tag.getCompound("input"));
        if (tag.contains("inputTank")) inputTank.readFromNBT(registries, tag.getCompound("inputTank"));
        if (tag.contains("tank")) outputTank.readFromNBT(registries, tag.getCompound("tank"));
        if (tag.contains("heat")) heatCapacitor.deserializeNBT(registries, tag.getCompound("heat"));
        progress = tag.getInt("progress");
        formed = tag.getBoolean("formed");
        bodyLayers = tag.getInt("bodyLayers");
        throughputMultiplier = Math.max(1, tag.getInt("throughputMultiplier"));
        shellPositions = new HashSet<>();
        for (long encoded : tag.getLongArray("shell")) {
            shellPositions.add(BlockPos.of(encoded));
        }
    }
}
