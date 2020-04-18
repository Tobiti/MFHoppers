package net.squidstudios.mfhoppers.hopper;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.upgrades.Upgrade;
import net.squidstudios.mfhoppers.util.Drop;
import net.squidstudios.mfhoppers.util.Methods;
import net.squidstudios.mfhoppers.util.item.ItemBuilder;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigHopper {

    /*
    Data that contains in hashmap: name, lore, limitPe    if HopperEnum is Grind:rChunk, should_drop_from_explosions

     */
    public static class BreakDropsElement {
        public Material Material;
        public boolean HasDamageValue = false;
        public short DamageValue = -1;
        public Drop Drop;

        public BreakDropsElement(Material material, boolean hasDamageValue, short damageValue, Drop drop) {
            this.DamageValue = damageValue;
            this.HasDamageValue = hasDamageValue;
            this.Material = material;
            this.Drop = drop;
        }
    }

    private String hopperName;
    private Map<String, Object> data = new HashMap<>();
    private Map<Integer, Upgrade> upgrades = new HashMap<>();
    private HopperEnum type;


    public ConfigHopper(Map<String, Object> data, MFHoppers pl, String hopperName) {
        this.data = data;
        this.data.put("name0", hopperName);
        this.type = HopperEnum.match(data.get("type").toString());

        if (type == null) {
            pl.out(" !-> Failed to init config hopper named: " + hopperName + ", maybe there's options missing?", PluginBuilder.OutType.ERROR_NOPREFIX);

        }
        if (!Methods.checkIfAllKeysExists(type, data)) {

            pl.out(" !-> Failed to init config hopper named: &c" + hopperName + "&4, maybe there's options missing?", PluginBuilder.OutType.ERROR_NOPREFIX);
            pl.out(" &4!-> Options required: &c" + type.getReq().toString().toCharArray().toString().replace("[", "").replace("]", ""), PluginBuilder.OutType.WITHOUT_PREFIX);

        } else {

            Object _chunkLimit = data.get("limitPerChunk");
            Object _name = data.get("name");
            Object _lore = data.get("lore");
            Object _should_drop_from_explosions = data.get("should_drop_from_explosions");

            if (!_chunkLimit.getClass().getTypeName().equalsIgnoreCase(Integer.class.getName())) {

                pl.out(" !-> Can't find right value in key: &c" + "limitPerChunk" + "&4 config hopper named: &c" + hopperName + "&4, value type should be: &cInteger", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;

            }
            if (!_should_drop_from_explosions.getClass().getTypeName().equalsIgnoreCase(Boolean.class.getName())) {

                pl.out(" !-> Can't find right value in key: &c" + "should_drop_from_explosions" + "&4 hopper named: &c" + hopperName + "&4, value type should be:&c Boolean", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;

            }
            if (!_lore.getClass().getTypeName().equalsIgnoreCase(ArrayList.class.getTypeName())) {

                pl.out(" !-> Can't find right value in key: &c" + "lore" + "&4 hopper named: &c" + hopperName + "&4, value type should be: &cList<String>", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;

            }
            if (type == HopperEnum.Mob) {


                if (!getData().get("drops").getClass().getTypeName().equalsIgnoreCase(ArrayList.class.getTypeName())) {

                    pl.out(" !-> Can't find right value in key: &c" + "drops" + "&4 hopper named: &c" + hopperName + "&4, value type should be:&c List<String>");
                    return;

                }

            } else if (type == HopperEnum.Crop) {
                if (!getData().get("crops").getClass().getTypeName().equalsIgnoreCase(ArrayList.class.getTypeName())) {

                    pl.out(" !-> Can't find right value in key:&c " + "crops" + "&4 hopper named: &c" + hopperName + "&4, value type should be:&c List<String>");
                    return;

                }

            }
        }
        this.hopperName = hopperName;
        pl.configHoppers.put(hopperName, this);
        new Upgrade(data, null, this, 1, pl, true);
    }

    public ConfigHopper(Map<String, Object> data, MFHoppers pl, String hopperName, Map<Integer, Map<String, Object>> upgr) {
        this.data = data;
        this.data.put("name0", hopperName);

        this.type = HopperEnum.match(data.get("type").toString());

        if (type == null) {

            pl.out(" !-> Failed to init config hopper named: " + hopperName + ", maybe there's options missing?", PluginBuilder.OutType.ERROR_NOPREFIX);

        }
        if (!Methods.checkIfAllKeysExists(type, data)) {

            pl.out(" !-> Failed to init config hopper named: &c" + hopperName + "&4, maybe there's options missing?", PluginBuilder.OutType.ERROR_NOPREFIX);
            pl.out(" &4!-> Options required: &c" + type.getReq().toString().replace("[", "").replace("]", ""), PluginBuilder.OutType.WITHOUT_PREFIX);

        } else {

            Object _chunkLimit = data.get("limitPerChunk");
            Object _name = data.get("name");
            Object _lore = data.get("lore");
            Object _should_drop_from_explosions = data.get("should_drop_from_explosions");

            if (!_chunkLimit.getClass().getTypeName().equalsIgnoreCase(Integer.class.getName())) {

                pl.out(" !-> Can't find right value in key: &c" + "limitPerChunk" + "&4 config hopper named: &c" + hopperName + "&4, value type should be: &cInteger", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;

            }
            if (!_should_drop_from_explosions.getClass().getTypeName().equalsIgnoreCase(Boolean.class.getName())) {

                pl.out(" !-> Can't find right value in key: &c" + "should_drop_from_explosions" + "&4 hopper named: &c" + hopperName + "&4, value type should be:&c Boolean", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;

            }
            if (!_lore.getClass().getTypeName().equalsIgnoreCase(ArrayList.class.getTypeName())) {

                pl.out(" !-> Can't find right value in key: &c" + "lore" + "&4 hopper named: &c" + hopperName + "&4, value type should be: &cList<String>", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;

            }
            if (type == HopperEnum.Mob) {


                if (!getData().get("drops").getClass().getTypeName().equalsIgnoreCase(ArrayList.class.getTypeName())) {

                    pl.out(" !-> Can't find right value in key: &c" + "drops" + "&4 hopper named: &c" + hopperName + "&4, value type should be:&c List<String>");
                    return;

                }

            } else if (type == HopperEnum.Crop) {
                if (!getData().get("crops").getClass().getTypeName().equalsIgnoreCase(ArrayList.class.getTypeName())) {

                    pl.out(" !-> Can't find right value in key:&c " + "crops" + "&4 hopper named: &c" + hopperName + "&4, value type should be:&c List<String>");
                    return;

                }
            }


        }
        this.hopperName = hopperName;
        pl.configHoppers.put(hopperName, this);
        new Upgrade(data, null, this, 1, pl, true);
        for (int level : upgr.keySet()) {
            new Upgrade(upgr.get(level), upgrades.get(level - 1).getToUpgrade(), this, level, pl, false);
        }
    }


    public Map<String, Object> getData() {
        return data;
    }

    public Map<Integer, Upgrade> getUpgrades() {
        return upgrades;
    }

    public HopperEnum getType() {
        return type;
    }

    public Map<String, Object> getDataOfHopper(IHopper hopper) {
        int upgradeLevel = hopper.getLevel();
        return upgrades.get(upgradeLevel).getToUpgrade();
    }

    public BreakDropsElement GetBreakDropELement(IHopper hopper, Material mat, short damage) {
        for (BreakDropsElement elem : (List<BreakDropsElement>) getDataOfHopper(hopper).get("drops")) {
            if (elem.Material == mat) {
                if (!elem.HasDamageValue) {
                    return elem;
                } else {
                    if (elem.DamageValue == damage) {
                        return elem;
                    }
                }
            }
        }
        return null;
    }

    public ItemStack getItem() {

        if (type == HopperEnum.Grind) {

            boolean isAuto = data.get("isAuto") != null ? Boolean.valueOf(data.get("isAuto").toString()) : false;
            boolean isGlobal = data.get("isGlobal") != null ? Boolean.valueOf(data.get("isGlobal").toString()) : false;


            return new ItemBuilder(Material.HOPPER)
                    .setName(c(data.get("name").toString().replace("%type%", StringUtils.capitalize(EntityType.valueOf(data.get("mob").toString()).name().replace("_", " ").toLowerCase()))))
                    .addNbt("type", type.name())
                    .addNbt("name0", data.get("name0").toString())
                    .addNbt("lvl", 1)
                    .addNbt("isGlobal", isGlobal)
                    .addNbt("isAuto", isAuto)
                    .addNbt("ent", EntityType.valueOf(data.get("mob").toString().toUpperCase()))
                    .setLore(data.containsKey("lore") ? (List<String>) data.get("lore") : new ArrayList<>(), true)
                    .replaceInLore("%type%", StringUtils.capitalize(EntityType.valueOf(data.get("mob").toString()).name().replace("_", " ").toLowerCase()))
                    .buildItem();
        } else {
            return new ItemBuilder(Material.HOPPER)
                    .setName(c(data.get("name").toString()))
                    .addNbt("type", type.name())
                    .addNbt("name0", data.get("name0").toString())
                    .addNbt("lvl", 1)
                    .setLore(data.containsKey("lore") ? (List<String>) data.get("lore") : new ArrayList<>(), true)
                    .buildItem();
        }

    }

    public ItemStack getItem(boolean isAuto, boolean isGlobal) {

        if (type == HopperEnum.Grind) {

            return new ItemBuilder(Material.HOPPER)
                    .setName(c(data.get("name").toString().replace("%type%", StringUtils.capitalize(EntityType.valueOf(data.get("mob").toString()).name().replace("_", " ").toLowerCase()))))
                    .addNbt("type", type.name())
                    .addNbt("name0", data.get("name0").toString())
                    .addNbt("lvl", 1)
                    .addNbt("isGlobal", isGlobal)
                    .addNbt("isAuto", isAuto)
                    .addNbt("ent", EntityType.valueOf(data.get("mob").toString().toUpperCase()))
                    .setLore(data.containsKey("lore") ? (List<String>) data.get("lore") : new ArrayList<>(), true)
                    .replaceInLore("%type%", StringUtils.capitalize(EntityType.valueOf(data.get("mob").toString()).name().replace("_", " ").toLowerCase()))
                    .buildItem();
        }
        return null;
    }

    String c(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public boolean isUpgradable() {
        if (upgrades.size() <= 1) {
            return false;
        } else {
            return true;
        }
    }

    public boolean isEditableFilter() {
        if (getData().containsKey("Editable_Filter")) {
            return Boolean.valueOf(getData().get("Editable_Filter").toString());
        } else {
            return false;
        }
    }
    
    public boolean onlyActiveWhenOwnerOnline() {
        if (getData().containsKey("onlyActiveWhenOwnerIsOnline")) {
            return Boolean.valueOf(getData().get("onlyActiveWhenOwnerIsOnline").toString());
        } else {
            return false;
        }
    }

    public boolean isBreakAll() {
        if (getData().containsKey("breakAll")) {
            return Boolean.valueOf(getData().get("breakAll").toString());
        } else {
            return false;
        }
    }

    public ItemStack getItemOfData(IHopper hopper) {

        int level = (int) hopper.getData().get("lvl");
        Map<String, Object> upgradeData = upgrades.get(level).getToUpgrade();
        if (type == HopperEnum.Grind) {

            List<String> lore = (List<String>) upgradeData.get("lore");
            String name = upgradeData.get("name").toString();
            boolean isAuto = Boolean.valueOf(hopper.getData().get("isAuto").toString());
            boolean isGlobal = Boolean.valueOf(hopper.getData().get("isGlobal").toString());
            String ent = hopper.getData().get("ent").toString();
            name = name.replace("%type%", StringUtils.capitalize(ent.replace("_", " ").toLowerCase()));
            String name0 = hopper.getName();
            return new ItemBuilder(Material.HOPPER)
                    .setName(c(name))
                    .addNbt("type", type.name())
                    .addNbt("name0", name0)
                    .addNbt("lvl", level)
                    .addNbt("isAuto", isAuto).
                            addNbt("isGlobal", isGlobal).
                            addNbt("ent", ent)
                    .setLore(lore, true)
                    .replaceInLore("%type%", StringUtils.capitalize(ent.replace("_", " ").toLowerCase()))
                    .buildItem();
        } else {

            List<String> lore = (List<String>) upgradeData.get("lore");
            String name = upgradeData.get("name").toString();
            String name0 = hopper.getName();
            return new ItemBuilder(Material.HOPPER)
                    .setName(c(name))
                    .setLore(lore, true)
                    .addNbt("lvl", level)
                    .addNbt("type", type.name())
                    .addNbt("name0", name0)
                    .buildItem();
        }
    }

    public Map<String, Object> getNextHopperUpgrade(IHopper hopper) {
        int upgradeLevel = (int) hopper.getData().get("lvl");
        upgradeLevel++;
        Upgrade upgrade = upgrades.get(upgradeLevel);
        if (upgrade == null) {
            return null;
        } else {
            return upgrade.getToUpgrade();
        }
    }

    public ItemStack buildItemByLevel(int level) {
        Map<String, Object> upgradeData = upgrades.get(level).getToUpgrade();
        if (type == HopperEnum.Grind) {

            List<String> lore = (List<String>) upgradeData.get("lore");
            String name = upgradeData.get("name").toString();
            name = name.replace("%type%", StringUtils.capitalize(EntityType.valueOf(upgradeData.get("mob").toString()).name().replace("_", " ").toLowerCase()));
            boolean isAuto = false;
            boolean isGlobal = false;
            return new ItemBuilder(Material.HOPPER)
                    .setName(c(name))
                    .addNbt("type", type.name())
                    .addNbt("name0", hopperName)
                    .addNbt("lvl", level)
                    .addNbt("isAuto", isAuto)
                    .addNbt("isGlobal", isGlobal)
                    .addNbt("ent", EntityType.valueOf(upgradeData.get("mob").toString()))
                    .setLore(lore, true)
                    .replaceInLore("%type%", StringUtils.capitalize(EntityType.valueOf(upgradeData.get("mob").toString()).name().replace("_", " ").toLowerCase()))
                    .buildItem();
        } else {

            List<String> lore = (List<String>) upgradeData.get("lore");
            String name = upgradeData.get("name").toString();
            return new ItemBuilder(Material.HOPPER)
                    .setName(c(name))
                    .setLore(lore, true)
                    .addNbt("lvl", level)
                    .addNbt("type", type.name())
                    .addNbt("name0", hopperName)
                    .buildItem();
        }
    }

    public ItemStack buildItemByLevel(int level, EntityType ent, boolean isAuto, boolean isGlobal) {


        Map<String, Object> upgradeData = upgrades.get(level).getToUpgrade();
        if (type == HopperEnum.Grind) {

            List<String> lore = (List<String>) upgradeData.get("lore");
            String name = upgradeData.get("name").toString();
            name = name.replace("%type%", StringUtils.capitalize(ent.name().replace("_", " ").toLowerCase()));
            return new ItemBuilder(Material.HOPPER)
                    .setName(c(name))
                    .addNbt("type", type.name())
                    .addNbt("name0", hopperName)
                    .addNbt("lvl", level)
                    .addNbt("isAuto", isAuto)
                    .addNbt("isGlobal", isGlobal)
                    .addNbt("ent", ent.name())
                    .setLore(lore, true)
                    .replaceInLore("%type%", StringUtils.capitalize(ent.name().replace("_", " ").toLowerCase()))
                    .buildItem();
        } else {

            List<String> lore = (List<String>) upgradeData.get("lore");
            String name = upgradeData.get("name").toString();
            return new ItemBuilder(Material.HOPPER)
                    .setName(c(name))
                    .setLore(lore, true)
                    .addNbt("lvl", level)
                    .addNbt("type", type.name())
                    .addNbt("name0", hopperName)
                    .buildItem();
        }
    }

    public String getTitle(IHopper hopper) {

        int lvl = (int) hopper.getData().get("lvl");

        if (getUpgrades().get(lvl).getToUpgrade().containsKey("inventoryTitle"))
            return getUpgrades().get(lvl).getToUpgrade().get("inventoryTitle").toString();
        else return getItemOfData(hopper).getItemMeta().getDisplayName();

    }

	public boolean allowNamedMobs() {
        if(getData().containsKey("allowNamedMobs")){
            try{
            return (boolean) getData().get("allowNamedMobs");
            } catch (Exception ignore) {}
        }
		return false;
	}

	public boolean isSupportingSSBMissions() {
		return Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2") && getData().containsKey("supportSSBMissions") && Boolean.valueOf(getData().get("supportSSBMissions").toString());
	}

	public boolean isLinkedInstantMove() {
        if(getData().containsKey("linkedInstantMove")){
            try{
                return (boolean) getData().get("linkedInstantMove");
            } catch (Exception ignore) {}
        }
        return true;
    }
    
	public boolean filterIsBlacklist() {
        if(getData().containsKey("filterIsBlacklist")){
            try{
            return (boolean) getData().get("filterIsBlacklist");
            } catch (Exception ignore) {}
        }
		return false;
	}
}
