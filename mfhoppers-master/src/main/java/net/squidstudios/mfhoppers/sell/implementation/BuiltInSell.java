package net.squidstudios.mfhoppers.sell.implementation;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.sell.ISell;
import net.squidstudios.mfhoppers.sell.SellItem;
import net.squidstudios.mfhoppers.util.Methods;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BuiltInSell extends ISell {

    private List<SellItem> items = new ArrayList<>();

    @Override
    public double getPrice(ItemStack item) {

        SellItem sellitem = items.stream().filter(sellItem -> sellItem.equalsItem(item)).findFirst().orElse(null);

        return sellitem == null ? 0.0 : sellitem.getPrice();

    }

    public BuiltInSell(List<String> strings){

        super("BuiltIn");
        strings.forEach(it -> items.add(new SellItem(it)));

        MFHoppers.getInstance().out("&b[BuiltIn]&3: Loaded " + items.size() + " sell items!");

    }
}
