package com.twogether.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

/**
 * Aging in the Aging Cask: whatever amount of the input fluid sits in the cask turns into the
 * result once it has rested there for the given time. No power, no heat, just time.
 */
public record AgingRecipe(FluidIngredient input, Fluid result, int time) implements Recipe<FermentingRecipe.Input> {

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
        return TwoGetherCoreMod.AGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<FermentingRecipe.Input>> getType() {
        return TwoGetherCoreMod.AGING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<AgingRecipe> {

        private static final MapCodec<AgingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FluidIngredient.CODEC_NON_EMPTY.fieldOf("input").forGetter(AgingRecipe::input),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("result").forGetter(AgingRecipe::result),
                Codec.INT.fieldOf("time").forGetter(AgingRecipe::time)
        ).apply(instance, AgingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, AgingRecipe> STREAM_CODEC = StreamCodec.composite(
                FluidIngredient.STREAM_CODEC, AgingRecipe::input,
                ByteBufCodecs.registry(Registries.FLUID), AgingRecipe::result,
                ByteBufCodecs.VAR_INT, AgingRecipe::time,
                AgingRecipe::new);

        @Override
        public MapCodec<AgingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AgingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
