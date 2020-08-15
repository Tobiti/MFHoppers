package net.squidstudios.mfhoppers.util;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.wildchests.api.WildChestsAPI;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.tr7zw.changeme.nbtapi.NBTEntity;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.NonNull;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.ConfigHopper;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.manager.SellManager;
import net.squidstudios.mfhoppers.tasks.TaskManager;
import net.squidstudios.mfhoppers.util.moveableItem.MoveItem;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Methods {

    private static MFHoppers plugin;

    public Methods(MFHoppers pl) {
        plugin = pl;
    }

    public static Set<IHopper> getSorted(HopperEnum henum, Chunk chunk) {
        Set<IHopper> hoppers = Sets.newHashSet();
        DataManager.getInstance().worldHolder(chunk).ifPresent(holder -> hoppers.addAll(holder.hoppersAt(chunk, hopper -> hopper.getType() == henum)));
        return hoppers;
    }

    public static Set<IHopper> getSorted(HopperEnum henum, Chunk ch, Material mat, short data) {
        Set<IHopper> hoppers = Sets.newHashSet();
        if (ch == null) return hoppers;

        DataManager.getInstance().worldHolder(ch).ifPresent(holder -> hoppers.addAll(holder.hoppersAt(ch, hopper -> hopper.getType() == henum && hopper.ContainsInFilterMaterialList(mat, data))));
        return hoppers;
    }

    public static boolean checkIfAllKeysExists(HopperEnum hopperEnum, Map<String, Object> data) {

        List<String> req = Arrays.asList(hopperEnum.getReq());
        boolean ret = true;

        for (String key : req) {

            if (!data.keySet().contains(key)) {
                plugin.out(" &c!-> Can't find required config option named: &4" + key, PluginBuilder.OutType.WITHOUT_PREFIX);
                ret = false;
            }


        }
        return ret;

    }

    public static Collection<MoveItem> addItem(Collection<MoveItem> items, IHopper hopper) {
        for (MoveItem moveItem : items) {
            if (moveItem.getAmount() <= 0) {
                break;
            }
            try{
                if (hopper.getLocation().getBlock().getType() == Material.AIR) {
                    continue;
                }
            } catch(IllegalStateException ignore){
                continue;
            }

            if (!hopper.ContainsInFilterMaterialList(moveItem.getEntity().getItemStack().getType(), moveItem.getEntity().getItemStack().getDurability())) {
                continue;
            }

            if (hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("pickupNamedItems") && !(boolean) hopper.getConfigHopper().getDataOfHopper(hopper).get("pickupNamedItems")
                    && moveItem.getEntity().getItemStack().hasItemMeta() && moveItem.getEntity().getItemStack().getItemMeta().hasDisplayName())
                continue;

            if(hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("instantSell") && (boolean) hopper.getConfigHopper().getDataOfHopper(hopper).get("instantSell")) {
                if (MFHoppers.getInstance().getEconomy() != null && hopper.getOwner() != null && !hopper.getOwner().isEmpty()){
        
                    Player player = Bukkit.getPlayer(hopper.getOwner());
                    double sellMultiplier = hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("sellMultiplier") ? (double) hopper.getConfigHopper().getDataOfHopper(hopper).get("sellMultiplier") : 1;
                    double price = SellManager.getInstance().getPrice(TaskManager.copy(moveItem.getItems().get(0), 1), player, sellMultiplier) * moveItem.getAmount();

                    if(player != null){
                        MFHoppers.getInstance().SellHistoryManager.AddEntry(player, moveItem.getItems().get(0), moveItem.getAmount(), price);
                        MFHoppers.getInstance().getEconomy().depositPlayer(player, price);
                    } else {
                        MFHoppers.getInstance().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(hopper.getOwner()), price);
                    }

                    moveItem.setAmount(0);
                    continue;
                        
                }
            }
            
            int itemsStartAmount = 0;

            if(moveItem.GetMaxReached()){
                itemsStartAmount = moveItem.getItems().stream().mapToInt(item -> item.getAmount()).sum();
            }
            List<ItemStack> leftItems = addItem2(moveItem.getItems(), hopper);
            if(!moveItem.GetMaxReached()){
                if(leftItems.size() == 0){
                    moveItem.setAmount(0);
                }
                else {
                    moveItem.setAmount(leftItems.stream().mapToInt(item -> item.getAmount()).sum());
                }
            }
            else {
                if(leftItems.size() == 0){
                    moveItem.setAmount(moveItem.getAmount() - itemsStartAmount);
                }
                else {
                    moveItem.setAmount((moveItem.getAmount() - itemsStartAmount) + leftItems.stream().mapToInt(item -> item.getAmount()).sum());
                }
            }
        }

        return items;
    }

    public static List<MoveItem> addItem(List<MoveItem> items, Collection<IHopper> iHoppers) {
        for (MoveItem moveItem : items) {
            for (IHopper hopper : iHoppers) {
                if (moveItem.getAmount() <= 0) {
                    break;
                }

                if (!hopper.ContainsInFilterMaterialList(moveItem.getEntity().getItemStack().getType(), moveItem.getEntity().getItemStack().getDurability())) {
                    continue;
                }

                if (hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("pickupNamedItems") && !(boolean) hopper.getConfigHopper().getDataOfHopper(hopper).get("pickupNamedItems") 
                    && moveItem.getEntity().getItemStack().hasItemMeta() && moveItem.getEntity().getItemStack().getItemMeta().hasDisplayName()) {
                    continue;
                }

                if(hopper.getConfigHopper().isInstantSell()) {
                    if (MFHoppers.getInstance().getEconomy() != null && hopper.getOwner() != null && !hopper.getOwner().isEmpty()){
            
                        Player player = Bukkit.getPlayer(hopper.getOwner());
                        double sellMultiplier = hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("sellMultiplier") ? (double) hopper.getConfigHopper().getDataOfHopper(hopper).get("sellMultiplier") : 1;
                        double price = SellManager.getInstance().getPrice(TaskManager.copy(moveItem.getItems().get(0), 1), player, sellMultiplier) * moveItem.getAmount();

                        if(player != null){
                            MFHoppers.getInstance().SellHistoryManager.AddEntry(player, moveItem.getItems().get(0), moveItem.getAmount(), price);
                            MFHoppers.getInstance().getEconomy().depositPlayer(player, price);
                        } else {
                            MFHoppers.getInstance().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(hopper.getOwner()), price);
                        }

                        moveItem.setAmount(0);
                        continue;
                            
                    }
                }
                int itemsStartAmount = 0;
    
                if(moveItem.GetMaxReached()){
                    itemsStartAmount = moveItem.getItems().stream().mapToInt(item -> item.getAmount()).sum();
                }
                List<ItemStack> leftItems = addItem2(moveItem.getItems(), hopper);
                if(!moveItem.GetMaxReached()){
                    if(leftItems.size() == 0){
                        moveItem.setAmount(0);
                    }
                    else {
                        moveItem.setAmount(leftItems.stream().mapToInt(item -> item.getAmount()).sum());
                    }
                }
                else {
                    if(leftItems.size() == 0){
                        moveItem.setAmount(moveItem.getAmount() - itemsStartAmount);
                    }
                    else {
                        moveItem.setAmount((moveItem.getAmount() - itemsStartAmount) + leftItems.stream().mapToInt(item -> item.getAmount()).sum());
                    }
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
                    inventories.add(MContainer.getOfLocation(location).getInventory(location).get().getInventory());
                } else {
                    hopper.unlink(location);
                }
            } catch (IllegalStateException | InterruptedException | ExecutionException ex) {
                hopper.unlink(location);
            }
        }
        return inventories;
    }

    public static List<ItemStack> addItem2(List<ItemStack> items, IHopper hopper) {
        List<ItemStack> tempItems = items.stream().filter(item -> item != null).collect(Collectors.toList());
        Inventory inv = null;
        try {
            inv = hopper.getInventory().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        if (inv == null) return tempItems;

        ItemStack[] itemArray = new ItemStack[0];

        if (hopper.isLinked() && hopper.isLinkedInstantMove()) {
            for (Inventory destination : Methods.GetLinkedInventorys(hopper)) {
                Location chestLocation = MContainer.getLocation(destination.getHolder());
                if(!isChunkLoaded(chestLocation)){
                    continue;
                }

                if(Bukkit.getPluginManager().isPluginEnabled("WildChests")){
                    Chest chest = WildChestsAPI.getChest(chestLocation);
                    if(chest != null){
                        Map<Integer, ItemStack> integerItemStackMap = chest.addItems(tempItems.toArray(itemArray));
                        if (integerItemStackMap.isEmpty()) {
                            return new ArrayList<>();
                        } else {
                            tempItems = integerItemStackMap.values().stream().collect(Collectors.toList());
                        }
                    }
                    else {
                        Map<Integer, ItemStack> integerItemStackMap = destination.addItem(tempItems.toArray(itemArray));
                        if (integerItemStackMap.isEmpty()) {
                            return new ArrayList<>();
                        } else {
                            tempItems = integerItemStackMap.values().stream().collect(Collectors.toList());
                        }
                    }
                }
                else {
                    Map<Integer, ItemStack> integerItemStackMap = destination.addItem(tempItems.toArray(itemArray));
                    if (integerItemStackMap.isEmpty()) {
                        return new ArrayList<>();
                    } else {
                        tempItems = integerItemStackMap.values().stream().collect(Collectors.toList());
                    }
                }
            }
        }
        Map<Integer, ItemStack> integerItemStackMap = inv.addItem(tempItems.stream().filter(item -> item != null && item.getType() != Material.AIR).collect(Collectors.toList()).toArray(itemArray));
        if (integerItemStackMap.isEmpty()) {
            return new ArrayList<>();
        } else {
            tempItems = integerItemStackMap.values().stream().collect(Collectors.toList());
        }

        return tempItems;
    }


    private static boolean isChunkLoaded(Location location) {
        if(location == null){
            return false;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        if (location.getWorld() == null)
            return false;

        return location.getWorld().isChunkLoaded(chunkX, chunkZ);
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
        if(item == null){
            return false;
        }
        if(item.getType() != XMaterial.HOPPER.parseMaterial()){
            return false;
        }
        NBTItem nbt = new NBTItem(item);

        if (nbt.hasKey("lvl") && nbt.hasKey("type")) {
            return true;
        } else {
            return false;
        }
    }

    public static Set<IHopper> getActiveHopperByType(HopperEnum... e) {
        List<HopperEnum> enums = Arrays.asList(e);
        Set<IHopper> hoppersSet = DataManager.getInstance().getHoppersSet(hopper -> enums.contains(hopper.getType()) && hopper.isActive());
        return hoppersSet;
    }

    public static Set<IHopper> getHopperByType(HopperEnum e) {
        Set<IHopper> hoppersSet = DataManager.getInstance().getHoppersSet();
        hoppersSet.removeIf(hopper -> hopper.getType() != e);

        return hoppersSet;
    }

    public static Map<Chunk, List<IHopper>> getMapHopperByType(HopperEnum... e) {

        Collection<IHopper> _nonHoppers;
        Map<Chunk, List<IHopper>> hoppers = new HashMap<>();
        _nonHoppers = DataManager.getInstance().getHoppersSet();

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

    public static Map<Chunk, List<IHopper>> getMapHopperByTypeOfLoadedChunks(HopperEnum... e) {

        Collection<IHopper> _nonHoppers = new ArrayList<>();
        Map<Chunk, List<IHopper>> hoppers = new HashMap<>();
        _nonHoppers = DataManager.getInstance().getHoppersSet();

        List<HopperEnum> types = Arrays.asList(e);

        for (IHopper hopper : _nonHoppers) {
            if (hopper == null) {
                continue;
            }
            if(hopper.getData().get("loc") == null){
                DataManager.getInstance().remove(hopper);
                continue;
            }

            if (!hopper.isChunkLoaded()) {
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

    public static Set<LivingEntity> getSortedEntities(Set<Entity> entityList, List<EntityType> blacklist, boolean allowCustomName) {
        Set<LivingEntity> entities = Sets.newHashSet();

        for (Entity entity : entityList) {
            if (entity == null || entity.isDead()) {
                continue;
            }
            if(OVersion.isAfter(8)){
                if(entity.isInvulnerable()){
                    continue;
                }
            }

            if (entity.getType() != EntityType.PLAYER && entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.DROPPED_ITEM && entity.getType().isAlive() && (blacklist == null || !blacklist.contains(entity.getType()))) {

                if(entity.hasMetadata("mfh_ignore")){
                    continue;
                }

                //MFHoppers.getInstance().getLogger().info(String.format("AllowCustomName: %s MobName: %s MobType: %s", String.valueOf(allowCustomName), entity.getCustomName(), entity.getType().toString()));
                if(!allowCustomName && entity.getCustomName() != null){
                    if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")){
                        StackedEntity stackedEntity = WildStackerAPI.getStackedEntity((LivingEntity) entity);
                        if(stackedEntity != null && stackedEntity.hasNameTag()){
                                continue;
                        }
                    }
                    else {
                        continue;
                    }
                }
                
                entities.add((LivingEntity) entity);
            }
        }

        return entities;
    }

    public static void addSlownessAndTeleport(Set<LivingEntity> ents, Location loc) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (LivingEntity ent : ents) {
                    if(ent == null ||ent.isDead()){
                        continue;
                    }
                    NBTEntity nbt = new NBTEntity(ent);
                    if (nbt.getByte("NoAI") == 1 && ent.getType() != EntityType.ENDERMAN) {
                        if(loc.distance(ent.getLocation()) >= 3){
                            ent.teleport(loc);
                        }
                        return;
                    }
                    PotionEffect effect = new PotionEffect(PotionEffectType.SLOW, 999999999, 60);
                    ent.addPotionEffect(effect, false);
                    nbt.setByte("NoAI", (byte) 1);
                    ent.teleport(loc);
                }
            }
        }.runTask(plugin);
    }

    public static List<EntityType> toEntityType(List<String> en) {

        List<EntityType> ret = new ArrayList<>();
        for (String e : en) {

            ret.add(EntityType.valueOf(e));

        }
        return ret;
    }

    public static LivingEntity nearest(final Set<LivingEntity> entities, Location mid) {

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

        return integerToEntity.getOrDefault(nums.get(0), null);

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

                    if (MFHoppers.getInstance().getConfig().contains("headHunterSupport") && MFHoppers.getInstance().getConfig().getBoolean("headHunterSupport")) {
                        if (Bukkit.getOnlinePlayers().size() > 0) {
                            Player player = null;
                            if (Bukkit.getOnlinePlayers().stream().anyMatch(p -> ((Player) p).getName().equals(hopper.getOwner()))) {
                                player = Bukkit.getOnlinePlayers().stream().filter(p -> ((Player) p).getName().equals(hopper.getOwner())).findFirst().get();
                            } else {
                                if (MFHoppers.getInstance().getConfig().contains("headHunterOfflineSupport") && MFHoppers.getInstance().getConfig().getBoolean("headHunterOfflineSupport")) {
                                    player = Bukkit.getOnlinePlayers().stream().findFirst().get();
                                }
                            }
                            if (player != null) {
                                ent.damage(amount, player);
                                ent.setLastDamageCause(new EntityDamageByEntityEvent(player, ent, EntityDamageEvent.DamageCause.ENTITY_ATTACK, amount));
                            } else {
                                ent.damage(amount);
                            }
                        }
                    } else {
                        List<EntityType> entitiesToKillByPlayer = new ArrayList();
                        if(OVersion.isOrAfter(13)){
                            entitiesToKillByPlayer.add(EntityType.valueOf("PHANTOM"));
                        }
                        entitiesToKillByPlayer.add(EntityType.BLAZE);

                        if(entitiesToKillByPlayer.contains(ent.getType())){
                            Player player = null;
                            if (Bukkit.getOnlinePlayers().stream().anyMatch(p -> ((Player) p).getName().equals(hopper.getOwner()))) {
                                player = Bukkit.getOnlinePlayers().stream().filter(p -> ((Player) p).getName().equals(hopper.getOwner())).findFirst().get();
                            } else {
                                player = Bukkit.getOnlinePlayers().stream().findFirst().get();
                            }
                            if(player != null){
                                ent.damage(amount, player);
                                if (damageType == null) {
                                    ent.setLastDamageCause(new EntityDamageByEntityEvent(player, ent, EntityDamageEvent.DamageCause.CUSTOM, amount));
                                } else {
                                    try {
                                        ent.setLastDamageCause(new EntityDamageByEntityEvent(player, ent, EntityDamageEvent.DamageCause.valueOf(damageType.toUpperCase()), amount));
                                    } catch (IllegalArgumentException ex) {
                                        MFHoppers.getInstance().getLogger().warning("There is no damage type: " + damageType);
                                    }
                                }
                                return;
                            }
                        }

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
                } else {
                    ent.damage(amount);
                }

            }
        }.runTask(plugin);

    }
    
    public static void breakBlock(Block block) {
        breakBlock(block, null);
    }

    public static void breakBlock(Block block, Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                
                if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
                    Island island = SuperiorSkyblockAPI.getIslandAt(block.getLocation());
                    if(island != null){
                        if(player != null){
                            BlockBreakEvent event = new BlockBreakEvent(block, player);
                            Bukkit.getPluginManager().callEvent(event);                         
                        }else {
                            island.handleBlockBreak(block);
                        }
                    }
                }
                block.setType(Material.AIR, true);
            }
        }.runTask(plugin);
    }

    public static void drop(ItemStack item, Location loc) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (item.getType() != Material.AIR && item.getAmount() > 0) {
                    loc.getWorld().dropItem(loc, item);
                }
            }
        }.runTask(plugin);
    }

    public static boolean materialEqualsTo(Location loc, Material toCompare) {
        return materialEqualsTo(loc, toCompare, 1);
    }

    public static boolean materialEqualsTo(Location loc, Material toCompare, int distance) {
        if (Thread.currentThread().getName().equalsIgnoreCase("Server thread")) {
            Location location = loc.clone();
            for (int i = 0; i < distance; i++) {
                if (location.getBlock().getType() != toCompare) {
                    return false;
                }
                location.add(0, 1, 0);
            }
            return true;
        } else {
            CompletableFuture<Boolean> ret = new CompletableFuture<>();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Location location = loc.clone();
                    for (int i = 0; i < distance; i++) {
                        if (location.getBlock().getType() != toCompare) {
                            ret.complete(false);
                            return;
                        }
                        location.add(0, 1, 0);
                    }
                    ret.complete(true);
                }
            }.runTask(plugin);
            try {
                boolean result = ret.get();
                return result;
            } catch (Exception ex) {
                return false;
            }
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
        }.runTask(plugin);

    }

    public static String toString(Location loc) {
        if (loc == null) return null;
        return loc.getWorld().getName() + ";" + loc.getX() + ";" + loc.getY() + ";" + loc.getZ();
    }

    public static boolean hasReachedLimit(Map<String, Object> data, Chunk chunk, Player player) {

        ConfigHopper hopper = MFHoppers.getInstance().getConfigHoppers().get(data.get("name").toString());
        int lvl = Integer.valueOf(data.get("lvl").toString());
        Map<String, Object> configData = hopper.getUpgrades().get(lvl).getToUpgrade();
        int limit = Integer.valueOf(configData.get("limitPerChunk").toString());

        if (limit != -1) {

            int[] sizeAtChunk = new int[]{0};
            DataManager.getInstance().worldHolder(chunk).ifPresent(holder -> sizeAtChunk[0] = holder.hoppersAt(chunk, h -> (int) h.getData().get("lvl") == lvl && h.getName().equalsIgnoreCase(data.get("name").toString())).size());

            if (limit <= sizeAtChunk[0]) {

                Lang.HOPPER_LIMIT_REACHED.send(new MapBuilder().add("%name%", data.get("name")).add("%type%", StringUtils.capitalize(StringUtils.lowerCase(data.get("type").toString()))).add("%limit%", limit).add("%lvl%", lvl).add("%level%", lvl).getMap(), player);
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

        for (IHopper hopper : DataManager.getInstance().getHoppersSet()) {
            if (hopper.isLinked()) hoppers.add(hopper);
        }

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

            if (Methods.isSimilar(item, itemStack))
                return true;
        }

        return false;
    }

    public static boolean removeItem(ItemStack item, int amt, Inventory inv) {
        if (item == null || amt <= 0) return false;

        ItemStack currentItem;
        ItemStack[] array = inv.getContents();

        for (int i = 0; i < inv.getSize(); i++) {
            if (amt <= 0) return true;

            currentItem = array[i];
            if (currentItem == null || currentItem.getType() == Material.AIR || !isSimilar(currentItem, item)) continue;

            if (currentItem.getAmount() >= amt) {
                if ((currentItem.getAmount() - amt) <= 0)
                    inv.setItem(i, new ItemStack(Material.AIR));

                else
                    currentItem.setAmount(currentItem.getAmount() - amt);

                return true;

            } else {
                amt -= currentItem.getAmount();
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }
        return amt <= 0;
    }

    public static boolean canFit(ItemStack itemStack, int amount, Inventory inv) {
        if(inv.firstEmpty() != -1){
            return true;
        }

        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) return true;

            if (isSimilar(item, itemStack))
                if ((item.getAmount() + itemStack.getAmount()) <= item.getType().getMaxStackSize())
                    return true;
        }

        return false;
    }

    public static boolean containsPlayersAroundHopper(@NonNull Location location) {
        int serverViewDistance = Bukkit.getServer().getViewDistance();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        int chunkX2 = chunkX + serverViewDistance;
        int chunkZ2 = chunkZ + serverViewDistance;

        World world = location.getWorld();
        List<Player> players = new ArrayList<>();
        try{
            for (Player player : world.getPlayers()) {
                players.add(player);
            }
        } catch (ConcurrentModificationException ignored){}

        try{
            for (Player p : players) {
                int chunkX3 = p.getLocation().getBlockX() >> 4;
                int chunkZ3 = p.getLocation().getBlockZ() >> 4;

                if (Math.min(chunkX, chunkX2) <= chunkX3 && chunkX3 <= Math.max(chunkX, chunkX2)) {
                    if (Math.min(chunkZ, chunkZ2) <= chunkZ3 && chunkZ3 <= Math.max(chunkZ, chunkZ2)) {
                        return true;
                    }
                }
            }
        } catch (ConcurrentModificationException ignored){}
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

    public static void forceSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread())
            runnable.run();

        else
            Bukkit.getScheduler().runTask(plugin, runnable);
    }

}
