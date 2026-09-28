package com.twogether.core;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiDownArrow;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.bar.GuiBar.IBarInfoHandler;
import mekanism.client.gui.element.bar.GuiHorizontalRateBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiFluidGauge;
import mekanism.client.gui.element.tab.GuiHeatTab;
import mekanism.client.gui.element.tab.GuiWarningTab;
import mekanism.common.MekanismLang;
import mekanism.common.content.evaporation.EvaporationMultiblockData;
import mekanism.common.inventory.warning.IWarningTracker;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

/**
 * Structure, coordinates and widgets are Mekanism's own GuiThermalEvaporationController
 * (src/main/java/mekanism/client/gui, MIT licensed), running on their real GuiMekanism
 * base class - which is generic over any AbstractContainerMenu, so our own menu plugs
 * straight into it. Only the data sources point at our controller instead of theirs.
 */
public class DistillationTowerControllerScreen extends GuiMekanism<DistillationTowerControllerMenu> {

    private GuiElement inputGauge, outputGauge;

    private final ClientFluidTankView inputTank;
    private final ClientFluidTankView outputTank;

    public DistillationTowerControllerScreen(DistillationTowerControllerMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        imageWidth += 20;
        inventoryLabelX += 10;
        inventoryLabelY += 2;
        dynamicSlots = true;
        inputTank = new ClientFluidTankView(() -> fluidById(menu.getData(12)), () -> menu.getData(6), () -> menu.getData(7));
        outputTank = new ClientFluidTankView(() -> fluidById(menu.getData(13)), () -> menu.getData(8), () -> menu.getData(9));
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiInnerScreen(this, 48, 15, 100, 46, () -> List.of(
              menu.getData(0) != 0 ? MekanismLang.MULTIBLOCK_FORMED.translate() : MekanismLang.MULTIBLOCK_INCOMPLETE.translate(),
              MekanismLang.EVAPORATION_HEIGHT.translate(menu.getData(1)),
              MekanismLang.TEMPERATURE.translate(MekanismUtils.getTemperatureDisplay(menu.getData(5), TemperatureUnit.KELVIN, true)),
              MekanismLang.FLUID_PRODUCTION.translate(Math.round(lastGain() * 100D) / 100D)
        )).padding(3).clearSpacing());
        addRenderableWidget(new GuiDownArrow(this, 32, 39));
        addRenderableWidget(new GuiDownArrow(this, 156, 39));
        addRenderableWidget(new GuiHorizontalRateBar(this, new IBarInfoHandler() {
            @Override
            public Component getTooltip() {
                return MekanismUtils.getTemperatureDisplay(menu.getData(5), TemperatureUnit.KELVIN, true);
            }

            @Override
            public double getLevel() {
                return Math.min(1, menu.getData(5) / EvaporationMultiblockData.MAX_MULTIPLIER_TEMP);
            }
        }, 58, 62))
              //Note: We just apply this warning to the bar as we don't have an arrow or anything here
              .warning(WarningType.INPUT_DOESNT_PRODUCE_OUTPUT, tooCold());
        inputGauge = addRenderableWidget(new GuiFluidGauge(() -> inputTank, this::fluidTanks, GaugeType.STANDARD, this, 6, 13))
              .warning(WarningType.NO_MATCHING_RECIPE, noUsableInput());
        outputGauge = addRenderableWidget(new GuiFluidGauge(() -> outputTank, this::fluidTanks, GaugeType.STANDARD, this, 172, 13))
              .warning(WarningType.NO_SPACE_IN_OUTPUT, noOutputSpace());
        addRenderableWidget(new GuiHeatTab(this, () -> {
            Component environment = MekanismUtils.getTemperatureDisplay(menu.getData(10) / 1_000D, TemperatureUnit.KELVIN, false);
            return Collections.singletonList(MekanismLang.DISSIPATED_RATE.translate(environment));
        }));
    }

    /** Names why the tower is idle, so the player is not left guessing which condition failed. */
    private Component statusLine() {
        int status = menu.getData(14);
        if (status == DistillationTowerControllerBlockEntity.STATUS_RUNNING) {
            return Component.translatable("gui.twogethercore.status.running").withStyle(ChatFormatting.GREEN);
        }
        String key = switch (status) {
            case DistillationTowerControllerBlockEntity.STATUS_NOT_FORMED -> "gui.twogethercore.status.not_formed";
            case DistillationTowerControllerBlockEntity.STATUS_NO_RECIPE -> "gui.twogethercore.status.no_recipe";
            case DistillationTowerControllerBlockEntity.STATUS_NOT_ENOUGH_INPUT -> "gui.twogethercore.status.not_enough_input";
            case DistillationTowerControllerBlockEntity.STATUS_TOO_COLD -> "gui.twogethercore.status.too_cold";
            case DistillationTowerControllerBlockEntity.STATUS_OUTPUT_FULL -> "gui.twogethercore.status.output_full";
            default -> "gui.twogethercore.status.idle";
        };
        return Component.translatable(key).withStyle(ChatFormatting.RED);
    }

    private static Fluid fluidById(int id) {
        return BuiltInRegistries.FLUID.byId(id);
    }

    private List<IExtendedFluidTank> fluidTanks() {
        return List.of(inputTank, outputTank);
    }

    /** mB produced per tick by the loaded recipe, same role as EvaporationMultiblockData.lastGain. */
    private double lastGain() {
        if (menu.getData(0) == 0) return 0;
        return (double) menu.getData(11) / Math.max(1, menu.getData(4));
    }

    private BooleanSupplier tooCold() {
        return () -> menu.getData(14) == DistillationTowerControllerBlockEntity.STATUS_TOO_COLD;
    }

    private BooleanSupplier noUsableInput() {
        return () -> menu.getData(14) == DistillationTowerControllerBlockEntity.STATUS_NO_RECIPE
                || menu.getData(14) == DistillationTowerControllerBlockEntity.STATUS_NOT_ENOUGH_INPUT;
    }

    private BooleanSupplier noOutputSpace() {
        return () -> menu.getData(14) == DistillationTowerControllerBlockEntity.STATUS_OUTPUT_FULL;
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

    /** Client-side view of a synced tank: display only, so the mutators are no-ops. */
    private static final class ClientFluidTankView implements IExtendedFluidTank {

        private final Supplier<Fluid> fluid;
        private final IntSupplier amountSupplier;
        private final IntSupplier capacitySupplier;

        ClientFluidTankView(Supplier<Fluid> fluid, IntSupplier amountSupplier, IntSupplier capacitySupplier) {
            this.fluid = fluid;
            this.amountSupplier = amountSupplier;
            this.capacitySupplier = capacitySupplier;
        }

        @Override
        public FluidStack getFluid() {
            int amount = amountSupplier.getAsInt();
            Fluid contents = fluid.get();
            return amount <= 0 || contents == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(contents, amount);
        }

        @Override
        public int getCapacity() {
            return Math.max(1, capacitySupplier.getAsInt());
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == fluid.get();
        }

        @Override
        public void setStack(FluidStack stack) {
        }

        @Override
        public void setStackUnchecked(FluidStack stack) {
        }

        @Override
        public void onContentsChanged() {
        }
    }
}
