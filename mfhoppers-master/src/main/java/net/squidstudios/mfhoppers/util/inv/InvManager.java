package net.squidstudios.mfhoppers.util.inv;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.squidstudios.mfhoppers.util.OVersion;

import static org.bukkit.Bukkit.getServer;

public class InvManager implements Listener {
    private JavaPlugin plugin;
    private static InvManager instance;
    public InvManager(JavaPlugin plugin){
        instance = this;
        this.plugin = plugin;
        getServer().getPluginManager().registerEvents(this,plugin);
    }
    @EventHandler
    public void onClick(InventoryClickEvent event){

        if(event.getWhoClicked().getOpenInventory().getTopInventory() != null && event.getClickedInventory() != null) {

            InventoryHolder holder = event.getView().getTopInventory().getHolder();

            if (holder instanceof InventoryBuilder) {
                event.setCancelled(true);

                InventoryBuilder builder = (InventoryBuilder) holder;

                if (event.getCurrentItem() != null && isSimilar(event.getCurrentItem(), builder.getBack())) {
                    if (event.getCurrentItem() != null) {

                        event.getWhoClicked().closeInventory();
                        Inventory inv = builder.getBackInv(event.getClickedInventory());
                        if (inv != null) {
                            event.getWhoClicked().openInventory(inv);
                        } else {
                            event.getWhoClicked().sendMessage(ChatColor.translateAlternateColorCodes('&', "&cERROR! Contact staff now!"));
                        }
                        return;

                    }
                } else if (event.getCurrentItem() != null && isSimilar(event.getCurrentItem(), builder.getForward())) {
                    if (event.getCurrentItem() != null) {

                        event.getWhoClicked().closeInventory();
                        Inventory inv = builder.getForwardInv(event.getClickedInventory());
                        if (inv != null) {
                            event.getWhoClicked().openInventory(inv);
                        } else {
                            event.getWhoClicked().sendMessage(ChatColor.translateAlternateColorCodes('&', "&cERROR! Contact staff now!"));
                        }
                        return;
                    }
                }

                if (builder.getClickEvent() != null) {

                    builder.getClickEvent().accept(event);

                }
            }

        }

    }

    @EventHandler
    public void onClose(InventoryCloseEvent event){

        if(event.getPlayer().getOpenInventory().getTopInventory() != null) {


            InventoryHolder holder = event.getView().getTopInventory().getHolder();

            if(holder instanceof InventoryBuilder){

                InventoryBuilder builder = (InventoryBuilder)holder;

                if(builder.getCloseEvent() != null){

                    builder.getCloseEvent().accept(event);

                }

            }

        }

    }
    @EventHandler
    public void onOpen(InventoryOpenEvent event){

        if(event.getPlayer().getOpenInventory().getTopInventory() != null) {

            InventoryHolder holder = event.getView().getTopInventory().getHolder();

            if(holder instanceof InventoryBuilder){

                InventoryBuilder builder = (InventoryBuilder)holder;

                if(builder.getOpenEvent() != null){

                    builder.getOpenEvent().accept(event);
                }

            }

        }
    }
    public boolean isSimilar(ItemStack first, ItemStack second){

        boolean similar = false;

        if(first == null || second == null){
            return similar;
        }

        boolean sameDurability = (first.getDurability() == second.getDurability());
        boolean sameAmount = (first.getAmount() == second.getAmount());
        boolean sameHasItemMeta = (first.hasItemMeta() == second.hasItemMeta());
        boolean sameEnchantments = (first.getEnchantments().equals(second.getEnchantments()));
        boolean sameItemMeta = true;

        if(sameHasItemMeta) {
            sameItemMeta = Bukkit.getItemFactory().equals(first.getItemMeta(), second.getItemMeta());
        }

        if(sameDurability && sameAmount && sameHasItemMeta && sameEnchantments && sameItemMeta){
            similar = true;
        }

        return similar;

    }
}
