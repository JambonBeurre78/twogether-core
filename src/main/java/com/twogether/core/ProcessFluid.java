package com.twogether.core;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * A tinted process fluid - fluid type, source, flowing form, placeable block and bucket - registered
 * in one go. Must be constructed during TwoGetherCoreMod's static init, before the registers are
 * attached to the mod bus.
 */
public final class ProcessFluid {

    public final String name;
    public final DeferredHolder<FluidType, FluidType> type;
    public final DeferredHolder<Fluid, BaseFlowingFluid.Source> still;
    public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
    public final DeferredBlock<LiquidBlock> block;
    public final DeferredItem<Item> bucket;

    public ProcessFluid(String name, int tint, MapColor mapColor) {
        this.name = name;
        this.type = TwoGetherCoreMod.FLUID_TYPES.register(name, () -> TwoGetherCoreMod.processFluidType(name, tint));
        this.still = TwoGetherCoreMod.FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
        this.flowing = TwoGetherCoreMod.FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
        this.block = TwoGetherCoreMod.BLOCKS.registerBlock(name,
                props -> new LiquidBlock(still.get(), props), TwoGetherCoreMod.processBlockProperties(mapColor));
        this.bucket = TwoGetherCoreMod.ITEMS.registerItem(name + "_bucket",
                props -> new BucketItem(still.get(), props.craftRemainder(Items.BUCKET).stacksTo(1)));
    }

    private BaseFlowingFluid.Properties properties() {
        return TwoGetherCoreMod.processProperties(type, still, flowing).bucket(bucket).block(block);
    }
}
