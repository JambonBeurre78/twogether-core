package com.twogether.core;

import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.heat.IMekanismHeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The Mixer: a squat brass vat on the Distillation Tower's octagon, with a bladed rotor column
 * in the middle. It runs on power and, for heated recipes, on heat - both fed through the steel
 * valves. More blades mix faster and draw proportionally more power, like Mekanism's turbine.
 */
public class MixerControllerBlockEntity extends BlockEntity implements IMekanismHeatHandler, MenuProvider, MultiblockController {

    public static final int INPUT_SLOTS = 4;

    private static final double HEAT_CAPACITY = 2000.0;
    /** Kelvin lost per tick towards ambient, turned into heat units for the capacitor. */
    private static final double PASSIVE_COOLING_K_PER_TICK = 0.05;
    /** Kelvin spent per tick while a heated recipe runs. */
    private static final double HEATED_RUN_COST_K_PER_TICK = 0.02;
    private static final int ENERGY_CAPACITY = 200_000;
    private static final int ENERGY_MAX_RECEIVE = 4_000;
    private static final int BASE_TANK_MB = 1000;
    private static final int RESCAN_INTERVAL_TICKS = 40;

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_NOT_FORMED = 1;
    public static final int STATUS_NO_RECIPE = 2;
    public static final int STATUS_NO_BLADES = 3;
    public static final int STATUS_NO_POWER = 4;
    public static final int STATUS_TOO_COLD = 5;
    public static final int STATUS_OUTPUT_FULL = 6;

    private final SimpleHeatCapacitor heatCapacitor = new SimpleHeatCapacitor(HEAT_CAPACITY, 1.0, 5.0, this::setChanged);
    private final Buffer energy = new Buffer();

