package net.squidstudios.mfhoppers.util.moveableItem;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import net.squidstudios.mfhoppers.manager.HookManager;
import net.squidstudios.mfhoppers.util.Methods;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

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

    public MoveItem(Item parent, List<ItemStack> itemStackList, int amount) {
        if (amount > 64) setStacked(true);
        setEntity(parent);

        items = itemStackList;
        this.amount = amount;
    }

    public static MoveItem getFrom(Item item) {
        if (HookManager.getInstance().isWildStackerHooked()) {

            List<ItemStack> items = new ArrayList<>();
            int amount = WildStackerAPI.getStackedItem(item).getStackAmount();

            //MFHoppers.getInstance().getLogger().info(String.format("Add Wildstacker Item %s Amount: %d", item.getItemStack().getType().toString(), amount));
            add(items, amount, item);
            return new MoveItem(item, items, amount);

        } else {
            List<ItemStack> items = new ArrayList<>();
            items.add(item.getItemStack());
            return new MoveItem(item, items, item.getItemStack().getAmount());
        }
    }

    static void add(List<ItemStack> items, int amount, Item parent) {
        add(items, amount, parent, 0);
    }

    static void add(List<ItemStack> items, int amount, Item parent, int count) {
        if (count > 20)
            return;

        int currentNumber = amount <= 64 ? amount : 64;

        ItemStack clone = parent.getItemStack().clone();
        clone.setAmount(currentNumber);

        items.add(clone);

        amount -= currentNumber;
        if (amount > 0)
            add(items, amount, parent, count + 1);
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
