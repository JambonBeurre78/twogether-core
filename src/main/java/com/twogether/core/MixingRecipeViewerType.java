package com.twogether.core;

import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Describes the Mixer category to Mekanism's recipe-viewer framework. Same window as the tower
 * and as Mekanism's evaporating display: the GUIs share the 196-pixel width, so -3 / -12 /
 * 176 x 62 transfers as-is.
 */
public class MixingRecipeViewerType implements IRecipeViewerRecipeType<MixingRecipe> {

    public static final MixingRecipeViewerType INSTANCE = new MixingRecipeViewerType();

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(TwoGetherCoreMod.MODID, "mixing");

    private MixingRecipeViewerType() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Class<? extends MixingRecipe> recipeClass() {
        return MixingRecipe.class;
    }

    @Override
    public boolean requiresHolder() {
        return true;
    }

    @Override
    public ItemStack iconStack() {
        return new ItemStack(TwoGetherCoreMod.MIXER_CONTROLLER.get());
    }

    @Override
    public ResourceLocation icon() {
        return ID;
    }

    @Override
    public int xOffset() {
        return -3;
    }

    @Override
    public int yOffset() {
        return -12;
    }

    @Override
    public int width() {
        return 176;
    }

    @Override
    public int height() {
        return 62;
    }

    @Override
    public List<ItemLike> workstations() {
        return List.of(TwoGetherCoreMod.MIXER_CONTROLLER.get());
    }

    @Override
    public Component getTextComponent() {
        return Component.translatable("emi.category.twogethercore.mixing");
    }
}
