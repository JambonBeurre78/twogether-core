package com.twogether.core;

import mekanism.client.recipe_viewer.type.IRecipeViewerRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Describes a tower category (fermenting or distilling) to Mekanism's recipe-viewer framework, so
 * their category and recipe renderers lay our entries out the way they lay out their own
 * machines. The window below is the top half of DistillationTowerControllerScreen, so the EMI
 * entry is literally a crop of the machine's own interface.
 */
public class TowerRecipeViewerType<R extends TowerRecipe> implements IRecipeViewerRecipeType<R> {

    public static final TowerRecipeViewerType<FermentingRecipe> FERMENTING =
            new TowerRecipeViewerType<>("fermenting", FermentingRecipe.class);
    public static final TowerRecipeViewerType<DistillingRecipe> DISTILLING =
            new TowerRecipeViewerType<>("distilling", DistillingRecipe.class);

    private final ResourceLocation id;
    private final Class<R> recipeClass;

    private TowerRecipeViewerType(String name, Class<R> recipeClass) {
        this.id = ResourceLocation.fromNamespaceAndPath(TwoGetherCoreMod.MODID, name);
        this.recipeClass = recipeClass;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Class<? extends R> recipeClass() {
        return recipeClass;
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
        return id;
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
        return Component.translatable("emi.category." + id.getNamespace() + "." + id.getPath());
    }
}
