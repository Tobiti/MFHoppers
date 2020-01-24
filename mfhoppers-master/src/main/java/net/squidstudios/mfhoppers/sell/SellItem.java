package net.squidstudios.mfhoppers.sell;

import net.squidstudios.mfhoppers.util.XMaterial;
import net.squidstudios.mfhoppers.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;


public class SellItem {

    private int id = 0;

    public int getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public double getPrice() {
        return price;
    }

    private Material material;
    private double price;

    public SellItem(String toFind){

        String split[] = toFind.split(":");

        if((split.length - 1) == 1){

            id = -1;
            Optional<XMaterial> optional = XMaterial.matchXMaterial(split[0]);
            if(optional.isPresent()){
                material = optional.get().parseMaterial();
            }
            price = Double.valueOf(split[1]);

        } else if((split.length - 1) == 2){

            id = Integer.valueOf(split[1]);
            Optional<XMaterial> optional = XMaterial.matchXMaterial(split[0]);
            if(optional.isPresent()){
                material = optional.get().parseMaterial();
            }
            price = Double.valueOf(split[2]);

        }

    }

    public boolean equalsItem(ItemStack toCompare) {

        ItemStack item = new ItemBuilder(getMaterial()).setDurability((byte) getId()).buildItem();

        if (toCompare.getType() == item.getType()) {
            if (id != -1) return id == toCompare.getDurability();
            else return true;

        }
        return false;
    }
}
