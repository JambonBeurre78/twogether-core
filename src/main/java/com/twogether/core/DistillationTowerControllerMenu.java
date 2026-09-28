package com.twogether.core;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Slot coordinates follow Mekanism's own Thermal Evaporation Controller container:
 * inventory at MekanismContainer.getInventoryXOffset/YOffset (8, 84) plus the
 * container type's registered .offset(10, 0), and the machine slot where their
 * input-tank item slot sits, so GuiMekanism's dynamicSlots draws everything in
 * the same places as theirs.
 *
 * Data slot layout: 0=formed, 1=bodyLayers, 2=throughputMultiplier, 3=progress,
 * 4=fermentTimeTicks, 5=temperature(K), 6=inputTank amount, 7=inputTank capacity,
 * 8=outputTank amount, 9=outputTank capacity, 10=environment loss (K/t x1000).
 */
public class DistillationTowerControllerMenu extends AbstractContainerMenu {

    private static final int INVENTORY_X = 18;
    private static final int INVENTORY_Y = 84;

    private final DistillationTowerControllerBlockEntity controller;

    public DistillationTowerControllerMenu(int containerId, Inventory playerInventory, DistillationTowerControllerBlockEntity controller) {
        super(TwoGetherCoreMod.DISTILLATION_CONTROLLER_MENU.get(), containerId);
        this.controller = controller;

        addSlot(new SlotItemHandler(controller.getInputSlot(), 0, 28, 20));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }

        addDataSlots(controller.getContainerData());
    }

    public DistillationTowerControllerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveController(playerInventory, buf));
    }

    private static DistillationTowerControllerBlockEntity resolveController(Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof DistillationTowerControllerBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No Distillation Tower controller at " + pos);
    }

    public DistillationTowerControllerBlockEntity getController() {
        return controller;
    }

    public int getData(int index) {
        return controller.getContainerData().get(index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index == 0) {
            if (!moveItemStackTo(original, 1, 37, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller.isRemoved() || controller.getLevel() == null) return false;
        if (player.level() != controller.getLevel()) return false;
        BlockPos pos = controller.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
