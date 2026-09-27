package de.alek.netherportalhelper.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record PortalBookmark(String id, String name, String dimension, int x, int y, int z) {
    public BlockPos position() {
        return new BlockPos(x, y, z);
    }

    public ResourceKey<Level> dimensionKey() {
        if (Level.OVERWORLD.identifier().toString().equals(dimension)) {
            return Level.OVERWORLD;
        }
        if (Level.NETHER.identifier().toString().equals(dimension)) {
            return Level.NETHER;
        }
        return null;
    }
}
