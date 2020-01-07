package net.squidstudios.mfhoppers.util;

import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

@EqualsAndHashCode
public class MChunk {
    private int x;
    private int z;

    public MChunk(Chunk chunk) {
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }

    public MChunk(Location location) {
        this.x = location.getBlockX() >> 4;
        this.z = location.getBlockZ() >> 4;
    }

    public MChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public boolean is(Location location) {
        double chunkX = location.getBlockX() >> 4;
        double chunkZ = location.getBlockZ() >> 4;

        return chunkX == getX() && chunkZ == getZ();
    }

    public boolean is(Chunk chunk) {
        return chunk.getX() == getX() && chunk.getZ() == getZ();
    }
}
