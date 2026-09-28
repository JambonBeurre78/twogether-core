package com.twogether.core;

import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Describes the fermenting category to Mekanism's recipe-viewer framework, so their category and
 * recipe renderers lay our entries out the way they lay out their own machines. The window below
 * is the top half of DistillationTowerControllerScreen, so the EMI entry is literally a crop of
 * the machine's own interface.
 */
public class FermentingRecipeViewerType implements IRecipeViewerRecipeType<FermentingRecipe> {

    public static final FermentingRecipeViewerType INSTANCE = new FermentingRecipeViewerType();

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(TwoGetherCoreMod.MODID, "fermenting");

    private FermentingRecipeViewerType() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public Class<? extends FermentingRecipe> recipeClass() {
        return FermentingRecipe.class;
    }

    @Override
    public boolean requiresHolder() {
        return true;
    }

    @Override
    public ItemStack iconStack() {
        return new ItemStack(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get());
    }

    @Override
    public ResourceLocation icon() {
        return ID;
    }

    // Same window Mekanism uses for its own evaporating display, and for the same reason: the
    // offsets are where the machine GUI's origin sits relative to the recipe view, so they are
    // negative. Our GUI is the same 196 wide as theirs, so these values transfer as-is.
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
        return List.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get(),
                TwoGetherCoreMod.DISTILLATION_CONTROLLER_STEEL.get());
    }

    @Override
    public Component getTextComponent() {
        return Component.translatable("emi.category.twogethercore.fermenting");
    }
}
