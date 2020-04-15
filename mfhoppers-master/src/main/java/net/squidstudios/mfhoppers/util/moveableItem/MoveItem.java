package net.squidstudios.mfhoppers.util.moveableItem;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import net.squidstudios.mfhoppers.manager.HookManager;
import net.squidstudios.mfhoppers.util.Methods;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MoveItem {

    private boolean reachedMaxCount = false;
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

    public MoveItem(Item parent, List<ItemStack> itemStackList, int amount) {
        if (amount > 64) setStacked(true);
        setEntity(parent);

        items = itemStackList;
        this.amount = amount;
    }

    public MoveItem(Item parent, List<ItemStack> itemStackList, int amount, boolean max) {
        if (amount > 64) setStacked(true);
        setEntity(parent);

        items = itemStackList;
        this.amount = amount;
        this.reachedMaxCount = max;
    }

    public boolean GetMaxReached(){
        return reachedMaxCount;
    }

    public static MoveItem getFrom(Item item) {
        if (HookManager.getInstance().isWildStackerHooked()) {

            List<ItemStack> items = new ArrayList<>();
            int amount = WildStackerAPI.getStackedItem(item).getStackAmount();

            //MFHoppers.getInstance().getLogger().info(String.format("Add Wildstacker Item %s Amount: %d", item.getItemStack().getType().toString(), amount));
            boolean max = add(items, amount, item.getItemStack());
            return new MoveItem(item, items, amount, max);

        } else {
            List<ItemStack> items = new ArrayList<>();
            items.add(item.getItemStack());
            return new MoveItem(item, items, item.getItemStack().getAmount());
        }
    }

    static boolean add(List<ItemStack> items, int amount, ItemStack parent) {
        boolean max = add(items, amount, parent, amount/64);

        int modulo = amount % 64;
        if(modulo > 0){
            ItemStack clone = parent.clone();
            clone.setAmount(amount % 64);
            items.add(clone);
        }

        return max;
    }

    static boolean add(List<ItemStack> items, int amount, ItemStack parent, int count) {
        boolean maxReached = false;
        if(count > 20){
            maxReached = true;
            count = 20;
        }
        for(int i = 0; i < count; i++){
            ItemStack clone = parent.clone();
            clone.setAmount(64);
            items.add(clone);
        }
        return maxReached;
    }

    public void setAmount(int amount) {
        this.amount = amount;

        if (amount <= 0) {
            Methods.forceSync(() -> getEntity().remove());
        }

        if (HookManager.getInstance().isWildStackerHooked() && WildStackerAPI.getStackedItem(getEntity()) != null)
            WildStackerAPI.getStackedItem(getEntity()).setStackAmount(amount, true);
    }

    public int getAmount() {
        return amount;
    }
}