    private final ItemStackHandler inputItems = new ItemStackHandler(INPUT_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    /** Accepts whatever a mixing recipe can take as its fluid, so adding a recipe is enough. */
    private final FluidTank inputTank = new FluidTank(BASE_TANK_MB, this::isRecipeFluid) {
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
    private int blades;
    private int ticksSinceRescan;
    private int progress;
    private int activeTimeTicks;
    private int activeEnergyPerTick;
    private int activeStatus = STATUS_NOT_FORMED;
    // Where the vat's interior is, for the fluid renderer (server copy, filled on formation).
    private int centerX, centerZ, bottomY;

    // What the client draws inside the vat, received through the block entity update packet.
    private boolean renderFormed;
    private int renderCenterX, renderCenterZ, renderBottomY, renderLayers;
    private FluidStack renderFluid = FluidStack.EMPTY;
    private float renderFill;

    // Last values sent, so an update goes out only when the picture actually changes.
    private int sentFillPercent = -1;
    @Nullable
    private Fluid sentFluid;
    private boolean sentFormed;

    /** Rotor column of the formed structure, bottom to top, so the blades can be set spinning. */
    private List<BlockPos> rotorPositions = List.of();
    /** Null until the first sync, so blades saved mid-spin are corrected after a reload. */
    @Nullable
    private Boolean rotorsSpinning;

    @Nullable
    private Set<Fluid> acceptedFluids;

    /** Values shown by the screen, in the index order the screen reads them. */
    private final WideContainerData containerData = new WideContainerData(
            () -> level != null && level.isClientSide,
            () -> formed ? 1 : 0,
            () -> bodyLayers,
            () -> blades,
            () -> progress,
            () -> activeTimeTicks,
            () -> (int) Math.round(heatCapacitor.getTemperature()),
            inputTank::getFluidAmount,
            inputTank::getCapacity,
            outputTank::getFluidAmount,
            outputTank::getCapacity,
            energy::getEnergyStored,
            energy::getMaxEnergyStored,
            () -> activeEnergyPerTick,
            () -> BuiltInRegistries.FLUID.getId(inputTank.getFluid().getFluid()),
            () -> BuiltInRegistries.FLUID.getId(outputTank.getFluid().getFluid()),
            () -> activeStatus);

    public MixerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(TwoGetherCoreMod.MIXER_CONTROLLER_BE.get(), pos, state);
    }

    public WideContainerData getContainerData() {
        return containerData;
    }

    public ItemStackHandler getItemSlots() {
        return inputItems;
    }

    public IFluidHandler getFluidCapability() {
        return fluidCapability;
    }

    // ---- MultiblockController ----

    @Override
    public FluidTank getInputTank() {
        return inputTank;
    }

    @Override
    public FluidTank getOutputTank() {
        return outputTank;
    }

    @Override
    public IItemHandler getInputItems() {
        return inputItems;
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    @Override
    public List<IHeatCapacitor> getHeatCapacitors(@Nullable Direction side) {
        return List.of(heatCapacitor);
    }

    @Override
    public void onContentsChanged() {
        setChanged();
    }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MixerMenu(containerId, inventory, this);
    }

    // ---- Tick ----

    public void serverTick() {
        if (level == null) return;
        tickMachine();
        updateRotors();
        syncRender();
    }

    /** The fluid shown in the vat: what is being mixed, or the product once the input is used up. */
    private FluidStack displayedFluid() {
        return inputTank.isEmpty() ? outputTank.getFluid() : inputTank.getFluid();
    }

    private int displayedCapacity() {
        return inputTank.isEmpty() ? outputTank.getCapacity() : inputTank.getCapacity();
    }

    /**
     * Pushes the vat's contents to nearby clients, but only when the level moves by a whole percent
     * or the fluid or formation changes - not every tick while the mixer runs.
     */
    private void syncRender() {
        if (level == null) return;
        FluidStack shown = displayedFluid();
        int percent = shown.isEmpty() ? 0 : (int) ((long) shown.getAmount() * 100 / Math.max(1, displayedCapacity()));
        Fluid fluid = shown.isEmpty() ? null : shown.getFluid();
        if (percent == sentFillPercent && fluid == sentFluid && formed == sentFormed) return;
        sentFillPercent = percent;
        sentFluid = fluid;
        sentFormed = formed;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag render = new CompoundTag();
        render.putBoolean("formed", formed);
        render.putInt("cx", centerX);
        render.putInt("cz", centerZ);
        render.putInt("bottom", bottomY);
        render.putInt("layers", bodyLayers);
        FluidStack shown = displayedFluid();
        render.put("fluid", shown.saveOptional(registries));
        render.putFloat("fill", shown.isEmpty() ? 0 : (float) shown.getAmount() / Math.max(1, displayedCapacity()));
        tag.put("render", render);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readRender(tag, registries);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        readRender(packet.getTag(), registries);
    }

    private void readRender(CompoundTag tag, HolderLookup.Provider registries) {
        if (!tag.contains("render")) return;
        CompoundTag render = tag.getCompound("render");
        renderFormed = render.getBoolean("formed");
        renderCenterX = render.getInt("cx");
        renderCenterZ = render.getInt("cz");
        renderBottomY = render.getInt("bottom");
        renderLayers = render.getInt("layers");
        renderFluid = FluidStack.parseOptional(registries, render.getCompound("fluid"));
        renderFill = render.getFloat("fill");
    }

    // ---- Read by MixerFluidRenderer on the client ----

    boolean isRenderFormed() {
        return renderFormed;
    }

    BlockPos getRenderInteriorBottom() {
        return new BlockPos(renderCenterX, renderBottomY, renderCenterZ);
    }

    int getRenderLayers() {
        return renderLayers;
    }

    FluidStack getRenderFluid() {
        return renderFluid;
    }

    float getRenderFill() {
        return renderFill;
    }

    private void tickMachine() {
        if (!formed || ++ticksSinceRescan >= RESCAN_INTERVAL_TICKS) {
            ticksSinceRescan = 0;
            tryFormStructure();
            refreshAcceptedFluids();
        }

        if (!formed) {
            stop(STATUS_NOT_FORMED);
            return;
        }
        heatCapacitor.coolTowardAmbient(PASSIVE_COOLING_K_PER_TICK * HEAT_CAPACITY);

        if (blades == 0) {
            stop(STATUS_NO_BLADES);
            return;
        }

        MixingRecipe recipe = findRecipe();
        if (recipe == null) {
            stop(STATUS_NO_RECIPE);
            return;
        }

        // Blades trade power for speed: the same recipe costs the same energy per batch whatever
        // the blade count, it just gets through it faster.
        activeTimeTicks = Math.max(1, recipe.time() / blades);
        activeEnergyPerTick = recipe.energyPerTick() * blades;
        boolean heated = recipe.minTemperature() > 0;
        boolean hot = !heated || heatCapacitor.getTemperature() >= recipe.minTemperature();
        boolean powered = energy.getEnergyStored() >= activeEnergyPerTick;
        boolean hasRoom = outputTank.fill(recipe.result(), IFluidHandler.FluidAction.SIMULATE) == recipe.result().getAmount();

        activeStatus = !powered ? STATUS_NO_POWER
                : !hot ? STATUS_TOO_COLD
                : !hasRoom ? STATUS_OUTPUT_FULL
                : STATUS_RUNNING;

        if (activeStatus != STATUS_RUNNING) {
            progress = 0;
            return;
        }

        energy.consume(activeEnergyPerTick);
        if (heated) heatCapacitor.handleHeat(-HEATED_RUN_COST_K_PER_TICK * HEAT_CAPACITY);
        if (++progress >= activeTimeTicks) {
            progress = 0;
            craft(recipe);
        }
        setChanged();
    }

    /** Spins the blades while the Mixer is producing and stops them otherwise; only writes on change. */
    private void updateRotors() {
        boolean spinning = formed && activeStatus == STATUS_RUNNING;
        if (level == null || Boolean.valueOf(spinning).equals(rotorsSpinning)) return;
        rotorsSpinning = spinning;
        for (BlockPos pos : rotorPositions) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof MixerRotorBlock && state.getValue(MixerRotorBlock.ACTIVE) != spinning) {
                level.setBlock(pos, state.setValue(MixerRotorBlock.ACTIVE, spinning), 3);
            }
        }
    }

    private void stop(int status) {
        activeStatus = status;
        activeEnergyPerTick = 0;
        if (progress != 0) {
            progress = 0;
            setChanged();
        }
    }

    private void craft(MixingRecipe recipe) {
        int[] taken = recipe.matchIngredients(slotContents());
        if (taken == null) return;
        for (int slot : taken) inputItems.extractItem(slot, 1, false);
        recipe.fluidInput().ifPresent(fluid -> inputTank.drain(fluid.amount(), IFluidHandler.FluidAction.EXECUTE));
        outputTank.fill(recipe.result().copy(), IFluidHandler.FluidAction.EXECUTE);
    }

    private List<ItemStack> slotContents() {
        List<ItemStack> stacks = new ArrayList<>(INPUT_SLOTS);
        for (int i = 0; i < INPUT_SLOTS; i++) stacks.add(inputItems.getStackInSlot(i));
        return stacks;
    }

    @Nullable
    private MixingRecipe findRecipe() {
        if (level == null) return null;
        return level.getRecipeManager()
                .getRecipeFor(TwoGetherCoreMod.MIXING_TYPE.get(), new MixingRecipe.Input(inputTank.getFluid(), slotContents()), level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    /**
     * True if some mixing recipe takes this fluid. Cached and refreshed on the structure rescan,
     * because the tank asks on every pipe insertion attempt.
     */
    private boolean isRecipeFluid(FluidStack stack) {
        if (level == null || stack.isEmpty()) return false;
        if (acceptedFluids == null) refreshAcceptedFluids();
        return acceptedFluids.contains(stack.getFluid());
    }

    private void refreshAcceptedFluids() {
        if (level == null) return;
        Set<Fluid> accepted = new HashSet<>();
        for (RecipeHolder<MixingRecipe> holder : level.getRecipeManager().getAllRecipesFor(TwoGetherCoreMod.MIXING_TYPE.get())) {
            holder.value().fluidInput().map(SizedFluidIngredient::getFluids).ifPresent(fluids -> {
                for (FluidStack fluid : fluids) accepted.add(fluid.getFluid());
            });
        }
        acceptedFluids = accepted;
    }

    // ---- Structure ----

    /** The controller sits in a body ring; try each face-centre cell as where it could be. */
    private void tryFormStructure() {
        for (DistillationTowerShape.Offset hypothesis : DistillationTowerShape.CONTROLLER_CELLS) {
            if (attemptFormAt(worldPosition.getX() - hypothesis.dx(), worldPosition.getZ() - hypothesis.dz())) return;
        }
        setFormed(false);
    }

    private boolean attemptFormAt(int cx, int cz) {
        int y = worldPosition.getY();
        if (!isBodyRing(cx, cz, y)) return false;

        int bottom = y;
        while (y - bottom < MixerShape.MAX_BODY_LAYERS && isBodyRing(cx, cz, bottom - 1)) bottom--;
        int top = y;
        while (top - bottom + 1 < MixerShape.MAX_BODY_LAYERS && isBodyRing(cx, cz, top + 1)) top++;

        int layers = top - bottom + 1;
        if (layers < MixerShape.MIN_BODY_LAYERS || layers > MixerShape.MAX_BODY_LAYERS) return false;
        if (!isSolidLayer(cx, cz, bottom - 1, false)) return false;
        if (!isSolidLayer(cx, cz, top + 1, true)) return false;

        int bladeCount = 0;
        for (int ry = bottom; ry <= top; ry++) {
            bladeCount += level.getBlockState(new BlockPos(cx, ry, cz)).getValue(MixerRotorBlock.BLADES);
        }

        for (int ry = bottom - 1; ry <= top + 1; ry++) {
            for (DistillationTowerShape.Offset o : DistillationTowerShape.FULL_R3) {
                BlockPos pos = new BlockPos(cx + o.dx(), ry, cz + o.dz());
                if (level.getBlockEntity(pos) instanceof DistillationTowerValveBlockEntity valve) {
                    valve.setControllerPos(worldPosition);
                }
            }
        }

        List<BlockPos> rotors = new ArrayList<>();
        for (int ry = bottom; ry <= top; ry++) rotors.add(new BlockPos(cx, ry, cz));
        rotorPositions = rotors;
        centerX = cx;
        centerZ = cz;
        bottomY = bottom;
        bodyLayers = layers;
        blades = bladeCount;
        int capacity = BASE_TANK_MB * MixerShape.FLUID_CELLS_PER_LAYER * layers;
        inputTank.setCapacity(capacity);
        outputTank.setCapacity(capacity);
        setFormed(true);
        setChanged();
        return true;
    }

    private void setFormed(boolean newFormed) {
        if (formed == newFormed) return;
        formed = newFormed;
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.hasProperty(MixerControllerBlock.FORMED)) {
                level.setBlock(worldPosition, state.setValue(MixerControllerBlock.FORMED, newFormed), 3);
            }
        }
    }

    /** A body ring: brass wall (or this controller), a rotor in the centre, open space elsewhere. */
    private boolean isBodyRing(int cx, int cz, int y) {
        for (DistillationTowerShape.Offset o : DistillationTowerShape.FULL_R3) {
            BlockPos pos = new BlockPos(cx + o.dx(), y, cz + o.dz());
            BlockState state = level.getBlockState(pos);
            if (o.dx() == 0 && o.dz() == 0) {
                if (!(state.getBlock() instanceof MixerRotorBlock)) return false;
            } else if (DistillationTowerShape.WALL_R3.contains(o)) {
                if (!pos.equals(worldPosition) && !isShell(state)) return false;
            } else if (!isOpen(state)) {
                return false;
            }
        }
        return true;
    }

    /** Floor or lid: solid brass, except the lid's centre, which is the drive. */
    private boolean isSolidLayer(int cx, int cz, int y, boolean lid) {
        for (DistillationTowerShape.Offset o : DistillationTowerShape.FULL_R3) {
            BlockPos pos = new BlockPos(cx + o.dx(), y, cz + o.dz());
            BlockState state = level.getBlockState(pos);
            if (lid && o.dx() == 0 && o.dz() == 0) {
                if (!state.is(TwoGetherCoreMod.MIXER_DRIVE.get())) return false;
            } else if (!isShell(state)) {
                return false;
            }
        }
        return true;
    }

    /** Brass casing, brass valve, or Mekanism's Structural Glass to see the blades turn. */
    private boolean isShell(BlockState state) {
        return state.is(TwoGetherCoreMod.MIXER_CASING.get())
                || state.is(TwoGetherCoreMod.MIXER_VALVE.get())
                || state.is(STRUCTURAL_GLASS);
    }

    private static final net.minecraft.resources.ResourceLocation STRUCTURAL_GLASS_ID =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanism", "structural_glass");
    private static final net.minecraft.world.level.block.Block STRUCTURAL_GLASS =
            BuiltInRegistries.BLOCK.get(STRUCTURAL_GLASS_ID);

    private static boolean isOpen(BlockState state) {
        if (state.isAir()) return true;
        FluidState fluid = state.getFluidState();
        return fluid.isEmpty() && state.canBeReplaced();
    }

    // ---- Fluid capability for buckets on the controller itself ----

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

    /** Receive-only from outside; spent internally while mixing. */
    private final class Buffer extends EnergyStorage {
        Buffer() {
            super(ENERGY_CAPACITY, ENERGY_MAX_RECEIVE, 0);
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) setChanged();
            return received;
        }

        void consume(int amount) {
            energy -= Math.min(energy, amount);
        }

        void set(int amount) {
            energy = Math.max(0, Math.min(capacity, amount));
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", inputItems.serializeNBT(registries));
        tag.put("inputTank", inputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("outputTank", outputTank.writeToNBT(registries, new CompoundTag()));
        tag.put("heat", heatCapacitor.serializeNBT(registries));
        tag.putInt("energy", energy.getEnergyStored());
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) inputItems.deserializeNBT(registries, tag.getCompound("items"));
        if (tag.contains("inputTank")) inputTank.readFromNBT(registries, tag.getCompound("inputTank"));
        if (tag.contains("outputTank")) outputTank.readFromNBT(registries, tag.getCompound("outputTank"));
        if (tag.contains("heat")) heatCapacitor.deserializeNBT(registries, tag.getCompound("heat"));
        energy.set(tag.getInt("energy"));
        progress = tag.getInt("progress");
    }
}
