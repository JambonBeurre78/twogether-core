package com.twogether.core;

import dev.emi.emi.api.widget.WidgetHolder;
import mekanism.api.recipes.ingredients.FluidStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiHorizontalRateBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.recipe_viewer.RecipeViewerUtils;
import mekanism.client.recipe_viewer.emi.MekanismEmiRecipeCategory;
import mekanism.client.recipe_viewer.emi.recipe.MekanismEmiHolderRecipe;
import mekanism.common.MekanismLang;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * One Mixer recipe, drawn by Mekanism's recipe-viewer renderer. The layout follows the Mixer's
 * own screen, squeezed on the right the way Mekanism squeezes its evaporating display, since the
 * recipe view is narrower than the machine GUI.
 */
public class MixingEmiRecipe extends MekanismEmiHolderRecipe<MixingRecipe> {

    private final MixingRecipe mixing;
    /** Index of each product among the recipe's outputs, or -1 when the recipe has none of that kind. */
    private final int fluidOutput;
    private final int itemOutput;

    public MixingEmiRecipe(MekanismEmiRecipeCategory category, RecipeHolder<MixingRecipe> holder) {
        super(category, holder);
        this.mixing = holder.value();
        for (Ingredient ingredient : mixing.ingredients()) {
            addInputDefinition(IngredientCreatorAccess.item().from(ingredient, 1));
        }
        // The fluid always sits right after the items, so the gauge can find it by index.
        mixing.fluidInput().ifPresentOrElse(fluid -> addInputDefinition(FluidStackIngredient.of(fluid)), this::addEmptyInput);
        int outputs = 0;
        if (!mixing.result().isEmpty()) addFluidOutputDefinition(List.of(mixing.result()));
        fluidOutput = mixing.result().isEmpty() ? -1 : outputs++;
        if (!mixing.resultItem().isEmpty()) addItemOutputDefinition(List.of(mixing.resultItem()));
        itemOutput = mixing.resultItem().isEmpty() ? -1 : outputs;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int items = mixing.ingredients().size();
        for (int i = 0; i < MixerControllerBlockEntity.INPUT_SLOTS; i++) {
            int x = MixerMenu.SLOT_X - 1 + (i % 2) * 18;
            int y = MixerMenu.SLOT_Y - 1 + (i / 2) * 18;
            if (i < items) addSlot(widgets, SlotType.INPUT, x, y, input(i));
            else addSlot(widgets, SlotType.NORMAL, x, y);
        }

        initTank(widgets, addElement(widgets, GuiFluidGauge.getDummy(GaugeType.STANDARD, this, 6, 13)), input(items));
        GuiFluidGauge outputGauge = addElement(widgets, GuiFluidGauge.getDummy(GaugeType.STANDARD, this, 158, 13));
        if (fluidOutput >= 0) initTank(widgets, outputGauge, output(fluidOutput));

        addElement(widgets, new GuiInnerScreen(this, 68, 19, 70, 40, this::screenLines).padding(3).clearSpacing());
        // A bottled product takes the arrow's place between the screen and the gauge.
        if (itemOutput >= 0) addSlot(widgets, SlotType.OUTPUT, 139, 36, output(itemOutput));
        else addElement(widgets, new GuiDownArrow(this, 142, 39));
        if (mixing.minTemperature() > 0) {
            // Shown full: this is the temperature the recipe asks for, not a live reading.
            addElement(widgets, new GuiHorizontalRateBar(this, RecipeViewerUtils.FULL_BAR, 68, 63));
        }
    }

    private List<Component> screenLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("emi.twogethercore.fermenting.time", mixing.time() / 20.0));
        lines.add(Component.translatable("emi.twogethercore.mixing.energy", mixing.energyPerTick()));
        lines.add(mixing.minTemperature() > 0
                ? MekanismLang.TEMPERATURE.translate(MekanismUtils.getTemperatureDisplay(mixing.minTemperature(), TemperatureUnit.KELVIN, true))
                : Component.translatable("emi.twogethercore.mixing.cold"));
        return lines;
    }
}
