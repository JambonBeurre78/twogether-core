package com.twogether.core;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import mekanism.client.gui.element.bar.GuiHorizontalRateBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.tab.GuiHeatTab;
import mekanism.client.gui.element.tab.GuiWarningTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.warning.IWarningTracker;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * The Mixer's interface, built on Mekanism's GuiMekanism like the tower's: input gauge, the four
 * ingredient slots, a status screen, the power bar and the output gauge, left to right.
 */
public class MixerScreen extends GuiMekanism<MixerMenu> {

    /** Temperature at which the heat bar reads full; heated recipes sit well below it. */
    private static final double BAR_FULL_TEMPERATURE = 1000.0;

    private GuiElement inputGauge, outputGauge;

    private final ClientFluidTankView inputTank;
    private final ClientFluidTankView outputTank;

    public MixerScreen(MixerMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth += 20;
        // Taller than Mekanism's default by the 20 pixels the product slot needs.
        imageHeight += 20;
        inventoryLabelX += 10;
        inventoryLabelY += 22;
        dynamicSlots = true;
        inputTank = new ClientFluidTankView(() -> ClientFluidTankView.fluidById(menu.getData(13)), () -> menu.getData(6), () -> menu.getData(7));
        outputTank = new ClientFluidTankView(() -> ClientFluidTankView.fluidById(menu.getData(14)), () -> menu.getData(8), () -> menu.getData(9));
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiInnerScreen(this, 68, 15, 80, 46, () -> List.of(
                menu.getData(0) != 0 ? MekanismLang.MULTIBLOCK_FORMED.translate() : MekanismLang.MULTIBLOCK_INCOMPLETE.translate(),
                Component.translatable("gui.twogethercore.mixer.blades", menu.getData(2)),
                MekanismLang.TEMPERATURE.translate(MekanismUtils.getTemperatureDisplay(menu.getData(5), TemperatureUnit.KELVIN, true)),
                Component.translatable("gui.twogethercore.mixer.power", menu.getData(12)),
                statusLine()
        )).padding(3).clearSpacing());

        addRenderableWidget(new GuiDownArrow(this, 41, 56));

        addRenderableWidget(new GuiHorizontalRateBar(this, new IBarInfoHandler() {
            @Override
            public Component getTooltip() {
                return MekanismUtils.getTemperatureDisplay(menu.getData(5), TemperatureUnit.KELVIN, true);
            }

            @Override
            public double getLevel() {
                return Math.min(1, menu.getData(5) / BAR_FULL_TEMPERATURE);
            }
        }, 68, 62)).warning(WarningType.INPUT_DOESNT_PRODUCE_OUTPUT, is(MixerControllerBlockEntity.STATUS_TOO_COLD));

        addRenderableWidget(new GuiVerticalPowerBar(this, new IBarInfoHandler() {
            @Override
            public Component getTooltip() {
                return Component.translatable("gui.twogethercore.mixer.energy", menu.getData(10), menu.getData(11));
            }

            @Override
            public double getLevel() {
                return menu.getData(11) == 0 ? 0 : (double) menu.getData(10) / menu.getData(11);
            }
        }, 156, 15)).warning(WarningType.NOT_ENOUGH_ENERGY, is(MixerControllerBlockEntity.STATUS_NO_POWER));

        inputGauge = addRenderableWidget(new GuiFluidGauge(() -> inputTank, this::fluidTanks, GaugeType.STANDARD, this, 6, 13))
                .warning(WarningType.NO_MATCHING_RECIPE, is(MixerControllerBlockEntity.STATUS_NO_RECIPE));
        outputGauge = addRenderableWidget(new GuiFluidGauge(() -> outputTank, this::fluidTanks, GaugeType.STANDARD, this, 172, 13))
                .warning(WarningType.NO_SPACE_IN_OUTPUT, is(MixerControllerBlockEntity.STATUS_OUTPUT_FULL));

        addRenderableWidget(new GuiHeatTab(this, () -> Collections.singletonList(
                MekanismLang.TEMPERATURE.translate(MekanismUtils.getTemperatureDisplay(menu.getData(5), TemperatureUnit.KELVIN, true)))));
    }

    /** Names why the mixer is idle, so the player is not left guessing which condition failed. */
    private Component statusLine() {
        int status = menu.getData(15);
        if (status == MixerControllerBlockEntity.STATUS_RUNNING) {
            return Component.translatable("gui.twogethercore.status.running").withStyle(ChatFormatting.GREEN);
        }
        String key = switch (status) {
            case MixerControllerBlockEntity.STATUS_NOT_FORMED -> "gui.twogethercore.status.not_formed";
            case MixerControllerBlockEntity.STATUS_NO_RECIPE -> "gui.twogethercore.status.no_recipe";
            case MixerControllerBlockEntity.STATUS_NO_BLADES -> "gui.twogethercore.status.no_blades";
            case MixerControllerBlockEntity.STATUS_NO_POWER -> "gui.twogethercore.status.no_power";
            case MixerControllerBlockEntity.STATUS_TOO_COLD -> "gui.twogethercore.status.too_cold";
            case MixerControllerBlockEntity.STATUS_OUTPUT_FULL -> "gui.twogethercore.status.output_full";
            default -> "gui.twogethercore.status.idle";
        };
        return Component.translatable(key).withStyle(ChatFormatting.RED);
    }

    private BooleanSupplier is(int status) {
        return () -> menu.getData(15) == status;
    }

    private List<IExtendedFluidTank> fluidTanks() {
        return List.of(inputTank, outputTank);
    }

    @Override
    protected void addWarningTab(IWarningTracker warningTracker) {
        //Move the tab to the right side of the gui so it doesn't intersect the heat tab
        addRenderableWidget(new GuiWarningTab(this, warningTracker, false));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleTextWithOffset(guiGraphics, inputGauge.getRelativeRight(), outputGauge.getRelativeX());
        renderInventoryText(guiGraphics);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
