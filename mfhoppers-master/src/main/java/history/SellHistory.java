package history;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;

import lombok.Getter;

@Getter
public class SellHistory{
    private ConcurrentHashMap<ItemStack, Integer> soldItems = new ConcurrentHashMap<ItemStack, Integer>();

    public void AddSoldItem(ItemStack itemStack, int amount){

        if(containsKey(soldItems, itemStack)){
            ItemStack item = getItem(soldItems, itemStack);
            soldItems.put(item, soldItems.get(item) + amount);
        }
        else {
            ItemStack item = itemStack.clone();
            item.setAmount(1);
            soldItems.put(item, amount);
        }
    }

    private boolean containsKey(ConcurrentHashMap<ItemStack, Integer> soldItemList, ItemStack itemStack) {
        for (ItemStack item : soldItemList.keySet()) {
            if(item.getType().equals(itemStack.getType())){
                if(item.getData().getData() == itemStack.getData().getData()){
                    return true;
                }
            }
        }
        return false;
    }

    private ItemStack getItem(ConcurrentHashMap<ItemStack, Integer> soldItemList, ItemStack itemStack) {
        for (ItemStack item : soldItemList.keySet()) {
            if(item.getType().equals(itemStack.getType())){
                if(item.getData().getData() == itemStack.getData().getData()){
                    return item;
                }
            }
        }
        return null;
    }

}