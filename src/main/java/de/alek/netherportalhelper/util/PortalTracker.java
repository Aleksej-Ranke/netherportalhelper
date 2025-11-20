package de.alek.netherportalhelper.util;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PortalTracker {
    public static boolean active = false;
    public static BlockPos targetPos = null;
    public static RegistryKey<World> targetDimension = null;

    public static void toggleFreeze(BlockPos calculatedTarget, RegistryKey<World> calculatedDimension) {
        if (active) {
            active = false;
            targetPos = null;
            targetDimension = null;
        } else {
            active = true;
            targetPos = calculatedTarget;
            targetDimension = calculatedDimension;
        }
    }
}