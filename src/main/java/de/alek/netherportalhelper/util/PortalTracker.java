package de.alek.netherportalhelper.util;

import net.minecraft.util.math.BlockPos;

public class PortalTracker {
    public static boolean active = false;
    public static BlockPos targetPos = null;

    public static void toggleFreeze(BlockPos calculatedTarget) {
        if (active) {
            active = false;
            targetPos = null;
        } else {
            active = true;
            targetPos = calculatedTarget;
        }
    }
}