package net.squidstudios.mfhoppers.util;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class MChunk {
    private int x;
    private int z;
    private String world_name;
    public MChunk(Chunk chunk){
        this.world_name = chunk.getWorld().getName();
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public World getWorld() {

        if(Bukkit.getWorld(world_name) != null){
            return Bukkit.getWorld(world_name);
        } else{
            throw new NullPointerException("Cannot find world by name:" + world_name);
        }

    }
    public Chunk getChunk(){
        return getWorld().getChunkAt(x,z);
    }

    public boolean is(Location location) {

        double chunkX = location.getBlockX() >> 4;
        double chunkZ = location.getBlockZ() >> 4;

        //if(chunkX.compareTo(getX()) == 1 || chunkZ.compareTo(getZ()) == 1) return chunkZ == getZ() && location.getWorld().getName().equalsIgnoreCase(world_name);

        return chunkX == getX() && chunkZ == getZ() && location.getWorld().getName().equalsIgnoreCase(world_name);

    }

    public boolean is(Chunk chunk) {

        return chunk.getX() == getX() && chunk.getZ() == getZ() && chunk.getWorld().getName().equalsIgnoreCase(world_name);

    }
}
