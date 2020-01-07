package net.squidstudios.mfhoppers.sell.implementation;

import com.google.common.collect.Sets;
import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.exception.player.PlayerDataNotLoadedException;
import net.brcdev.shopgui.player.PlayerData;
import net.brcdev.shopgui.shop.Shop;
import net.brcdev.shopgui.shop.ShopItem;
import net.brcdev.shopgui.shop.WrappedShopItem;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.manager.SellManager;
import net.squidstudios.mfhoppers.sell.ISell;
import net.squidstudios.mfhoppers.util.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static net.squidstudios.mfhoppers.util.Methods.isSimilar;

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
    public double getPrice(ItemStack item, Player player) {
        if(!hooked) return 0;
        if (player != null) {
            try {
                double price = ShopGuiPlusApi.getItemStackPriceSell(player, item);
                if (price > 0)
                    return price;
            } catch (Exception ex) {
                if (!(ex instanceof PlayerDataNotLoadedException)) {
                    ex.printStackTrace();
                }
            }
        }

        Collection<Shop> shops = Sets.newHashSet(ShopGuiPlugin.getInstance().getShopManager().shops.values());
        for (Shop shop : shops) {
            for (ShopItem shopItem : shop.getShopItems()) {
                if (isSimilar(item, shopItem.getItem()))
                    return shopItem.getSellPrice();
            }
        }

        return 0;
    }
}
