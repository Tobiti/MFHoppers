package net.squidstudios.mfhoppers.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.squidstudios.mfhoppers.MFHoppers;
import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public enum MContainer {

    CHEST("Chest"),
    HOPPER("Hopper"),
    BARREL("Barrel"),
    SHULKER_BOX("ShulkerBox"),
    Dropper("Dropper"),
    Dispenser("Dispenser"),
    DoubleChest("DoubleChest");

    private String classLocation;
    private static Cache<Location, InventoryHolder> inventoriesHolderCache = CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .build();

    MContainer(String location) {
        classLocation = location;
    }

    public static Location getLocation(InventoryHolder holder) {

        try {
            if (holder instanceof org.bukkit.block.DoubleChest)
                return ((org.bukkit.block.DoubleChest) holder).getLocation();

            if (MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_9_R1.getVersionId()) {
                Container container = (Container) holder;
                return container.getLocation();
            } else {
                return ((BlockState) holder).getLocation();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    public static MContainer getOfLocation(Location loc) {
        if (loc == null) return null;

        InventoryHolder holder = null;
        try {
            holder = getInventory(loc).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (holder == null) return null;

        if (holder instanceof Chest)
            return CHEST;

        else if (holder instanceof DoubleChest)
            return DoubleChest;

        else if (holder instanceof Dispenser)
            return Dispenser;

        else if (holder instanceof Dropper)
            return Dropper;

        else if (holder instanceof Hopper)
            return HOPPER;

        else if (OVersion.isOrAfter(11)){
            if (holder instanceof ShulkerBox)
                return SHULKER_BOX;

            else if (OVersion.isOrAfter(14))
                if (holder.getClass().getSimpleName().contains("Barrel"))
                    return BARREL;
        }
        return null;
    }

    public static boolean isContainer(Location location) {
        return getOfLocation(location) != null;
    }

    public static MContainer getFromHolder(InventoryHolder holder) {

        try {
            if (MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_9_R1.getVersionId()) {
                if (!(holder instanceof Container)) return null;
                return getOfLocation(((Container) holder).getLocation());
            } else {
                if (!(holder instanceof BlockState)) return null;
                return getOfLocation(((BlockState) holder).getLocation());
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;

    }

    static boolean containsInBukkit(String classz) {
        try {
            Class.forName(classz);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static String getMinecraftName(InventoryHolder holder) {

        if (holder instanceof Chest || holder instanceof org.bukkit.block.DoubleChest) {
            return "minecraft:chest";
        } else if (holder instanceof Dropper) {
            return "minecraft:dropper";
        } else if (holder instanceof org.bukkit.block.Dispenser) {
            return "minecraft:dispenser";
        } else if (holder instanceof Hopper)
            return "minecraft:hopper";
        
        if (containsInBukkit("org.bukkit.block." + SHULKER_BOX.classLocation)) {
            if (holder instanceof ShulkerBox) {
                return "minecraft:shulker_box";
            }
        }

        return "";
    }

    public static CompletableFuture<InventoryHolder> getInventory(Location location) {
        InventoryHolder inv = inventoriesHolderCache.getIfPresent(location);
        CompletableFuture<InventoryHolder> future;

        if (inv != null) {
            future = new OFuture<>();
            future.complete(inv);

        } else if (Thread.currentThread().getName().equalsIgnoreCase("Server Thread")) {
            future = new OFuture<>();
            future.complete(_getInventoryHolder(location));

        } else {
            future = new CompletableFuture<>();
            new BukkitRunnable(){
                @Override
                public void run() {
                    future.complete(_getInventoryHolder(location));
                }
            }.runTask(MFHoppers.getInstance());
        }

        return future;
    }

    private static InventoryHolder _getInventoryHolder(Location location) {
        InventoryHolder toReturn = null;
        if (MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_9_R1.getVersionId()) {
            toReturn = ((Container) location.getBlock().getState()).getInventory().getHolder();

        } else {
            BlockState state = location.getBlock().getState();
            if (state instanceof Chest) {
                toReturn = ((Chest) state).getInventory().getHolder();

            } else if (state instanceof DoubleChest) {
                toReturn = ((DoubleChest) state).getInventory().getHolder();

            } else if (state instanceof Hopper) {
                toReturn = ((Hopper) state).getInventory().getHolder();

            } else if (state instanceof Dispenser) {
                toReturn = ((Dispenser) state).getInventory().getHolder();

            } else if (state instanceof Dropper) {
                toReturn = ((Dropper) state).getInventory().getHolder();
            }
        }

        if (toReturn != null) {
            inventoriesHolderCache.put(location, toReturn);
        }
        return toReturn;
    }

    static boolean containsMethod(Object object, String name) {
        try {
            object.getClass().getMethod(name);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isDoubleChest(Location loc) {
        if (loc.getBlock().getState() instanceof Chest) {
            if (((Chest) loc.getBlock().getState()).getInventory() instanceof DoubleChestInventory) {
                return true;
            }
        }
        return false;
    }

    public boolean canBeCasted(Object var1, Object var2) {

        try {

            var1.getClass().cast(var2);
            return true;

        } catch (Exception ex) {
            return false;
        }

    }

}