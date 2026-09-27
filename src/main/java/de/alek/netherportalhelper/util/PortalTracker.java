package de.alek.netherportalhelper.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class PortalTracker {
    private static boolean active = false;
    private static BlockPos targetPos = null;
    private static ResourceKey<Level> targetDimension = null;
    private static String targetName = null;

    public static boolean isActive() {
        return active;
    }

    public static BlockPos getTargetPos() {
        return targetPos;
    }

    public static ResourceKey<Level> getTargetDimension() {
        return targetDimension;
    }

    public static String getTargetName() {
        return targetName;
    }

    public static boolean isPortalDimension(ResourceKey<Level> dimension) {
        return dimension == Level.NETHER || dimension == Level.OVERWORLD;
    }

    public static ResourceKey<Level> getCounterpartDimension(ResourceKey<Level> currentDimension) {
        if (currentDimension == Level.NETHER) {
            return Level.OVERWORLD;
        }
        if (currentDimension == Level.OVERWORLD) {
            return Level.NETHER;
        }
        return null;
    }

    public static BlockPos calculateCounterpartPosition(BlockPos currentPos, ResourceKey<Level> currentDimension) {
        if (currentDimension == Level.NETHER) {
            return new BlockPos(currentPos.getX() * 8, currentPos.getY(), currentPos.getZ() * 8);
        }
        if (currentDimension == Level.OVERWORLD) {
            return new BlockPos(Math.floorDiv(currentPos.getX(), 8), currentPos.getY(), Math.floorDiv(currentPos.getZ(), 8));
        }
        return null;
    }

    public static boolean lockTarget(BlockPos calculatedTarget, ResourceKey<Level> calculatedDimension) {
        return lockTarget(calculatedTarget, calculatedDimension, null);
    }

    public static boolean lockTarget(BlockPos calculatedTarget, ResourceKey<Level> calculatedDimension, String name) {
        if (calculatedTarget == null || calculatedDimension == null) {
            return false;
        }
        active = true;
        targetPos = calculatedTarget;
        targetDimension = calculatedDimension;
        targetName = name;
        return true;
    }

    public static void clear() {
        active = false;
        targetPos = null;
        targetDimension = null;
        targetName = null;
    }

    public static void toggleFreeze(BlockPos calculatedTarget, ResourceKey<Level> calculatedDimension) {
        if (active) {
            clear();
        } else {
            lockTarget(calculatedTarget, calculatedDimension);
        }
    }
}
