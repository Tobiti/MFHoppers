package net.squidstudios.mfhoppers.util.inv;

import com.google.common.collect.HashBiMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class InventoryBuilder implements InventoryHolder {
    private InvType type = InvType.SINGLE;
    private Consumer<InventoryCloseEvent> closeEvent = null;
    private Consumer<InventoryClickEvent> clickEvent = null;
    private Consumer<InventoryOpenEvent> openEvent = null;

    //PAGED STUFF
    private HashBiMap<Inventory, Integer> pages = HashBiMap.create();
    private List<ItemStack> items = new ArrayList<>();
    private Inventory currentInventory;
    private int size;
    private String title;
    private ItemStack forward;
    private ItemStack back;

    public InventoryBuilder(String title, int size){

        this.currentInventory = Bukkit.createInventory(this, size, color(title));

    }
    public InventoryBuilder(String title, int size, InvType type){

        if(type == InvType.PAGED) {
            this.currentInventory = Bukkit.createInventory(this, size, color(title));
            this.type = type;
            this.size = size;
            this.title = title;
        } else if(type == InvType.SINGLE){
            this.currentInventory = Bukkit.createInventory(this, size, color(title));
        }

    }
    public Inventory buildInventory(){
        if(type == InvType.PAGED){

            int page = 1;
            int backSlot = currentInventory.getSize() - 9;
            int forwardSlot = currentInventory.getSize() - 1;

            pages.put(currentInventory, page);
            page++;

            int pagesNeeded = (int)Math.round((double) items.size() / (double)currentInventory.getSize());

            if(pagesNeeded == 1){

                if(items.size() - currentInventory.getSize() > 0){

                    pagesNeeded++;

                }

            }

            if(pagesNeeded > 1){
                currentInventory.setItem(forwardSlot, forward);
            }

            for(ItemStack item : items){

                if(countFreeSlots(currentInventory) == 0){

                    Inventory inv = Bukkit.createInventory(this, this.size, color(title));
                    currentInventory = inv;

                    if(page != pagesNeeded){
                        currentInventory.setItem(forwardSlot, back);
                    }
                    if(pagesNeeded != 1){
                        currentInventory.setItem(backSlot, back);
                    }

                    pages.put(currentInventory, page);
                    inv.addItem(item);
                    page++;

                } else{

                    currentInventory.addItem(item);

                }

            }

            return pages.inverse().get(1);


        } else {
            return currentInventory;
        }
    }
    protected String color(String c){

        return ChatColor.translateAlternateColorCodes('&', c);

    }

    /**
     * Use this when it's ONLY single paged
     */
    public InventoryBuilder setItem(int slot, ItemStack item){

        currentInventory.setItem(slot, item);
        return this;

    }
    public InventoryBuilder addItem(ItemStack item){
        if(type == InvType.PAGED){
            items.add(item);
        } else{
            currentInventory.addItem(item);
        }
        return this;
    }

    public InventoryBuilder setCloseListener(Consumer<InventoryCloseEvent> consumer){

        this.closeEvent = consumer;
        return this;

    }
    public InventoryBuilder setClickListener(Consumer<InventoryClickEvent> consumer){

        this.clickEvent = consumer;
        return this;

    }
    public InventoryBuilder setOpenListener(Consumer<InventoryOpenEvent> consumer){

        this.openEvent = consumer;
        return this;

    }

    @Override
    public Inventory getInventory() {
        return buildInventory();
    }

    public Consumer<InventoryOpenEvent> getOpenEvent() {
        return openEvent;
    }

    public Consumer<InventoryCloseEvent> getCloseEvent() {
        return closeEvent;
    }

    public Consumer<InventoryClickEvent> getClickEvent() {
        return clickEvent;
    }

    public void setBack(ItemStack back) {
        this.back = back;
    }

    public void setForward(ItemStack forward) {
        this.forward = forward;
    }

    public ItemStack getBack() {
        return back;
    }

    public ItemStack getForward() {
        return forward;
    }
    public Inventory getBackInv(Inventory inv){
        if(pages.containsKey(inv)){
            int page = pages.get(inv);
            page--;
            if(pages.inverse().containsKey(page)){
                return pages.inverse().get(page);
            }

        } else{
            return inv;
        }
        return inv;
    }
    public Inventory getForwardInv(Inventory inv){


        if(pages.containsKey(inv)){
            int page = pages.get(inv);
            page++;

            if(pages.inverse().containsKey(page)){

                return pages.inverse().get(page);
            }

        } else{
            return null;
        }
        return null;
    }

    public int countFreeSlots(Inventory inventory){
        return Arrays.stream(inventory.getContents()).filter(item -> item == null || item.getType() == Material.AIR).toArray().length;
    }
    public InventoryBuilder fill(ItemStack item){
        for(int i = 0; i < currentInventory.getSize();i++){
            currentInventory.setItem(i, item.clone());
        }
        return this;
    }
}
