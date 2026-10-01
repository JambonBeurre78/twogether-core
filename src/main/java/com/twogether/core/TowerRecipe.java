package com.twogether.core;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/**
 * What the Distillation Tower needs from a recipe, whichever process it is: fermenting and
 * distilling run the same way and differ only in the temperature window they accept.
 */
public interface TowerRecipe {

    SizedFluidIngredient input();

    FluidStack result();

    int time();

    double minTemperature();

    /** Above this the tower stops; infinite for processes that only need to be warm enough. */
    double maxTemperature();
}
