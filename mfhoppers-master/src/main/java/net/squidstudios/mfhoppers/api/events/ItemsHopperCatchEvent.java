package net.squidstudios.mfhoppers.api.events;

import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.util.moveableItem.MoveItem;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemsHopperCatchEvent extends Event implements Cancellable {

    final static HandlerList handlers = new HandlerList();
    private boolean isCancelled = false;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public ItemsHopperCatchEvent(List<MoveItem> itemList, Collection<IHopper> hopperList) {
        this.itemList = itemList;
        this.hopperList = hopperList;
    }

    private List<MoveItem> itemList;
    private List<MoveItem> itemStackList = new ArrayList<>();

    private Collection<IHopper> hopperList;

    public List<MoveItem> getItemStackList() {
        return itemStackList;
    }

    public Collection<IHopper> getHopperList() {
        return hopperList;
    }

    public List<MoveItem> getItemList() {
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

    public static ItemsHopperCatchEvent fromAsync(List<MoveItem> itemList, List<IHopper> hopperList, JavaPlugin plugin) {

        CompletableFuture<ItemsHopperCatchEvent> toComplete = new CompletableFuture<>();
        new BukkitRunnable(){
            @Override
            public void run() {
                toComplete.complete(new ItemsHopperCatchEvent(itemList, hopperList));
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
