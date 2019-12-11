package net.squidstudios.mfhoppers.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.util.item.nbt.NBTEntity;
import net.squidstudios.mfhoppers.util.item.nbt.NBTItem;
import net.squidstudios.mfhoppers.util.moveableItem.MoveItem;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import net.squidstudios.mfhoppers.hopper.ConfigHopper;
import net.squidstudios.mfhoppers.hopper.IHopper;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;

import static java.util.stream.Collectors.toList;

public class Methods {

    private static MFHoppers pl;

    public Methods(MFHoppers pl) {
        this.pl = pl;
    }

    public static HashMap<Location, IHopper> getSorted(HopperEnum henum, MChunk ch) {

        HashMap<Location, IHopper> ret = new HashMap<>();
        if (ch == null || !DataManager.getInstance().getHoppers().containsKey(ch) || DataManager.getInstance().getHoppers().get(ch) == null) {
            return ret;
        }
        final Map<Location, IHopper> hoppers = DataManager.getInstance().getHoppers().get(ch);
        for (Location lo : hoppers.keySet()) {

            if (HopperEnum.valueOf(hoppers.get(lo).getData().get("type").toString()) == henum) {
                ret.put(lo, hoppers.get(lo));
            }

        }
        return ret;

    }

    public static HashMap<Location, IHopper> getSorted(HopperEnum henum, Chunk ch, Material mat, short data) {

        HashMap<Location, IHopper> ret = new HashMap<>();
        if (DataManager.getInstance().getHoppers().isEmpty() && !DataManager.getInstance().containsHoppersChunk(ch)) {
            return ret;
        }
        final Map<Location, IHopper> hoppers = DataManager.getInstance().getHoppers(ch);

        for (Location lo : hoppers.keySet()) {

            IHopper hopper = hoppers.get(lo);

            if (hopper == null) continue;

            if (hopper.getType() == henum) {


                if (hopper.ContainsInFilterMaterialList(mat, data)) {

                    ret.put(lo, hoppers.get(lo));

                }
            }

        }
        return ret;

    }

    public static boolean checkIfAllKeysExists(HopperEnum hopperEnum, Map<String, Object> data) {

        List<String> req = Arrays.asList(hopperEnum.getReq());
        boolean ret = true;

        for (String key : req) {

            if (!data.keySet().contains(key)) {
                pl.out(" &c!-> Can't find required config option named: &4" + key, PluginBuilder.OutType.WITHOUT_PREFIX);
                ret = false;
            }


        }
        return ret;

    }

    public static List<MoveItem> addItem(List<MoveItem> items, Collection<IHopper> blocks) {

        if (Thread.currentThread().getName().equalsIgnoreCase("Server thread")) {
            for (IHopper hopper : blocks) {

                for (MoveItem moveItem : items.stream().filter(item -> hopper.ContainsInFilterMaterialList(item.getEntity().getItemStack().getType(), item.getEntity().getItemStack().getDurability())).collect(toList())) {

                    if (moveItem.getAmount() <= 0) continue;

                    ItemStack clone = moveItem.getItems().stream().findFirst().orElse(null);

                    if (hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("pickupNamedItems") && !(boolean) hopper.getConfigHopper().getDataOfHopper(hopper).get("pickupNamedItems") && clone.hasItemMeta() && clone.getItemMeta().hasDisplayName())
                        continue;

                    if (hopper.getLocation().getBlock().getType() == Material.AIR) continue;

                    int amount = moveItem.getAmount();
                    int added = addItem2(moveItem.getItems(), hopper);

                    moveItem.setAmount(amount - added);

                }
            }
        } else {
            for (IHopper hopper : blocks) {

                for (MoveItem moveItem : items.stream().filter(item -> hopper.ContainsInFilterMaterialList(item.getEntity().getItemStack().getType(), item.getEntity().getItemStack().getDurability())).collect(toList())) {

                    if (moveItem.getAmount() <= 0) continue;

                    ItemStack clone = moveItem.getItems().stream().findFirst().orElse(null);

                    if (hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("pickupNamedItems") && !(boolean) hopper.getConfigHopper().getDataOfHopper(hopper).get("pickupNamedItems") && clone.hasItemMeta() && clone.getItemMeta().hasDisplayName())
                        continue;

                    if (materialEqualsTo(hopper.getLocation(), Material.AIR)) continue;

                    int amount = moveItem.getAmount();
                    int added = addItem2(moveItem.getItems(), hopper);

                    moveItem.setAmount(amount - added);

                }
            }
        }

        return items;
    }

