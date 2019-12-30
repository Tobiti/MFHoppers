package net.squidstudios.mfhoppers.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class Drop {

    private int max = -1;
    private int min = -1;
    private Material mat;

    public Drop(Material mat, int max) {
        this.mat = mat;
        this.max = max;
    }

    public Drop(Material mat, int min, int max) {
        this.mat = mat;
        this.max = max;
        this.min = min;
    }

    public boolean hasMin() {
        if (min == -1) {
            return false;
        } else {
            return true;
        }
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public Material getMat() {
        return mat;
    }

    public ItemStack getItem(Material mat) {

        if (mat == Material.GOLD_ORE || mat == Material.IRON_ORE) {

            mat = Material.valueOf(mat.name().split("_")[0] + "_INGOT");

        }

        ItemStack item = new ItemStack(mat);

        if (OVersion.isBefore(13)) {
            if (item.getType() == MMaterial.matchMaterial("INK_SACK"))
                item.setDurability((byte) 4);

        }

        if (hasMin()) {
            item.setAmount(ThreadLocalRandom.current().nextInt(getMin(), getMax() + 1));
        } else {
            item.setAmount(getMax());
        }
        return item;
    }
}
