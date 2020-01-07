package net.squidstudios.mfhoppers.hopper.upgrades;

import de.tr7zw.changeme.nbtapi.NBTItem;
import me.clip.placeholderapi.PlaceholderAPI;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.util.*;
import net.squidstudios.mfhoppers.util.inv.InventoryBuilder;
import net.squidstudios.mfhoppers.util.item.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpgradeInventory {

    public static UpgradeInventory instance;
    private Map<String, Object> data = new HashMap<>();

    public UpgradeInventory(Map<String, Object> data) {

        this.data = data;
        instance = this;

    }

    public UpgradeInventory(MFHoppers MFHoppers) {
        instance = this;
    }

    public static UpgradeInventory getInstance() {
        return instance;
    }

    public Inventory build(IHopper hopper, Player player) {

        Map<String, Object> DATA = hopper.getData();
        Map<String, Object> CONFIG_DATA = MFHoppers.getInstance().configHoppers.get(DATA.get("name").toString()).getNextHopperUpgrade(hopper);

        ItemStack filler = GlassColor.valueOf(data.get("filler_color").toString()).getItem();
        ItemStack infoItem = new ItemBuilder(MMaterial.matchMaterial(data.get("infoItem.material").toString())).
                setLore(toLore(data.get("infoItem.lore")), true).
                replaceLore(new MapBuilder().add("%type%", DATA.get("type").toString()).add("%name%", DATA.get("name").toString()).add("%lvl%", DATA.get("lvl")).getMap(), player).
                setName(data.get("infoItem.name").toString()).
                addNbt("info", "info").
                addItemFlag(ItemFlag.HIDE_ATTRIBUTES).buildItem();
        MapBuilder replaceMap = new MapBuilder();
        replaceMap.add("%lvl%", DATA.get("lvl").toString());
        replaceMap.add("%nextlvl%", String.valueOf(Integer.valueOf(DATA.get("lvl").toString()) + 1));
        replaceMap.add("%price%", (int) CONFIG_DATA.get("price"));
        replaceMap.add("%pricetype%", CONFIG_DATA.containsKey("customPriceTypeName") ? CONFIG_DATA.get("customPriceTypeName").toString() : UpgradeEnum.valueOf(CONFIG_DATA.get("priceType").toString()).getUnderstandable());

        ItemStack upgradeItem = new ItemBuilder(MMaterial.matchMaterial(data.get("upgradeItem.material").toString())).
                setLore(toLore(data.get("upgradeItem.lore")), true).
                replaceLore(replaceMap.getMap(), player).
                setName(data.get("upgradeItem.name").toString().replace("%nextlvl%", String.valueOf(Integer.valueOf(DATA.get("lvl").toString()) + 1))).
                addNbt("upgrade", "up").
                addItemFlag(ItemFlag.HIDE_ATTRIBUTES).buildItem();

        return new InventoryBuilder(c(data.get("title")), 9).
                fill(filler).
                setItem(3, upgradeItem).
                setItem(5, infoItem).
                setClickListener(event -> {
                    if (event.getCurrentItem() != null) {

                        NBTItem nbt = new NBTItem(event.getCurrentItem());
                        if (nbt.hasKey("upgrade")) {

                            UpgradeEnum priceType = UpgradeEnum.valueOf(CONFIG_DATA.get("priceType").toString());
                            int price = (int) CONFIG_DATA.get("price");
                            Player p = (Player) event.getWhoClicked();

                            switch (priceType) {
                                case COMMAND:
                                    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
                                        MFHoppers.getInstance().getLogger().warning("Can't use COMMAND as Price Type, because PlaceholderAPI is missing!");
                                        return;
                                    }
                                    if (!PlaceholderAPI.containsPlaceholders(CONFIG_DATA.get("pricePlaceholderValue").toString())) {
                                        MFHoppers.getInstance().getLogger().warning(String.format("Can't find the placeholder %s!", CONFIG_DATA.get("pricePlaceholderValue").toString()));
                                        return;
                                    }

                                    String placeholder = CONFIG_DATA.get("pricePlaceholderValue").toString();
                                    String command = CONFIG_DATA.get("priceCommand").toString();
                                    double currentAmount = Double.valueOf(PlaceholderAPI.setPlaceholders(p, placeholder));

                                    if (currentAmount >= price) {
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(p, command.replace("{player}", p.getName()).replace("{price}", String.valueOf(price))));

                                        hopper.getData().replace("lvl", ((int) hopper.getData().get("lvl")) + 1);
                                        Lang.HOPPER_UPGRADED.send(new MapBuilder().add("%lastlvl%", String.valueOf(((int) hopper.getData().get("lvl")) - 1)).add("%newlvl%", hopper.getData().get("lvl")).getMap(), p);
                                        event.getWhoClicked().closeInventory();

                                        // Update Hopper
                                        DataManager.getInstance().update(hopper);
                                    } else {
                                        SendNotEhoughMessage(p, CONFIG_DATA, priceType.getUnderstandable(), String.valueOf(price - currentAmount), String.valueOf(currentAmount), String.valueOf(price));
                                    }
                                    break;
                                case XP:
                                    int currentXp = ExperienceManager.getTotalExperience(p);
                                    if (currentXp >= price) {

                                        hopper.getData().replace("lvl", ((int) hopper.getData().get("lvl")) + 1);

                                        ExperienceManager.setTotalExperience(p, currentXp - price);
                                        Lang.HOPPER_UPGRADED.send(new MapBuilder().add("%lastlvl%", String.valueOf(((int) hopper.getData().get("lvl")) - 1)).add("%newlvl%", hopper.getData().get("lvl")).getMap(), p);
                                        event.getWhoClicked().closeInventory();

                                        // Update Hopper
                                        DataManager.getInstance().getUpdatedHopperQueue().add(hopper);
                                    } else {
                                        SendNotEhoughMessage(p, CONFIG_DATA, priceType.getUnderstandable(), String.valueOf(price - currentXp), String.valueOf(currentXp), String.valueOf(price));
                                    }
                                    break;
                                case ECO:
                                    if (MFHoppers.getInstance().getEconomy() == null) {
                                        event.getWhoClicked().sendMessage(c("&c&l(!)&7 Economy is disabled, failed to convert."));

                                    } else {
                                        double current = MFHoppers.getInstance().getEconomy().getBalance(p);
                                        if (current >= price) {

                                            hopper.getData().replace("lvl", ((int) hopper.getData().get("lvl")) + 1);
                                            MFHoppers.getInstance().getEconomy().withdrawPlayer(p, price);
                                            Lang.HOPPER_UPGRADED.send(new MapBuilder().add("%lastlvl%", String.valueOf(((int) hopper.getData().get("lvl")) - 1)).add("%newlvl%", hopper.getData().get("lvl")).getMap(), p);
                                            event.getWhoClicked().closeInventory();

                                            // Update Hopper
                                            DataManager.getInstance().update(hopper);
                                        } else {
                                            SendNotEhoughMessage(p, CONFIG_DATA, priceType.getUnderstandable(), String.valueOf(price - current), String.valueOf(current), String.valueOf(price));
                                        }
                                    }
                                    break;
                            }
                        }
                        event.setCancelled(true);

                    }

                }).buildInventory();

    }

    String c(Object text) {
        return ChatColor.translateAlternateColorCodes('&', text.toString());
    }

    List<String> toLore(Object obj) {
        return (List<String>) obj;
    }

    private void SendNotEhoughMessage(Player player, Map<String, Object> config, String type, String missing, String current, String needed) {
        if (config.containsKey("customNotEnoughMessage")) {
            String message = config.get("customNotEnoughMessage").toString();
            message = message.replace("{type}", type);
            message = message.replace("{missing}", missing);
            message = message.replace("{current}", current);
            message = message.replace("{needed}", needed);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        } else {
            Lang.HOPPER_NOT_ENOUGH_VALUE_TO_UPGRADE.send(new MapBuilder().add("%type%", type).add("%missing%", missing).add("%current%", current).add("%needed%", needed).getMap(), player);
        }
    }
}
