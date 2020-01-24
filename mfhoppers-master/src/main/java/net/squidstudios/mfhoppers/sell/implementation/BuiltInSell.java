package net.squidstudios.mfhoppers.sell.implementation;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.sell.ISell;
import net.squidstudios.mfhoppers.sell.SellItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BuiltInSell extends ISell {

    private List<SellItem> items = new ArrayList<>();

    @Override
    public double getPrice(ItemStack item, Player player) {
        SellItem sellitem = items.stream().filter(sellItem -> sellItem.equalsItem(item)).findFirst().orElse(null);
        return sellitem == null ? 0.0 : sellitem.getPrice();
    }

    public BuiltInSell(List<String> strings){
        super("BuiltIn");
        strings.forEach(it ->{
            SellItem item = new SellItem(it);
            if(item.getMaterial() != null){
                items.add(item);
            }
            else {
                MFHoppers.getInstance().out(String.format("&b[BuiltIn]&3: %s contains no valid material.", it));
            }
            });

        MFHoppers.getInstance().out("&b[BuiltIn]&3: Loaded " + items.size() + " sell items!");

    }
}
