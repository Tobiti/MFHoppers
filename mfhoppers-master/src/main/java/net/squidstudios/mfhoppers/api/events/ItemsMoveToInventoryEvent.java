package net.squidstudios.mfhoppers.api.events;

import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.util.moveableItem.MoveItem;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemsMoveToInventoryEvent extends Event implements Cancellable {

    final static HandlerList handlers = new HandlerList();
    private boolean isCancelled = false;

    private List<ItemStack> itemList;

    private IHopper hopper;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public ItemsMoveToInventoryEvent(List<ItemStack> items, IHopper hopper) {
        this.itemList = items;
        this.hopper = hopper;
    }

    public IHopper getHopper() {
        return hopper;
    }

    public List<ItemStack> getItemList() {
        return itemList;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.isCancelled = b;
    }

    public static ItemsMoveToInventoryEvent fromAsync(List<ItemStack> itemList, IHopper hopper, JavaPlugin plugin) {

        CompletableFuture<ItemsMoveToInventoryEvent> toComplete = new CompletableFuture<>();
        new BukkitRunnable(){
            @Override
            public void run() {
                toComplete.complete(new ItemsMoveToInventoryEvent(itemList, hopper));
            }
        }.runTask(plugin);

        try{
            return toComplete.get();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return null;

    }


}
