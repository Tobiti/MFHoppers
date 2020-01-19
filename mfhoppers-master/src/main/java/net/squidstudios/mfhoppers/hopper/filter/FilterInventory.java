package net.squidstudios.mfhoppers.hopper.filter;

import com.google.common.collect.Sets;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.util.inv.InventoryBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FilterInventory {

    public static FilterInventory instance;
    private Map<String, Object> data = new HashMap<>();

    public FilterInventory(Map<String, Object> data) {

        this.data = data;
        instance = this;

    }

    public FilterInventory() {
        instance = this;
    }

    public static FilterInventory getInstance() {
        return instance;
    }

    public Inventory build(IHopper hopper) {

        InventoryBuilder builder = new InventoryBuilder(c(hopper.getConfigHopper().getTitle(hopper) + " &2&lFilter"), 54);
        for (IHopper.FilterElement fe : hopper.getFilterMaterialList()) {
            if (fe.HasDamageValue) {
                builder.addItem(new ItemStack(fe.Material, 1, fe.DamageValue));

            } else {
                builder.addItem(new ItemStack(fe.Material, 1));
            }
        }
        builder.setCloseListener(inventoryCloseEvent -> {
            Set<IHopper.FilterElement> newFilter = Sets.newConcurrentHashSet();
            for (int i = 0; i < inventoryCloseEvent.getInventory().getSize(); i++) {
                ItemStack item = inventoryCloseEvent.getInventory().getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    continue;
                }

                if (item.getDurability() != 0) {
                    newFilter.add(new IHopper.FilterElement(item.getType(), true, item.getDurability()));
                } else {
                    newFilter.add(new IHopper.FilterElement(item.getType(), false, (short) 1));
                }
            }
            hopper.SetFilterMaterialList(newFilter);
        });
        builder.setClickListener(inventoryClickEvent -> {
            if (inventoryClickEvent.getCurrentItem() != null) {
                if (inventoryClickEvent.getClickedInventory() == inventoryClickEvent.getInventory()) {
                    inventoryClickEvent.getInventory().remove(inventoryClickEvent.getCurrentItem().getType());
                } else {
                    if (!inventoryClickEvent.getInventory().contains(inventoryClickEvent.getCurrentItem().getType())) {
                        inventoryClickEvent.getInventory().addItem(new ItemStack(inventoryClickEvent.getCurrentItem().getType(), 1, inventoryClickEvent.getCurrentItem().getDurability()));
                    }
                }
            }
        });
        return builder.buildInventory();

    }

    String c(Object text) {
        return ChatColor.translateAlternateColorCodes('&', text.toString());
    }
}
