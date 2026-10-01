package com.twogether.core;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import mekanism.client.recipe_viewer.emi.MekanismEmiRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Puts the tower's fermenting and distilling recipes, and the Mixer's, in EMI, under our own category and on our own blocks. */
@EmiEntrypoint
public class TwoGetherEmiPlugin implements EmiPlugin {

    public static final MekanismEmiRecipeCategory FERMENTING = new MekanismEmiRecipeCategory(
            TowerRecipeViewerType.FERMENTING,
            EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get()),
            EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get()));

    public static final MekanismEmiRecipeCategory DISTILLING = new MekanismEmiRecipeCategory(
            TowerRecipeViewerType.DISTILLING,
            EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_STEEL.get()),
            EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_STEEL.get()));

    public static final EmiRecipeCategory AGING = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(TwoGetherCoreMod.MODID, "aging"),
            EmiStack.of(TwoGetherCoreMod.AGING_CASK.get()));

    public static final MekanismEmiRecipeCategory MIXING = new MekanismEmiRecipeCategory(
            MixingRecipeViewerType.INSTANCE,
            EmiStack.of(TwoGetherCoreMod.MIXER_CONTROLLER.get()),
            EmiStack.of(TwoGetherCoreMod.MIXER_CONTROLLER.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(FERMENTING);
        registry.addWorkstation(FERMENTING, EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get()));
        registry.addWorkstation(FERMENTING, EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_STEEL.get()));

        for (RecipeHolder<FermentingRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(TwoGetherCoreMod.FERMENTING_TYPE.get())) {
            registry.addRecipe(new TowerEmiRecipe<>(FERMENTING, holder));
        }

        registry.addCategory(DISTILLING);
        registry.addWorkstation(DISTILLING, EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get()));
        registry.addWorkstation(DISTILLING, EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_STEEL.get()));
        for (RecipeHolder<DistillingRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(TwoGetherCoreMod.DISTILLING_TYPE.get())) {
            registry.addRecipe(new TowerEmiRecipe<>(DISTILLING, holder));
        }

        registry.addCategory(AGING);
        registry.addWorkstation(AGING, EmiStack.of(TwoGetherCoreMod.AGING_CASK.get()));
        for (RecipeHolder<AgingRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(TwoGetherCoreMod.AGING_TYPE.get())) {
            registry.addRecipe(new AgingEmiRecipe(AGING, holder));
        }

        registry.addCategory(MIXING);
        registry.addWorkstation(MIXING, EmiStack.of(TwoGetherCoreMod.MIXER_CONTROLLER.get()));
        for (RecipeHolder<MixingRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(TwoGetherCoreMod.MIXING_TYPE.get())) {
            registry.addRecipe(new MixingEmiRecipe(MIXING, holder));
        }
    }
}