    public static List<Inventory> GetLinkedInventorys(IHopper hopper) {
        List<Inventory> inventories = new ArrayList<>();
        if (!hopper.isLinked()) {
            return inventories;
        }
        for (Location location : hopper.getLinked()) {

            try {
                if (MContainer.isContainer(location)) {
                    inventories.add(MContainer.getOfLocation(location).getInventory(location));
                } else {
                    hopper.unlink(location);
                }
            } catch (IllegalStateException ex) {
                hopper.unlink(location);
            }
        }
        return inventories;
    }

    public static int addItem2(List<ItemStack> items, IHopper hopper) {

        int added = 0;

        for (ItemStack item : items) {

            Inventory inv = hopper.getInventory();
            if (inv == null) return added;

            if (hopper.isLinked()) {
                boolean itemWasAdded = false;
                for (Inventory destination : Methods.GetLinkedInventorys(hopper)) {
                    if (Methods.canFit(item, item.getAmount(), destination)) {
                        added += item.getAmount();
                        destination.addItem(item);
                        itemWasAdded = true;
                        break;
                    }
                }
                if (itemWasAdded) {
                    continue;
                }
            }


            if (inv.firstEmpty() != -1) {

                added += item.getAmount();

                inv.addItem(item);
                continue;
            }

            if (!canFit(item, item.getAmount(), inv)) return added;
            HashMap left = inv.addItem(item);
            if (!left.isEmpty()) {

                int a = item.getAmount() - (Integer) left.keySet().toArray()[0];

                if (item.getAmount() != (Integer) left.keySet().toArray()[0]) {
                    added += a;
                }

                item.setAmount(a);

            } else {

                added += item.getAmount();

            }

        }

        return added;

    }


    public static Location toLocation(String s) {

        if (!s.contains(";") || s.contains("[") || s.contains("]")) {
            return null;
        }
        s = s.replace("]", "").replace("[", "");

        String splitted[] = s.split(";");

        return new Location(Bukkit.getWorld(splitted[0]), Double.valueOf(splitted[1]), Double.valueOf(splitted[2]), Double.valueOf(splitted[3]));
    }

    public static String worldName(String s) {

        String splitted[] = s.split(";");
        return splitted[0];

    }

    public static boolean isHopper(ItemStack item) {


        NBTItem nbt = new NBTItem(item);

        if (nbt.hasKey("lvl") && nbt.hasKey("type")) {
            return true;
        } else {
            return false;
        }

    }

    public static List<IHopper> getHopperByType(HopperEnum e) {


        List<IHopper> _nonHoppers = new ArrayList<>();
        List<IHopper> hoppers = new ArrayList<>();
        DataManager.getInstance().getHoppers().values().forEach(locationIHopperMap -> _nonHoppers.addAll(locationIHopperMap.values()));

        for (IHopper hopper : _nonHoppers) {

            if (hopper.getType() == e) {

                hoppers.add(hopper);

            }

        }
        return hoppers;

    }

    public static Map<Chunk, List<IHopper>> getMapHopperByType(HopperEnum... e) {

        List<IHopper> _nonHoppers = new ArrayList<>();
        Map<Chunk, List<IHopper>> hoppers = new HashMap<>();
        DataManager.getInstance().getHoppers().values().forEach(locationIHopperMap -> _nonHoppers.addAll(locationIHopperMap.values()));

        List<HopperEnum> types = Arrays.asList(e);

        for (IHopper hopper : _nonHoppers) {
            if (hopper == null) {
                continue;
            }
            try {
                if (types.contains(hopper.getType())) {
                    if (hopper.getChunk() != null) {
                        if (hoppers.containsKey(hopper.getChunk()) && hoppers.get(hopper.getChunk()) != null) {
                            hoppers.get(hopper.getChunk()).add(hopper);
                        } else {
                            if (hoppers.get(hopper.getChunk()) == null) {
                                hoppers.remove(hopper.getChunk());
                            }
                            List<IHopper> list = new ArrayList<>();
                            list.add(hopper);
                            hoppers.put(hopper.getChunk(), list);
                        }
                    }
                }
            } catch (NullPointerException n) {
                DataManager.getInstance().remove(hopper);
            }
        }
        return hoppers;
    }

