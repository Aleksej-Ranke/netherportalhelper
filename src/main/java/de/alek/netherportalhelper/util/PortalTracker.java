package de.alek.netherportalhelper.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortalTracker {
    private static boolean active = false;
    private static BlockPos targetPos = null;
    private static RegistryKey<World> targetDimension = null;

    public static boolean isActive() {
        return active;
    }

    public static BlockPos getTargetPos() {
        return targetPos;
    }

    public static RegistryKey<World> getTargetDimension() {
        return targetDimension;
    }

    public static boolean isPortalDimension(RegistryKey<World> dimension) {
        return dimension == World.NETHER || dimension == World.OVERWORLD;
    }

    public static RegistryKey<World> getCounterpartDimension(RegistryKey<World> currentDimension) {
        if (currentDimension == World.NETHER) {
            return World.OVERWORLD;
        }
        if (currentDimension == World.OVERWORLD) {
            return World.NETHER;
        }
        return null;
    }

    public static BlockPos calculateCounterpartPosition(BlockPos currentPos, RegistryKey<World> currentDimension) {
        if (currentDimension == World.NETHER) {
            return new BlockPos(currentPos.getX() * 8, currentPos.getY(), currentPos.getZ() * 8);
        }
        if (currentDimension == World.OVERWORLD) {
            return new BlockPos(Math.floorDiv(currentPos.getX(), 8), currentPos.getY(), Math.floorDiv(currentPos.getZ(), 8));
        }
        return null;
    }

    public static boolean lockTarget(BlockPos calculatedTarget, RegistryKey<World> calculatedDimension) {
        if (calculatedTarget == null || calculatedDimension == null) {
            return false;
        }
        active = true;
        targetPos = calculatedTarget;
        targetDimension = calculatedDimension;
        return true;
    }

    public static void clear() {
        active = false;
        targetPos = null;
        targetDimension = null;
    }

    public static void toggleFreeze(BlockPos calculatedTarget, RegistryKey<World> calculatedDimension) {
        if (active) {
            clear();
        } else {
            lockTarget(calculatedTarget, calculatedDimension);
        }
    }
}
