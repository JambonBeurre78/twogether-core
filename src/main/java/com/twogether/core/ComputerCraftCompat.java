package com.twogether.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.GenericPeripheral;
import dan200.computercraft.api.peripheral.PeripheralType;

import java.util.Map;

/**
 * Makes the valves a ComputerCraft peripheral. Registered as a generic source rather than a
 * peripheral capability, so CC keeps adding its own fluid, inventory and energy methods next to
 * ours on the same valve. Only loaded when CC:Tweaked is installed.
 */
public final class ComputerCraftCompat implements GenericPeripheral {

    static void register() {
        ComputerCraftAPI.registerGenericSource(new ComputerCraftCompat());
    }

    @Override
    public String id() {
        return TwoGetherCoreMod.MODID + ":valve";
    }

    @Override
    public PeripheralType getType() {
        return PeripheralType.ofType("twogethercore_valve");
    }

    /** Everything the machine's screen shows, as one table. */
    @LuaFunction(mainThread = true)
    public Map<String, Object> getState(DistillationTowerValveBlockEntity valve) throws LuaException {
        return controller(valve).getComputerState();
    }

    @LuaFunction(mainThread = true)
    public boolean isFormed(DistillationTowerValveBlockEntity valve) {
        MultiblockController controller = valve.getController();
        return controller != null && Boolean.TRUE.equals(controller.getComputerState().get("formed"));
    }

    /** Why the machine is or is not running: "running", "not_formed", "no_recipe", "too_cold"... */
    @LuaFunction(mainThread = true)
    public String getStatus(DistillationTowerValveBlockEntity valve) {
        MultiblockController controller = valve.getController();
        return controller == null ? "not_formed" : (String) controller.getComputerState().get("status");
    }

    @LuaFunction(mainThread = true)
    public double getTemperature(DistillationTowerValveBlockEntity valve) throws LuaException {
        return (double) controller(valve).getComputerState().get("temperature");
    }

    private static MultiblockController controller(DistillationTowerValveBlockEntity valve) throws LuaException {
        MultiblockController controller = valve.getController();
        if (controller == null) throw new LuaException("Valve is not part of a formed machine");
        return controller;
    }
}
