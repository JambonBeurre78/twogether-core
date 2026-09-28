package com.twogether.core;

import net.minecraft.world.inventory.ContainerData;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * ContainerData that survives vanilla's sync: ClientboundContainerSetDataPacket writes each value
 * as a short, so anything above 32767 - tank capacities, energy - arrives wrapped on the client.
 * Each logical int is carried in two slots (low and high 16 bits) and reassembled on read.
 *
 * <p>On the server, slots are computed live from the suppliers so vanilla's change detection sees
 * every update; on the client they hold whatever the last packets delivered.
 */
public final class WideContainerData implements ContainerData {

    private final BooleanSupplier isClient;
    private final IntSupplier[] sources;
    private final int[] synced;

    public WideContainerData(BooleanSupplier isClient, IntSupplier... sources) {
        this.isClient = isClient;
        this.sources = sources;
        this.synced = new int[sources.length * 2];
    }

    /** The logical value: live on the server, reassembled from the synced halves on the client. */
    public int getValue(int index) {
        if (!isClient.getAsBoolean()) return sources[index].getAsInt();
        return ((synced[index * 2 + 1] & 0xFFFF) << 16) | (synced[index * 2] & 0xFFFF);
    }

    @Override
    public int get(int slot) {
        if (isClient.getAsBoolean()) return synced[slot];
        int value = sources[slot / 2].getAsInt();
        return slot % 2 == 0 ? value & 0xFFFF : (value >>> 16) & 0xFFFF;
    }

    @Override
    public void set(int slot, int value) {
        synced[slot] = value;
    }

    @Override
    public int getCount() {
        return synced.length;
    }
}
