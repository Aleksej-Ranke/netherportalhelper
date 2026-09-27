package de.alek.netherportalhelper.util;

import net.minecraft.core.BlockPos;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;

public final class PortalTrackerRegressionCheck {
    private PortalTrackerRegressionCheck() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        checkNetherToOverworld(-9, -72);
        checkNetherToOverworld(-8, -64);
        checkNetherToOverworld(-1, -8);
        checkNetherToOverworld(0, 0);
        checkNetherToOverworld(9, 72);

        checkOverworldToNether(-9, -2);
        checkOverworldToNether(-8, -1);
        checkOverworldToNether(-1, -1);
        checkOverworldToNether(0, 0);
        checkOverworldToNether(9, 1);
    }

    private static void checkNetherToOverworld(int netherCoordinate, int expected) {
        BlockPos actual = PortalTracker.calculateCounterpartPosition(
                new BlockPos(netherCoordinate, 70, netherCoordinate), Level.NETHER
        );
        assert actual.equals(new BlockPos(expected, 70, expected)) : actual;
    }

    private static void checkOverworldToNether(int overworldCoordinate, int expected) {
        BlockPos actual = PortalTracker.calculateCounterpartPosition(
                new BlockPos(overworldCoordinate, 70, overworldCoordinate), Level.OVERWORLD
        );
        assert actual.equals(new BlockPos(expected, 70, expected)) : actual;
    }
}