    public static List<IHopper> getHopperByData(List<String> toCompare) {

        List<IHopper> _nonHoppers = new ArrayList<>();
        List<IHopper> hoppers = new ArrayList<>();
        DataManager.getInstance().getHoppers().values().forEach(locationIHopperMap -> _nonHoppers.addAll(locationIHopperMap.values()));
        for (IHopper hopper : _nonHoppers) {
            int keysNotFound = 0;

            for (String key : toCompare) {
                if (!hopper.getData().containsKey(key)) {
                    keysNotFound++;
                }
            }
            if (keysNotFound == 0) {
                hoppers.add(hopper);
            }

            hoppers.add(hopper);


        }
        return hoppers;


    }


    public static List<LivingEntity> getSortedEntities(ArrayList<Entity> entityList, List<EntityType> blacklist) {

        CompletableFuture<List<LivingEntity>> ent = new CompletableFuture<>();

        new BukkitRunnable() {

            List<LivingEntity> entities = new ArrayList<>();

            @Override
            public void run() {
                for (Entity entity : entityList) {
                    if (entity == null) {
                        continue;
                    }

                    if (entity.getType() != EntityType.PLAYER && entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.DROPPED_ITEM && entity.getType().isAlive() && (blacklist == null || !blacklist.contains(entity.getType()))) {
                        entities.add((LivingEntity) entity);
                    }
                }
                ent.complete(entities);
            }
        }.runTask(pl);


        try {
            return ent.get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new ArrayList<>();
    }

    public static List<Entity> getItems(ArrayList<Entity> entityList) {

        List<Entity> entities = new ArrayList<>();
        for (Entity entity : entityList) {

            if (entity.getType() == EntityType.DROPPED_ITEM) {
                entities.add(entity);
            }

        }
        return entities;
    }

    public static void addSlownessAndTeleport(LivingEntity ent, Location loc) {

        new BukkitRunnable() {
            @Override
            public void run() {
                NBTEntity nbt = new NBTEntity(ent);
                if (nbt.getByte("NoAI") == 1 && ent.getType() != EntityType.ENDERMAN) {
                    return;
                }
                PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 999999999, 60);
                ent.addPotionEffect(effect, false);
                nbt.setByte("NoAI", (byte) 1);
                ent.teleport(loc);
            }
        }.runTask(pl);

    }

    public static List<EntityType> toEntityType(List<String> en) {
        List<EntityType> ret = new ArrayList<>();
        for (String e : en) {
            try {
                ret.add(EntityType.valueOf(e));
            } catch (Exception error){
                MFHoppers.getInstance().getLogger().info("Mob conversion error: " + e);
            }

        }
        return ret;

    }

    public static LivingEntity nearest(final List<LivingEntity> entities, Location mid) {

        HashMap<LivingEntity, Integer> distances = new HashMap<>();
        HashMap<Integer, LivingEntity> distanceToEntity = new HashMap<>();
        for (LivingEntity ent : entities) {

            distances.put(ent, Integer.valueOf(String.valueOf(Math.round(mid.distance(ent.getLocation())))));
            distanceToEntity.put(Integer.valueOf(String.valueOf(Math.round(mid.distance(ent.getLocation())))), ent);

        }


        HashMap<Integer, LivingEntity> integerToEntity = new HashMap<>();
        for (LivingEntity ent : distances.keySet()) {

            integerToEntity.put(distances.get(ent), ent);

        }
        List<Integer> nums = new ArrayList<>(integerToEntity.keySet());
        Collections.sort(nums);
        if (integerToEntity.size() == 0 && nums.size() == 0) {
            return null;
        }

        if (integerToEntity.containsKey(nums.get(0))) {
            return integerToEntity.get(nums.get(0));
        } else {
            return null;
        }

    }

