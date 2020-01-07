package net.squidstudios.mfhoppers.sell;

import net.squidstudios.mfhoppers.manager.SellManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class ISell {

    private String name = "";

    public ISell(String name){
        this.name = name;

        SellManager.getInstance().add(this);

    }

    public abstract double getPrice(ItemStack item, Player player);

    public String getName(){
        return name;
    }

}
