package net.squidstudios.mfhoppers.util.item;

import me.clip.placeholderapi.PlaceholderAPI;
import net.squidstudios.mfhoppers.util.item.nbt.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ItemBuilder {

    private ItemStack item;
    private List<String> lore = new ArrayList<>();
    private List<ItemFlag> flags = new ArrayList<>();
    private List<ItemFlag> flagsToRemove = new ArrayList<>();
    private HashMap<Object, Object> nbts = new HashMap<>();
    private List<IEnchant> enchants = new ArrayList<>();
    private List<Enchantment> enchantsToRemove = new ArrayList<>();
    private List<String> keys_to_remove = new ArrayList<>();
    private String name;

    public ItemBuilder(Material mat){
        item = new ItemStack(mat);
    }

    public ItemBuilder(ItemStack item){
        this.item = item;
    }
    public ItemBuilder addNbt(Object ob1, Object ob2){
        nbts.put(ob1,ob2);
        return this;
    }
    public ItemBuilder addToLore(String string){
        lore.add(c(string));
        return this;
    }
    private String c(String s){
        return ChatColor.translateAlternateColorCodes('&',s);
    }
    public ItemBuilder removeFromLore(int index){
        lore.remove(index);
        return this;
    }
    public ItemBuilder setName(String s){
        name = s;
        return this;
    }
    public ItemBuilder addToLore(String string, Integer index){
        lore.add(index,c(string));
        return this;
    }
    public ItemStack buildItem(){

        ItemMeta meta = item.getItemMeta();

        List<String> finalLore = new ArrayList<>();

        if(meta.hasLore()){
            finalLore.addAll(meta.getLore());
        }
        finalLore.addAll(lore);
        meta.setLore(finalLore);
        if(name != null) {
            meta.setDisplayName(c(name));
        }

        for(ItemFlag flag : flagsToRemove){

            meta.removeItemFlags(flag);

        }
        for(ItemFlag flag : flags){

            meta.addItemFlags(flag);

        }

        for(Enchantment enchant : enchantsToRemove){

            meta.removeEnchant(enchant);

        }

        for(IEnchant enchantment : enchants){

            meta.addEnchant(enchantment.getEnchantment(), enchantment.getSize(), true);

        }

        item.setItemMeta(meta);

        NBTItem nbt = new NBTItem(item);

        for(Object key : nbts.keySet()){

            nbt.setString(key.toString(), nbts.get(key).toString().replace("\"", ""));

        }
        for(String s : keys_to_remove){

            nbt.removeKey(s);

        }
        return nbt.getItem();
    }
    public ItemBuilder removeFromNbt(String key){
        keys_to_remove.add(key);
        return this;
    }
    public ItemBuilder clearLore(){
        lore.clear();
        return this;
    }
    public ItemBuilder setLore(List<String> l){
        lore = l;
        return this;
    }
    public ItemBuilder replaceInLore(String key, String to){
        List<String> copy = new ArrayList<>(lore);
        lore.clear();
        for(String s : copy){
            s = s.replaceAll(key,to);
            lore.add(s);
        }
        return this;
    }
    public ItemBuilder random(String key){
        nbts.put(key + "_randomness_" + ThreadLocalRandom.current().nextInt(999999), key + "_randomness_" + ThreadLocalRandom.current().nextInt(999999));
        return this;
    }
    public ItemBuilder random(){
        nbts.put("_randomness_" + ThreadLocalRandom.current().nextInt(999999), "_randomness_" + ThreadLocalRandom.current().nextInt(999999));
        return this;
    }
    public ItemBuilder addItemFlag(ItemFlag flag){

        flags.add(flag);
        return this;

    }
    public ItemBuilder removeItemFlag(ItemFlag flag){

        flagsToRemove.remove(flag);
        return this;

    }
    public ItemBuilder addEnchant(Enchantment enchant, int level){

        enchants.add(new IEnchant(enchant, level));
        return this;

    }
    public ItemBuilder removeEnchant(Enchantment enchant){

        enchantsToRemove.add(enchant);
        return this;

    }
    public ItemBuilder setLore(List<String> lore, boolean coloured){

        if(coloured){

            List<String> s = new ArrayList<>();
            lore.forEach(s1 -> s.add(c(s1)));
            this.lore = s;

        }
        return this;

    }

    public ItemBuilder replaceLore(Map<String, Object> replace) {
        return replaceLore(replace, null);
    }

    public ItemBuilder replaceLore(Map<String, Object> replace, Player player) {

        List<String> newLore = new ArrayList<>();

        for (String l : this.lore) {

            for (String r : replace.keySet()) {
                l = l.replaceAll(r, replace.get(r).toString());
            }

            try {
                if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    if (player != null) {
                        PlaceholderAPI.setPlaceholders(player, l);
                    }
                }
            } catch (NoClassDefFoundError ignore) {}

            newLore.add(l);

        }
        this.lore = newLore;
        return this;
    }

    public ItemBuilder setDurability(byte data){
        item.setDurability(data);
        return this;
    }
}
