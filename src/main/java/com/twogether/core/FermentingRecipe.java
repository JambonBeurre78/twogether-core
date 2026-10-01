package com.twogether.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * Fermentation in the Distillation Tower: a fluid in, a fluid out, a duration and a minimum
 * temperature. This is deliberately our own recipe type rather than Mekanism's evaporating one -
 * sharing theirs would have put our beer and wine on their Thermal Evaporation Plant, and their
 * brine on our tower.
 */
public record FermentingRecipe(SizedFluidIngredient input, FluidStack result, int time, double minTemperature)
        implements Recipe<FermentingRecipe.Input>, TowerRecipe {

    /** Fermentation only needs warmth: there is no upper limit. */
    @Override
    public double maxTemperature() {
        return Double.POSITIVE_INFINITY;
    }

    /** Fluid-only input: the tower ferments a tank's contents, no item is involved. */
    public record Input(FluidStack fluid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        /**
         * Must be overridden: RecipeManager.getRecipeFor bails out on an "empty" input, and the
         * default emptiness check only looks at items - of which a fluid recipe has none, so
         * every lookup would silently find nothing.
         */
        @Override
        public boolean isEmpty() {
            return fluid.isEmpty();
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return this.input.test(input.fluid());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
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
    public RecipeSerializer<? extends Recipe<Input>> getSerializer() {
        return TwoGetherCoreMod.FERMENTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<Input>> getType() {
        return TwoGetherCoreMod.FERMENTING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<FermentingRecipe> {

        private static final MapCodec<FermentingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                SizedFluidIngredient.NESTED_CODEC.fieldOf("input").forGetter(FermentingRecipe::input),
                FluidStack.CODEC.fieldOf("result").forGetter(FermentingRecipe::result),
                Codec.INT.optionalFieldOf("time", 200).forGetter(FermentingRecipe::time),
                Codec.DOUBLE.optionalFieldOf("min_temperature", 350.0).forGetter(FermentingRecipe::minTemperature)
        ).apply(instance, FermentingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, FermentingRecipe> STREAM_CODEC = StreamCodec.composite(
                SizedFluidIngredient.STREAM_CODEC, FermentingRecipe::input,
                FluidStack.STREAM_CODEC, FermentingRecipe::result,
                ByteBufCodecs.VAR_INT, FermentingRecipe::time,
                ByteBufCodecs.DOUBLE, FermentingRecipe::minTemperature,
                FermentingRecipe::new);

        @Override
        public MapCodec<FermentingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FermentingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