    public static void damage(IHopper hopper, Block damager, double amount, LivingEntity ent) {
        damage(hopper, damager, amount, ent, null);
    }

    public static void damage(IHopper hopper, Block damager, double amount, LivingEntity ent, String damageType) {
        new BukkitRunnable() {
            @Override
            public void run() {
                double current = ent.getHealth();

                if ((current - amount) < 0) {

                    if(MFHoppers.getInstance().getConfig().contains("headHunterSupport") && MFHoppers.getInstance().getConfig().getBoolean("headHunterSupport")){
                        if(Bukkit.getOnlinePlayers().size() > 0)
                        {
                            Player player = null;
                            if(Bukkit.getOnlinePlayers().stream().anyMatch(p -> ((Player) p).getName().equals(hopper.getOwner()))){
                                player = Bukkit.getOnlinePlayers().stream().filter(p -> ((Player) p).getName().equals(hopper.getOwner())).findFirst().get();
                            }
                            else {
                                if(MFHoppers.getInstance().getConfig().contains("headHunterOfflineSupport") && MFHoppers.getInstance().getConfig().getBoolean("headHunterOfflineSupport")) {
                                    player = Bukkit.getOnlinePlayers().stream().findFirst().get();
                                }
                            }
                            if(player != null) {
                                ent.damage(amount, player);
                                ent.setLastDamageCause(new EntityDamageByEntityEvent(player, ent, EntityDamageEvent.DamageCause.ENTITY_ATTACK, amount));
                            }
                            else {
                                ent.damage(amount);
                            }
                        }
                    }
                    else {
                        ent.damage(amount);
                        if (damageType == null) {
                            if (Bukkit.getPluginManager().isPluginEnabled("BeastCore")) {
                                ent.setLastDamageCause(new EntityDamageByBlockEvent(damager, ent, EntityDamageEvent.DamageCause.CUSTOM, amount));
                            }
                        } else {
                            try {
                                ent.setLastDamageCause(new EntityDamageByBlockEvent(damager, ent, EntityDamageEvent.DamageCause.valueOf(damageType.toUpperCase()), amount));
                            } catch (IllegalArgumentException ex) {
                                MFHoppers.getInstance().getLogger().warning("There is no damage type: " + damageType);
                            }
                        }
                    }

                    //FIXED HH double head problem!
                    /*List<ItemStack> list = new ArrayList<>();
                    MFHoppers.getInstance().getLogger().info(String.valueOf(ent.getEntityId()));
                    Bukkit.getServer().getPluginManager().callEvent(new EntityDeathEvent(ent, list));*/
                    return;
                }
                else {
                    ent.damage(amount);
                }

            }
        }.runTask(pl);

    }

