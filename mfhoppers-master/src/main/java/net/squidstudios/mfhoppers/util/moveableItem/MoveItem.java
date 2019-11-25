package net.squidstudios.mfhoppers.util.moveableItem;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import net.squidstudios.mfhoppers.manager.HookManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoveItem {

    private boolean isStacked = false;

    public List<ItemStack> getItems() {
        return items;
    }

    private List<ItemStack> items;
    private Item entity;
    private int amount;

    public void setEntity(Item entity) {
        this.entity = entity;
    }

    public void setStacked(boolean stacked) {
        isStacked = stacked;
    }

    public Item getEntity() {
        return entity;
    }

    public boolean isStacked() {
        return isStacked;
    }

    public MoveItem(Item parent, List<ItemStack> itemStackList, int amount){

        if(amount > 64) setStacked(true);
        setEntity(parent);
        items = itemStackList;
        this.amount = amount;

    }

    public static MoveItem getFrom(Item item){

        if(HookManager.getInstance().isWildStackerHooked()){

            List<ItemStack> items = new ArrayList<>();
            int amount = WildStackerAPI.getStackedItem(item).getStackAmount();

            add(items, amount, item);
            return new MoveItem(item, items, amount);

        } else {
                List<ItemStack> items = new ArrayList<>();
                items.add(item.getItemStack());
                return new MoveItem(item, items, item.getItemStack().getAmount());
        }

    }

    static void add(List<ItemStack> items, int amount, Item parent) {
        int currentNumber = amount <= 64 ? amount : 64;

        ItemStack clone = parent.getItemStack().clone();
        clone.setAmount(currentNumber);

        items.add(clone);

        amount -= currentNumber;
        if (amount > 0)
            add(items,amount,parent);
    }

    public void setAmount(int amount){

        this.amount = amount;

        if(amount <= 0){
            getEntity().remove();
        }

        if(HookManager.getInstance().isWildStackerHooked() && WildStackerAPI.getStackedItem(getEntity()) != null) WildStackerAPI.getStackedItem(getEntity()).setStackAmount(amount, true);
    }

    public int getAmount() {
        return amount;
    }
}
