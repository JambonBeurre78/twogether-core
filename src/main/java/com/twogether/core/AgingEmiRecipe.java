package com.twogether.core;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Arrays;
import java.util.List;

/**
 * An aging recipe in EMI. The cask has no interface to crop, so this is a plain EMI layout:
 * young spirit, an arrow, aged spirit, and how long it takes.
 */
public class AgingEmiRecipe implements EmiRecipe {

    private static final long SHOWN_AMOUNT = 1000;

    private final EmiRecipeCategory category;
    private final ResourceLocation id;
    private final AgingRecipe recipe;
    private final EmiIngredient input;
    private final EmiStack output;

    public AgingEmiRecipe(EmiRecipeCategory category, RecipeHolder<AgingRecipe> holder) {
        this.category = category;
        this.id = holder.id();
        this.recipe = holder.value();
        this.input = EmiIngredient.of(Arrays.stream(recipe.input().getStacks())
                .map(stack -> EmiStack.of(stack.getFluid(), SHOWN_AMOUNT))
                .toList());
        this.output = EmiStack.of(recipe.result(), SHOWN_AMOUNT);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(input);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of(output);
    }

    @Override
    public int getDisplayWidth() {
        return 96;
    }

    @Override
    public int getDisplayHeight() {
        return 32;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(input, 0, 0);
        widgets.addFillingArrow(36, 1, 4000);
        widgets.addSlot(output, 78, 0).recipeContext(this);
        // One Minecraft day is 24000 ticks.
        widgets.addText(Component.translatable("emi.twogethercore.aging.time", recipe.time() / 24000.0), 0, 22, 0xFF404040, false);
    }
}
