package com.twogether.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Distillation in the Distillation Tower: like fermenting, but the tower must be held inside a
 * temperature window. Below it nothing boils off; above it the water boils with the alcohol and
 * the run stops - so the heat has to be regulated, by hand or by a computer on a valve.
 */
public record DistillingRecipe(SizedFluidIngredient input, FluidStack result, int time, double minTemperature, double maxTemperature)
        implements Recipe<FermentingRecipe.Input>, TowerRecipe {

    @Override
    public boolean matches(FermentingRecipe.Input input, Level level) {
        return this.input.test(input.fluid());
    }

    @Override
    public ItemStack assemble(FermentingRecipe.Input input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends Recipe<FermentingRecipe.Input>> getSerializer() {
        return TwoGetherCoreMod.DISTILLING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<FermentingRecipe.Input>> getType() {
        return TwoGetherCoreMod.DISTILLING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<DistillingRecipe> {

        private static final MapCodec<DistillingRecipe> FIELDS = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SizedFluidIngredient.NESTED_CODEC.fieldOf("input").forGetter(DistillingRecipe::input),
                FluidStack.CODEC.fieldOf("result").forGetter(DistillingRecipe::result),
                Codec.INT.optionalFieldOf("time", 200).forGetter(DistillingRecipe::time),
                Codec.DOUBLE.fieldOf("min_temperature").forGetter(DistillingRecipe::minTemperature),
                Codec.DOUBLE.fieldOf("max_temperature").forGetter(DistillingRecipe::maxTemperature)
        ).apply(instance, DistillingRecipe::new));

        private static final MapCodec<DistillingRecipe> CODEC = FIELDS.validate(recipe -> recipe.maxTemperature() > recipe.minTemperature()
                ? DataResult.success(recipe)
                : DataResult.error(() -> "max_temperature must be above min_temperature"));

        private static final StreamCodec<RegistryFriendlyByteBuf, DistillingRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedFluidIngredient.STREAM_CODEC, DistillingRecipe::input,
                FluidStack.STREAM_CODEC, DistillingRecipe::result,
                ByteBufCodecs.VAR_INT, DistillingRecipe::time,
                ByteBufCodecs.DOUBLE, DistillingRecipe::minTemperature,
                ByteBufCodecs.DOUBLE, DistillingRecipe::maxTemperature,
                DistillingRecipe::new);

        @Override
        public MapCodec<DistillingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, DistillingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
