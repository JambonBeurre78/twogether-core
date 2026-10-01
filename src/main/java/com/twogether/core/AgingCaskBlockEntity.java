package com.twogether.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * A cask that ages what it holds. It only wakes once a second and does no more than add to a
 * counter, so a cellar full of them costs next to nothing. Topping a cask up with young spirit
 * dilutes the age already reached, the way blending would.
 */
public class AgingCaskBlockEntity extends BlockEntity {

    public static final int CAPACITY = 8000;
    private static final int CHECK_INTERVAL_TICKS = 20;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            onTankChanged();
        }
    };

    /** Ticks the current contents have aged, and the fluid and amount they were counted for. */
    private int agedTicks;
    private Fluid agingFluid = Fluids.EMPTY;
    private int lastAmount;

    public AgingCaskBlockEntity(BlockPos pos, BlockState state) {
        super(TwoGetherCoreMod.AGING_CASK_BE.get(), pos, state);
    }

    public FluidTank getTank() {
        return tank;
    }

    private void onTankChanged() {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty() || fluid.getFluid() != agingFluid) {
            agingFluid = fluid.getFluid();
            agedTicks = 0;
        } else if (fluid.getAmount() > lastAmount && lastAmount > 0) {
            // Fresh spirit added: the blend is as old as its average.
            agedTicks = (int) ((long) agedTicks * lastAmount / fluid.getAmount());
        }
        lastAmount = fluid.getAmount();
        setChanged();
    }

    public void serverTick() {
        if (level == null || level.getGameTime() % CHECK_INTERVAL_TICKS != 0 || tank.isEmpty()) return;
        AgingRecipe recipe = findRecipe();
        if (recipe == null) return;
        agedTicks += CHECK_INTERVAL_TICKS;
        if (agedTicks >= recipe.time()) {
            tank.setFluid(new FluidStack(recipe.result(), tank.getFluidAmount()));
            onTankChanged();
        }
        setChanged();
    }

    @Nullable
    private AgingRecipe findRecipe() {
        if (level == null) return null;
        return level.getRecipeManager()
                .getRecipeFor(TwoGetherCoreMod.AGING_TYPE.get(), new FermentingRecipe.Input(tank.getFluid()), level)
                .map(RecipeHolder::value)
                .orElse(null);
    }

    /** What the cask shows when right-clicked with an empty hand. */
    public Component describe() {
        if (tank.isEmpty()) return Component.translatable("message.twogethercore.cask.empty");
        Component contents = Component.translatable("message.twogethercore.cask.contents",
                tank.getFluid().getHoverName(), tank.getFluidAmount(), CAPACITY);
        AgingRecipe recipe = findRecipe();
        if (recipe == null) return Component.translatable("message.twogethercore.cask.not_aging", contents);
        int percent = (int) Math.min(100, 100L * agedTicks / Math.max(1, recipe.time()));
        return Component.translatable("message.twogethercore.cask.aging", contents, percent);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("agedTicks", agedTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        agingFluid = tank.getFluid().getFluid();
        lastAmount = tank.getFluidAmount();
        agedTicks = tag.getInt("agedTicks");
    }
}