    public static void breakBlock(Block block) {
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(Material.AIR);

                if(Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")){
                    SuperiorSkyblockAPI.getIslandAt(block.getLocation()).handleBlockBreak(block);
                }
            }
        }.runTask(pl);
    }

    public static void drop(ItemStack item, Location loc) {
        new BukkitRunnable() {
            @Override
            public void run() {
                loc.getWorld().dropItem(loc, item);
            }
        }.runTask(pl);
    }
    public static boolean materialEqualsTo(Location loc, Material toCompare) {
        return materialEqualsTo(loc, toCompare, 1);
    }

    public static boolean materialEqualsTo(Location loc, Material toCompare, int distance) {
        CompletableFuture<Boolean> ret = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                Location location = loc.clone();
                for(int i = 0; i < distance; i++){
                    MFHoppers.getInstance().getLogger().info(String.format("Material: %s Block: %s", toCompare.toString(), location.getBlock().getType().toString()));
                    if(location.getBlock().getType() != toCompare){
                        ret.complete(false);
                        return;
                    }
                    location.add(0, 1, 0);
                }
                ret.complete(true);
            }
        }.runTask(pl);
        try {
            boolean result = ret.get();
            MFHoppers.getInstance().getLogger().info("Material EQUALS Result: " + result);
            return result;
        } catch (Exception ex) {
            return false;
        }
    }

    public static List<LivingEntity> nearest(Location hopperLoc, double radius) {

        List<LivingEntity> retu = new ArrayList<>();

        for (Entity nearbyEntity : hopperLoc.getWorld().getNearbyEntities(hopperLoc, radius, radius, radius)) {
            if (nearbyEntity.getType().isAlive() && nearbyEntity.getType() != EntityType.PLAYER) {
                retu.add((LivingEntity) nearbyEntity);
            }
        }
        return retu;

    }

    public static void removeSlow(LivingEntity ent) {

        new BukkitRunnable() {
            @Override
            public void run() {
                ent.removePotionEffect(PotionEffectType.SLOW);
                NBTEntity nbt = new NBTEntity(ent);
                nbt.setByte("NoAI", (byte) 0);
            }
        }.runTask(pl);

    }

    public static String toString(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ();
    }

    public static boolean hasReachedLimit(Map<String, Object> data, MChunk chunk, Player player) {

        ConfigHopper hopper = MFHoppers.getInstance().getConfigHoppers().get(data.get("name").toString());
        int lvl = Integer.valueOf(data.get("lvl").toString());
        Map<String, Object> configData = hopper.getUpgrades().get(lvl).getToUpgrade();
        int limit = Integer.valueOf(configData.get("limitPerChunk").toString());

        if (limit != -1) {

            Collection<IHopper> hoppers = getSorted(HopperEnum.match(data.get("type").toString()), chunk).values();
            hoppers.removeIf(h -> (int) h.getData().get("lvl") != lvl);
            hoppers.removeIf(h -> !h.getName().equalsIgnoreCase(data.get("name").toString()));

            if (limit <= hoppers.size()) {

                Lang.HOPPER_LIMIT_REACHED.send(new MapBuilder().add("%name%", data.get("name")).add("%type%", StringUtils.capitalize(StringUtils.lowerCase(data.get("type").toString()))).add("%limit%", limit).add("%lvl%", lvl).getMap(), player);
                return true;

            }

        }
        return false;

    }

    public static void convertGrind(Player player, ItemStack handClone, EntityType type, boolean resetLevel, int amount) {

        int slot = player.getInventory().first(handClone);
        NBTItem nbt = new NBTItem(handClone);
        ConfigHopper hopper = MFHoppers.getInstance().getConfigHoppers().get(nbt.getString("name0"));
        int level = Integer.valueOf(nbt.getString("lvl"));
        if (resetLevel) {
            level = 1;
        }

        ItemStack replace = hopper.buildItemByLevel(level, type, Boolean.valueOf(nbt.getString("isAuto")), Boolean.valueOf(nbt.getString("isGlobal")));
        replace.setAmount(amount);

        nbt.setString("type", type.name());
        player.getInventory().setItem(slot, replace);

    }

    public static String colorize(String title) {
        return ChatColor.translateAlternateColorCodes('&', title);
    }

    public static IHopper getLinkedHopper(Location location) {

        final List<IHopper> hoppers = new ArrayList<>();

        DataManager.getInstance().getHoppers().values().forEach(map -> {
            map.values().forEach(hopper -> {
                if (hopper.isLinked()) hoppers.add(hopper);
            });
        });

        Optional<IHopper> optional = hoppers.stream().filter(it -> it.isLinkedTo(location)).findFirst();


        return optional.orElse(null);
    }

    public static Map<String, Object> deserialize(String str) {

        Map<String, Object> map = new HashMap<>();
        if (str.length() <= 2) return map;

        try {

            Properties props = new Properties();
            props.load(new StringReader(str.substring(1, str.length() - 1).replace(", ", "\n")));
            for (Map.Entry<Object, Object> e : props.entrySet()) {

                if (e.getValue().toString().startsWith("list-")) {

                    String value = e.getValue().toString().replace("list-", "");
                    String split[] = value.split("\\^");
                    List<String> locs = new ArrayList<>();
                    for (String s : split) {
                        locs.add(s);
                    }

                    map.put(e.getKey().toString(), locs);
                    continue;

                }

                map.put(e.getKey().toString(), e.getValue());

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return map;
    }

    public static String serialize(Map<String, Object> map) {

        Map<String, Object> toReturn = new HashMap<>();
        for (String key : map.keySet()) {

            if (map.get(key) instanceof List) {

                String string = "";
                for (String value : ((List<String>) map.get(key))) {
                    if (string == "") {
                        string = string + value;
                    } else {
                        string = string + "^" + value;
                    }
                }

                String list = "list-" + string;
                toReturn.put(key, list);
                continue;

            }
            toReturn.put(key, map.get(key));

        }
        return toReturn.toString();

    }


    public static boolean containsInInventory(ItemStack item, Inventory inventory) {

        for (ItemStack itemStack : inventory.getContents()) {

            if (itemStack == null || itemStack.getType() == Material.AIR) continue;

            if (MFHoppers.is13version) {

                if (itemStack.getType() == item.getType() && item.getData() == itemStack.getData()) {

                    return true;

                }
            } else {

                if (Methods.isSimilar(item, itemStack)) return true;
            }

        }

        return false;
    }

    public static boolean removeItem(ItemStack item, int amt, Inventory inv) {

        if (!inv.containsAtLeast(item, amt)) {
            return false;
        }

        ItemStack currentItem;
        for (int i = 0; i < 36; i++) {
            if ((currentItem = inv.getItem(i)) != null && currentItem.isSimilar(item)) {
                if (currentItem.getAmount() >= amt) {

                    if ((currentItem.getAmount() - amt) <= 0) inv.setItem(i, new ItemStack(Material.AIR));
                    else currentItem.setAmount(currentItem.getAmount() - amt);

                    return true;

                } else {
                    amt -= currentItem.getAmount();
                    inv.setItem(i, new ItemStack(Material.AIR));
                }
            }
        }
        return false;
    }

    public static boolean canFit(ItemStack itemStack, int amount, Inventory inv) {

        if (inv.firstEmpty() != -1) return true;

        List<ItemStack> items = Arrays.stream(inv.getContents()).filter(item -> item != null && item.getType() == itemStack.getType() && item.getDurability() == itemStack.getDurability() && item.getAmount() != item.getMaxStackSize()).collect(toList());

        items = items.stream().filter(it -> isSimilar(it, itemStack)).collect(Collectors.toList());


        if (items.isEmpty()) return false;

        boolean toReturn = true;

        for (ItemStack item : items) {

            if (item.getAmount() == item.getMaxStackSize()) toReturn = false;
            else toReturn = true;

        }

        return toReturn;

    }

    public static boolean containsPlayersAroundHopper(Location location) {
        int serverViewDistance = Bukkit.getServer().getViewDistance();

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        World world = location.getWorld();
        Chunk mainChunk = world.getChunkAt(chunkX, chunkZ);
        if (mainChunk == null) return false;

        List<Player> players = new ArrayList<>();
        players.addAll(Bukkit.getOnlinePlayers());

        Location center = mainChunk.getBlock(8, 64, 8).getLocation().clone();

        for (Player p : players) {
            if (p != null) {
                if (p.getLocation().getWorld() == center.getWorld()) {
                    Location pLocation = p.getLocation().clone();
                    pLocation.setY(255);
                    center.setY(255);
                    if (Math.round(center.distance(pLocation) / 16f) <= serverViewDistance) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    public static boolean isSimilar(ItemStack first, ItemStack second) {

        boolean similar = false;

        if (first == null || second == null) {
            return similar;
        }

        boolean sameTypeId = (first.getType() == second.getType());
        boolean sameDurability = (first.getDurability() == second.getDurability());
        boolean sameHasItemMeta = (first.hasItemMeta() == second.hasItemMeta());
        boolean sameEnchantments = (first.getEnchantments().equals(second.getEnchantments()));
        boolean sameItemMeta = true;

        if (sameHasItemMeta) {
            sameItemMeta = Bukkit.getItemFactory().equals(first.getItemMeta(), second.getItemMeta());
        }

        if (sameTypeId && sameDurability && sameHasItemMeta && sameEnchantments && sameItemMeta) {
            similar = true;
        }
        return similar;
    }

}
