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
 * Four ingredient slots in a 2x2 block between the input gauge and the screen, and the player
 * inventory at the same Mekanism offset as the tower, so both machines line up.
 *
 * <p>Data layout: 0=formed, 1=bodyLayers, 2=blades, 3=progress, 4=time, 5=temperature (K),
 * 6/7=input amount/capacity, 8/9=output amount/capacity, 10/11=energy stored/max,
 * 12=energy per tick, 13/14=input/output fluid id, 15=status.
 */
public class MixerMenu extends AbstractContainerMenu {

    public static final int SLOT_X = 28;
    public static final int SLOT_Y = 20;
    private static final int INVENTORY_X = 18;
    private static final int INVENTORY_Y = 84;
    private static final int MACHINE_SLOTS = MixerControllerBlockEntity.INPUT_SLOTS;

    private final MixerControllerBlockEntity controller;

    public MixerMenu(int containerId, Inventory playerInventory, MixerControllerBlockEntity controller) {
        super(TwoGetherCoreMod.MIXER_MENU.get(), containerId);
        this.controller = controller;

        for (int i = 0; i < MACHINE_SLOTS; i++) {
            addSlot(new SlotItemHandler(controller.getItemSlots(), i, SLOT_X + (i % 2) * 18, SLOT_Y + (i / 2) * 18));
        }
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

    public MixerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveController(playerInventory, buf));
    }

    private static MixerControllerBlockEntity resolveController(Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof MixerControllerBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No Mixer controller at " + pos);
    }

    public int getData(int index) {
        return controller.getContainerData().getValue(index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(original, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, MACHINE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (controller.isRemoved() || controller.getLevel() != player.level()) return false;
        BlockPos pos = controller.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
