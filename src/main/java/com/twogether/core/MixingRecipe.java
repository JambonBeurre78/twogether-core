package com.twogether.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;
import java.util.Optional;

/**
 * What the Mixer does: solid ingredients (one of each consumed per batch, taken from a shared
 * pool so four grapes can sit in one slot), an optional fluid, and a fluid out. Energy is per
 * tick at one blade; the controller scales speed and draw with the blade count. A non-zero
 * minimum temperature makes it a heated recipe, like Create's heated mixing.
 */
public record MixingRecipe(List<Ingredient> ingredients, Optional<SizedFluidIngredient> fluidInput,
                           FluidStack result, int time, int energyPerTick, double minTemperature)
        implements Recipe<MixingRecipe.Input> {

    public record Input(FluidStack fluid, List<ItemStack> items) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }

        /**
         * Must be overridden: RecipeManager.getRecipeFor bails out on an "empty" input, and the
         * default check only looks at items - a mixer holding only fluid would never match.
         */
        @Override
        public boolean isEmpty() {
            return fluid.isEmpty() && items.stream().allMatch(ItemStack::isEmpty);
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (fluidInput.isPresent() && !fluidInput.get().test(input.fluid())) return false;
        return matchIngredients(input.items()) != null;
    }

    /**
     * Which slot each ingredient is taken from, or null if they cannot all be covered. Several
     * ingredients may draw from the same slot as long as its count allows it.
     */
    public int[] matchIngredients(List<ItemStack> slots) {
        int[] remaining = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) remaining[i] = slots.get(i).getCount();

        int[] taken = new int[ingredients.size()];
        for (int n = 0; n < ingredients.size(); n++) {
            Ingredient ingredient = ingredients.get(n);
            int found = -1;
            for (int i = 0; i < slots.size(); i++) {
                if (remaining[i] > 0 && ingredient.test(slots.get(i))) {
                    found = i;
                    break;
                }
            }
            if (found < 0) return null;
            remaining[found]--;
            taken[n] = found;
        }
        return taken;
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
        return TwoGetherCoreMod.MIXING_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<Input>> getType() {
        return TwoGetherCoreMod.MIXING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<MixingRecipe> {

        private static final MapCodec<MixingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.LIST_CODEC_NONEMPTY.fieldOf("ingredients").forGetter(MixingRecipe::ingredients),
                SizedFluidIngredient.NESTED_CODEC.optionalFieldOf("fluid").forGetter(MixingRecipe::fluidInput),
                FluidStack.CODEC.fieldOf("result").forGetter(MixingRecipe::result),
                Codec.INT.optionalFieldOf("time", 100).forGetter(MixingRecipe::time),
                Codec.INT.optionalFieldOf("energy_per_tick", 20).forGetter(MixingRecipe::energyPerTick),
                Codec.DOUBLE.optionalFieldOf("min_temperature", 0.0).forGetter(MixingRecipe::minTemperature)
        ).apply(instance, MixingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, MixingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), MixingRecipe::ingredients,
                ByteBufCodecs.optional(SizedFluidIngredient.STREAM_CODEC), MixingRecipe::fluidInput,
                FluidStack.STREAM_CODEC, MixingRecipe::result,
                ByteBufCodecs.VAR_INT, MixingRecipe::time,
                ByteBufCodecs.VAR_INT, MixingRecipe::energyPerTick,
                ByteBufCodecs.DOUBLE, MixingRecipe::minTemperature,
                MixingRecipe::new);

        @Override
        public MapCodec<MixingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MixingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
