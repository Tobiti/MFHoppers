package net.squidstudios.mfhoppers.util.item;

import org.bukkit.enchantments.Enchantment;

public class IEnchant {
    private Enchantment enchantment;
    private int size;

    public IEnchant(Enchantment enchantment, int size){

        this.enchantment = enchantment;
        this.size = size;

    }

    public Enchantment getEnchantment() {
        return enchantment;
    }

    public int getSize() {
        return size;
    }
}
