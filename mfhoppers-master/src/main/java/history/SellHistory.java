package history;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;

import lombok.Getter;

@Getter
public class SellHistory{

    public class SellHistoryEntry{
        public int Amount = 0;
        public double Price = 0;

        public SellHistoryEntry(int amount, double price){
            this.Amount = amount;
            this.Price = price;
        }
    }

    private ConcurrentHashMap<ItemStack, SellHistoryEntry> soldItems = new ConcurrentHashMap<ItemStack, SellHistoryEntry>();

    public void AddSoldItem(ItemStack itemStack, int amount, double price){

        if(containsKey(soldItems, itemStack)){
            ItemStack item = getItem(soldItems, itemStack);
            soldItems.get(item).Amount = soldItems.get(item).Amount +amount;
            soldItems.get(item).Price = soldItems.get(item).Price + price;
        }
        else {
            ItemStack item = itemStack.clone();
            item.setAmount(1);
            soldItems.put(item, new SellHistoryEntry(amount, price));
        }
    }

    private boolean containsKey(ConcurrentHashMap<ItemStack, SellHistoryEntry> soldItemList, ItemStack itemStack) {
        for (ItemStack item : soldItemList.keySet()) {
            if(item.getType().equals(itemStack.getType())){
                if(item.getData().getData() == itemStack.getData().getData()){
                    return true;
                }
            }
        }
        return false;
    }

    private ItemStack getItem(ConcurrentHashMap<ItemStack, SellHistoryEntry> soldItemList, ItemStack itemStack) {
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