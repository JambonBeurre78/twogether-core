package com.twogether.core;

import dev.emi.emi.api.widget.WidgetHolder;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiHorizontalRateBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.recipe_viewer.RecipeViewerUtils;
import mekanism.client.recipe_viewer.emi.MekanismEmiRecipeCategory;
import mekanism.client.recipe_viewer.emi.recipe.MekanismEmiHolderRecipe;
import mekanism.common.MekanismLang;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

/**
 * One tower recipe, fermenting or distilling, drawn by Mekanism's own recipe-viewer renderer with
 * the exact element coordinates of DistillationTowerControllerScreen - so the EMI entry is the
 * machine's interface, not a drawing of it. Only the recipes and the categories are ours, which
 * is what keeps them off the Thermal Evaporation Plant.
 */
public class TowerEmiRecipe<R extends Recipe<?> & TowerRecipe> extends MekanismEmiHolderRecipe<R> {

    private final R recipe;

    public TowerEmiRecipe(MekanismEmiRecipeCategory category, RecipeHolder<R> holder) {
        super(category, holder);
        this.recipe = holder.value();
        addInputDefinition(FluidStackIngredient.of(recipe.input()));
        addFluidOutputDefinition(List.of(recipe.result()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        addElement(widgets, new GuiInnerScreen(this, 48, 19, 86, 40, this::screenLines)
                .padding(3).clearSpacing());
        addElement(widgets, new GuiDownArrow(this, 32, 39));
        addElement(widgets, new GuiDownArrow(this, 142, 39));
        // Shown full: this is the temperature the recipe asks for, not a live reading.
        addElement(widgets, new GuiHorizontalRateBar(this, RecipeViewerUtils.FULL_BAR, 51, 63));

        initTank(widgets, addElement(widgets, GuiFluidGauge.getDummy(GaugeType.STANDARD, this, 6, 13)), input(0));
        initTank(widgets, addElement(widgets, GuiFluidGauge.getDummy(GaugeType.STANDARD, this, 158, 13)), output(0));
    }

    private List<Component> screenLines() {
        Component temperature = Double.isInfinite(recipe.maxTemperature())
                ? MekanismLang.TEMPERATURE.translate(kelvin(recipe.minTemperature()))
                : Component.translatable("emi.twogethercore.distilling.window", kelvin(recipe.minTemperature()), kelvin(recipe.maxTemperature()));
        return List.of(
                temperature,
                Component.translatable("emi.twogethercore.fermenting.time", recipe.time() / 20.0),
                Component.translatable("emi.twogethercore.fermenting.amount",
                        recipe.input().amount(), recipe.result().getAmount()));
    }

    private static Component kelvin(double temperature) {
        return MekanismUtils.getTemperatureDisplay(temperature, TemperatureUnit.KELVIN, true);
    }
}
