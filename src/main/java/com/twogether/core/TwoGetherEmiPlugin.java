package com.twogether.core;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import mekanism.client.recipe_viewer.emi.MekanismEmiRecipeCategory;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Puts the tower's fermenting recipes in EMI, under our own category and on our own blocks. */
@EmiEntrypoint
public class TwoGetherEmiPlugin implements EmiPlugin {

    public static final MekanismEmiRecipeCategory FERMENTING = new MekanismEmiRecipeCategory(
            FermentingRecipeViewerType.INSTANCE,
            EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get()),
            EmiStack.of(TwoGetherCoreMod.DISTILLATION_CONTROLLER_COPPER.get()));

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
            registry.addRecipe(new FermentingEmiRecipe(FERMENTING, holder));
        }

        registry.addCategory(MIXING);
        registry.addWorkstation(MIXING, EmiStack.of(TwoGetherCoreMod.MIXER_CONTROLLER.get()));
        for (RecipeHolder<MixingRecipe> holder :
                registry.getRecipeManager().getAllRecipesFor(TwoGetherCoreMod.MIXING_TYPE.get())) {
            registry.addRecipe(new MixingEmiRecipe(MIXING, holder));
        }
    }
}
