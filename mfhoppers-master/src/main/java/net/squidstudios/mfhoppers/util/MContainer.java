package net.squidstudios.mfhoppers.util;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.util.item.nbt.utils.MinecraftVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.*;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import static net.squidstudios.mfhoppers.MFHoppers.is13version;
import static net.squidstudios.mfhoppers.MFHoppers.is9version;

public enum MContainer {

    CHEST("Chest"),
    HOPPER("Hopper"),
    SHULKER_BOX("ShulkerBox"),
    Dropper("Dropper"),
    Dispenser("Dispenser"),
    DoubleChest("DoubleChest");

    private String classLocation;

    MContainer(String location) {
        classLocation = location;
    }

    public Location getLocation(InventoryHolder holder){

        try {
            if(holder.getInventory() instanceof DoubleChestInventory)
                return ((org.bukkit.block.Chest)holder).getLocation();

            if(MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_9_R1.getVersionId()) {
                Container container = (Container) holder;
                return container.getLocation();
            }
            else {
                return ((BlockState) holder).getLocation();
            }

        } catch (Exception e){
            e.printStackTrace();
        }
        return null;

    }

    public static MContainer getOfLocation(Location loc){

        if(loc == null) return null;
        Material material = loc.getBlock().getType();

        if(loc.getBlock().getState() instanceof Chest){

            Inventory inv = ((Chest) loc.getBlock().getState()).getInventory();

            if(inv instanceof DoubleChestInventory){
                return DoubleChest;
            }
        }

        if(material.name().contains("CHEST")){
            return CHEST;
        } else if(material.name().contains("DISPENSER")){
            return Dispenser;
        } else if(material.name().contains("DROPPER")){
            return Dropper;
        } else if(material.name().contains("HOPPER")) {
            return HOPPER;
        } else if(is9version || is13version) {
            if (material.name().contains("SHULKER_BOX")) {
                return SHULKER_BOX;
            }
        }
        return null;
    }

    public static String getContainerName(Location location){

        try {

            return getOfLocation(location).getInventory(location).getTitle();

        } catch (Exception ex){
            ex.printStackTrace();
        }

        return "";
    }

    public static boolean isContainer(Location location){
        return getOfLocation(location) != null;

    }

    public static MContainer getFromHolder(InventoryHolder holder) {

        try {
            if(MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_9_R1.getVersionId()) {
                if (!(holder instanceof Container)) return null;
                return getOfLocation(((Container) holder).getLocation());
            }
            else {
                if (!(holder instanceof BlockState)) return null;
                return getOfLocation(((BlockState) holder).getLocation());
            }

        } catch (Exception ex){
            ex.printStackTrace();
        }

        return null;

    }

    static boolean containsInBukkit(String classz){
        try{
            Class.forName(classz);
            return true;
        } catch (Exception ex){
            return false;
        }
    }

    public static String getMinecraftName(InventoryHolder holder){

        if(holder instanceof Chest || holder instanceof org.bukkit.block.DoubleChest){
            return "minecraft:chest";
        } else if(holder instanceof Dropper){
            return "minecraft:dropper";
        } else if(holder instanceof org.bukkit.block.Dispenser){
            return "minecraft:dispenser";
        }
        if(containsInBukkit("org.bukkit.block." + SHULKER_BOX.classLocation)){
            if(holder instanceof ShulkerBox){
                return "minecraft:shulker_box";
            }
        }

        return "";
    }

    public Inventory getInventory(Location location){
        if(MinecraftVersion.getVersion().getVersionId() >= MinecraftVersion.MC1_9_R1.getVersionId()) {
            return ((Container) location.getBlock().getState()).getInventory();
        }
        else {
            if(location.getBlock().getState() instanceof Chest){
                return ((Chest)location.getBlock().getState()).getInventory();
            }
            if(location.getBlock().getState() instanceof Hopper){
                return ((Hopper)location.getBlock().getState()).getInventory();
            }
            if(location.getBlock().getState() instanceof Dispenser){
                return ((Dispenser)location.getBlock().getState()).getInventory();
            }
            if(location.getBlock().getState() instanceof Dropper){
                return ((Dropper)location.getBlock().getState()).getInventory();
            }
            return null;
        }
    }

    static boolean containsMethod(Object object, String name){

        try{
            object.getClass().getMethod(name);
            return true;
        } catch (Exception ex){
            return false;
        }

    }

    public static boolean isDoubleChest(Location loc){
        if(loc.getBlock().getState() instanceof Chest){
            if(((Chest) loc.getBlock().getState()).getInventory() instanceof DoubleChestInventory){
                return true;
            }
        }
        return false;
    }

    public boolean canBeCasted(Object var1, Object var2){

        try {

            var1.getClass().cast(var2);
            return true;

        } catch (Exception ex) {
            return false;
        }

    }

}