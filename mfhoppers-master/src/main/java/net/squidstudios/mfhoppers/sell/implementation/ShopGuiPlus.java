package net.squidstudios.mfhoppers.sell.implementation;

import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.manager.SellManager;
import net.squidstudios.mfhoppers.sell.ISell;
import net.squidstudios.mfhoppers.util.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShopGuiPlus extends ISell {

    private boolean hooked = false;

    public ShopGuiPlus(){

        super("ShopGUIPlus");
        if(Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus")){

            hooked = true;

        } else{

            MFHoppers.getInstance().out("Couldn't find ShopGUIPlus, disabling Hook...");

        }

    }

    @Override
    public double getPrice(ItemStack item) {

        if(!hooked) return 0;

        double price = 0.0;

        List<ShopItem> shopItems = new ArrayList<>();
        ShopGuiPlugin.getInstance().getShopManager().shops.values().forEach(shop -> shopItems.addAll(shop.getShopItems()));

        for(ShopItem sItem : shopItems.stream().filter(shopItem -> equalsItem(shopItem.getItem(), item)).collect(Collectors.toList())){

            if(price == 0.0) {
                price = sItem.getSellPriceForAmount(1);
            } else {
                if(sItem.getSellPriceForAmount(1) < price) {
                    price = sItem.getSellPriceForAmount(1);
                }
            }

        }

        return price;
    }

    public boolean equalsItem(ItemStack toCompare, ItemStack two) {

        ItemStack item = new ItemBuilder(two.getType()).setDurability((byte) two.getDurability()).buildItem();
        byte id = (byte)two.getDurability();

        if (toCompare.getType() == item.getType()) {
            if (id != -1) return id == toCompare.getDurability();
            else return true;

        }
        return false;
    }

}
