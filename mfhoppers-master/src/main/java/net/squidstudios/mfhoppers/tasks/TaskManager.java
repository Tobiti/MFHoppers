package net.squidstudios.mfhoppers.tasks;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import com.google.common.collect.Sets;

import de.Linus122.DropEdit.DropContainer;
import de.Linus122.DropEdit.Main;
import de.Linus122.EntityInfo.EntityKeyInfo;
import de.Linus122.EntityInfo.KeyGetter;
import de.tr7zw.changeme.nbtapi.NBTEntity;
import net.aminecraftdev.customdrops.CustomDropsAPI;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.ConfigHopper;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.manager.SellManager;
import net.squidstudios.mfhoppers.tasks.Listeners.BeastCoreListener;
import net.squidstudios.mfhoppers.util.MContainer;
import net.squidstudios.mfhoppers.util.Methods;
import net.squidstudios.mfhoppers.util.OPair;
import net.squidstudios.mfhoppers.util.OVersion;
import net.squidstudios.mfhoppers.util.XMaterial;
import net.squidstudios.mfhoppers.util.ent.EntitiesGatherer;
import net.squidstudios.mfhoppers.util.moveableItem.MoveItem;
import net.squidstudios.mfhoppers.util.particles.ParticleEffect;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TaskManager implements Listener {
    private MFHoppers pl;
    private List<BukkitTask> tasks = new ArrayList<>();

    public class DropElement {
        public World World;
        public Location Loc;
        public ItemStack Item;

        public DropElement(World w, Location l, ItemStack i) {
            World = w;
            Loc = l;
            Item = i;
        }
    }

    public TaskManager(MFHoppers MFHoppers) {

        this.pl = MFHoppers;
        add(new BukkitRunnable() {
            @Override
            public void run() {
                runGrind();
                runAutoKillTask();
                runBreakTask();
                runLinkTask();
                runSellTask();
            }
        }.runTaskTimerAsynchronously(MFHoppers, 0, 25));

        add(new BukkitRunnable() {
            @Override
            public void run() {
                if (MFHoppers.getInstance().getConfig().getBoolean("CollectAlreadyDropedItems", true)) {
                    runItemsTask();
                }
            }
        }.runTaskTimerAsynchronously(MFHoppers, 0,
                20 * MFHoppers.getInstance().getConfig().getLong("CollectItemsEvery", 3)));

        // Auto Link Task
        add(new BukkitRunnable() {
            @Override
            public void run() {
                runAutoLinkTask();
            }
        }.runTaskTimer(MFHoppers, 0, 20 * MFHoppers.getInstance().getConfig().getLong("AutoLinkEvery", 3)));

    }

    public void add(BukkitTask task) {
        tasks.add(task);
    }

    Map<Location, EntityType> types = new ConcurrentHashMap<>();

    public void runGrind() {
        final Collection<IHopper> hoppers = Methods.getActiveHopperByType(HopperEnum.Grind);

        for (IHopper hopper : hoppers) {
            if (!hopper.isChunkLoaded()) {
                continue;
            }

            boolean IS_GLOBAL = (boolean) hopper.getData().get("isGlobal");
            ConfigHopper CONFIG_HOPPER = pl.configHoppers.get(hopper.getData().get("name").toString());
            List<EntityType> BLACKLIST = Methods
                    .toEntityType((List<String>) CONFIG_HOPPER.getDataOfHopper(hopper).get("mob-blacklist"));

            Location MIDDLE = hopper.getLocation().clone().add(0.5, 0.7, 0.5);
            final Set<Entity> entityList = EntitiesGatherer.from(MIDDLE.getChunk()).accepts(LivingEntity.class)
                    .gather();
            final Set<LivingEntity> LIVING_ENTITIES = Methods.getSortedEntities(entityList, BLACKLIST, CONFIG_HOPPER.allowNamedMobs());
            Set<LivingEntity> toAddSlowness = Sets.newHashSet();

            if (IS_GLOBAL) {

                LivingEntity NEAREST = Methods.nearest(LIVING_ENTITIES, MIDDLE);
                EntityType TYPE = null;
                if (types.containsKey(MIDDLE)) {

                    if (NEAREST == null) {

                        TYPE = types.get(MIDDLE);

                    } else {

                        TYPE = NEAREST.getType();

                    }

                }

                for (LivingEntity entity : LIVING_ENTITIES) {
                    NBTEntity nbt = new NBTEntity(entity);
                    if ((nbt.getByte("NoAI") == 1 && entity.getType() != EntityType.ENDERMAN)
                            && hopper.getLocation().distance(entity.getLocation()) < 3) {
                        continue;
                    }

                    if (TYPE == null) {
                        if (NEAREST == null) {
                            continue;
                        }
                        TYPE = NEAREST.getType();
                        toAddSlowness.add(entity);

                    } else {
                        if (NEAREST != null) {
                            TYPE = NEAREST.getType();
                            if (entity.getType() == TYPE) {
                                toAddSlowness.add(entity);
                            }
                        }
                    }
                }
                if (TYPE != null) {
                    types.put(MIDDLE, TYPE);
                }

            } else {
                EntityType type = EntityType.valueOf(hopper.getData().get("ent").toString());
                final Set<LivingEntity> entities = LIVING_ENTITIES.stream().filter(e -> e.getType() == type)
                        .collect(Collectors.toSet());

                toAddSlowness.addAll(entities);
            }

            Methods.addSlownessAndTeleport(toAddSlowness, MIDDLE);
        }
    }

    public void RemoveGrindHopper(IHopper hopper) {
        if (autoKillTask.containsKey(hopper)) {
            autoKillTask.remove(hopper);
        }
        if (types.containsKey(hopper.getLocation().clone().add(0.5, 0.7, 0.5))) {
            types.remove(types.containsKey(hopper.getLocation().clone().add(0.5, 0.7, 0.5)));
        }
    }

    private Map<IHopper, Integer> autoKillTask = new ConcurrentHashMap<>();

    public void runAutoKillTask() {
        final Set<IHopper> hoppers = Methods.getActiveHopperByType(HopperEnum.Grind);

        for (IHopper hopper : hoppers) {
            if (!hopper.isChunkLoaded()) {
                continue;
            }

            if (hopper == null) {
                continue;
            }

            if (hopper.getData().get("isAuto").toString().equalsIgnoreCase("true")) {

                int time = 0;

                if (autoKillTask.containsKey(hopper))
                    time = autoKillTask.get(hopper);
                else
                    time = (int) pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("time");

                time--;
                if (time <= 0) {
                    autoKillTask.remove(hopper);
                    autoKillTask.put(hopper,
                            (int) pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("time"));
                    ConfigHopper CONFIG_HOPPER = pl.configHoppers.get(hopper.getData().get("name").toString());
                    List<EntityType> BLACKLIST = Methods
                            .toEntityType((List<String>) CONFIG_HOPPER.getDataOfHopper(hopper).get("mob-blacklist"));

                    EntityType type = EntityType.valueOf(hopper.getData().get("ent").toString());
                    boolean isGlobal = (boolean) hopper.getData().get("isGlobal");

                    final Set<Entity> savedEntityList = EntitiesGatherer.from(hopper.getLocation().getChunk())
                            .accepts(LivingEntity.class).gather();
                    List<LivingEntity> entities = Methods.getSortedEntities(savedEntityList, BLACKLIST).stream()
                            .filter(e -> e.getLocation().distance(
                                    hopper.getLocation()) < (type.equals(EntityType.GHAST) || isGlobal ? 3 : 1))
                            .filter(e -> e.getType().equals(type) || isGlobal).collect(Collectors.toList());

                    for (LivingEntity ent : entities) {
                        if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")
                                || Bukkit.getPluginManager().isPluginEnabled("BeastCore")) {
                            if (pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).containsKey("stack_kill")
                                    && Integer.valueOf(pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper)
                                            .get("stack_kill").toString()) > 1) {

                                int stackKill = 1;
                                if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
                                    stackKill = WildStackerAPI.getEntityAmount(ent);
                                }
                                stackKill = Math.min(Integer.valueOf(pl.configHoppers.get(hopper.getName())
                                        .getDataOfHopper(hopper).get("stack_kill").toString()), stackKill);

                                int finalStackKill = stackKill;
                                final List<DropElement> dropList = new ArrayList<>();
                                if (finalStackKill > 1) {
                                    List<ItemStack> entDrops = new ArrayList<>();

                                    boolean isSingleItem = false;
                                    boolean isAllItems = false;

                                    if (Bukkit.getPluginManager().isPluginEnabled("BeastCore")) {
                                        BeastCoreListener.getInstance().beastCoreStackedKill.put(ent, finalStackKill);
                                        isAllItems = true;
                                    }

                                    if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
                                        StackedEntity stackedEnt = WildStackerAPI.getStackedEntity(ent);
                                        entDrops = stackedEnt.getDrops(0, finalStackKill);
                                        isAllItems = true;
                                    }
                                    if (Bukkit.getPluginManager().isPluginEnabled("DropEdit2")) {
                                        DropContainer container = ((Main) Main.pl)
                                                .getDrops(KeyGetter.getKey(ent.getType()), ent);
                                        if (container != null) {
                                            isSingleItem = true;
                                            isAllItems = false;
                                            entDrops = container.getItemDrops();
                                        }
                                    }
                                    if (Bukkit.getPluginManager().isPluginEnabled("CustomDrops")) {
                                        isAllItems = true;
                                        entDrops = new ArrayList<>();
                                        for (int i = 0; i < finalStackKill; i++) {
                                            entDrops.addAll(CustomDropsAPI.getCustomDrops(ent.getType()).stream().filter(itemStack -> itemStack.getType() != Material.AIR).collect(Collectors.toList()));
                                        }
                                    }

                                    boolean finalIsDropEdit = isSingleItem;
                                    boolean finalIsAllItems = isAllItems;
                                    entDrops.forEach(itemStack ->
                                    {
                                        if (itemStack != null) {
                                            if (finalIsDropEdit) {
                                                    dropList.add(new DropElement(ent.getWorld(), ent.getLocation(), new ItemStack(itemStack.getType(), finalStackKill * itemStack.getAmount())));
                                            } else {
                                                if (finalIsAllItems) {
                                                    dropList.add(new DropElement(ent.getWorld(), ent.getLocation(), itemStack));
                                                }
                                            }
                                        }
                                    });
                                }
                                new BukkitRunnable() {

                                    @Override
                                    public void run() {
                                        dropList.forEach(drop -> {
                                            drop.World.dropItemNaturally(drop.Loc, drop.Item);
                                        });
                                        if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
                                            try {
                                                ent.setLastDamageCause(new EntityDamageByBlockEvent(hopper.getLocation().getBlock(), ent, EntityDamageEvent.DamageCause.valueOf(CONFIG_HOPPER.getDataOfHopper(hopper).get("damageType").toString().toUpperCase()), 1000000));
                                            } catch (IllegalArgumentException ex) {
                                                MFHoppers.getInstance().getLogger().warning("There is no damage type: " + CONFIG_HOPPER.getDataOfHopper(hopper).get("damageType").toString());
                                            }

                                            WildStackerAPI.getStackedEntity(ent).runUnstack(finalStackKill);
                                        } else {
                                            if (CONFIG_HOPPER.getDataOfHopper(hopper).containsKey("damageType")) {
                                                Methods.damage(hopper, hopper.getLocation().getBlock(), 10000000, ent, CONFIG_HOPPER.getDataOfHopper(hopper).get("damageType").toString());
                                            } else {
                                                Methods.damage(hopper, hopper.getLocation().getBlock(), 10000000, ent);
                                            }
                                        }
                                    }
                                }.runTask(pl);
                            } else {
                                if (CONFIG_HOPPER.getDataOfHopper(hopper).containsKey("damageType")) {
                                    Methods.damage(hopper, hopper.getLocation().getBlock(), Double.valueOf(pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("damage").toString()), ent, CONFIG_HOPPER.getDataOfHopper(hopper).get("damageType").toString());
                                } else {
                                    Methods.damage(hopper, hopper.getLocation().getBlock(), Double.valueOf(pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("damage").toString()), ent);
                                }
                            }
                        } else {
                            if (CONFIG_HOPPER.getDataOfHopper(hopper).containsKey("damageType")) {
                                Methods.damage(hopper, hopper.getLocation().getBlock(), Double.valueOf(pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("damage").toString()), ent, CONFIG_HOPPER.getDataOfHopper(hopper).get("damageType").toString());
                            } else {
                                Methods.damage(hopper, hopper.getLocation().getBlock(), Double.valueOf(pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("damage").toString()), ent);
                            }
                        }
                    }
                } else {
                    autoKillTask.remove(hopper);
                    autoKillTask.put(hopper, time);
                }

            }

        }
    }

    public void runBreakTask() {
        final Set<IHopper> hoppers = Methods.getActiveHopperByType(HopperEnum.Break);

        ItemStack tool = new ItemStack(Material.DIAMOND_PICKAXE);

        for (IHopper hopper : hoppers) {
            if (!hopper.isChunkLoaded()) {
                continue;
            }

            ConfigHopper CONFIG_HOPPER = pl.configHoppers.get(hopper.getName());
            Map<String, Object> DATA = CONFIG_HOPPER.getDataOfHopper(hopper);

            int time = 0;
            if (hopper.getData().get("time") == null) {
                time = (int) DATA.get("breakEvery");
            } else {
                time = (int) hopper.getData().get("time");
            }
            time--;
            if (time <= 0) {
                Location upper = hopper.getLocation().clone().add(new Vector(0, 1, 0));

                final ConfigHopper.BreakDropsElement dropElement = CONFIG_HOPPER.GetBreakDropELement(hopper, upper.getBlock().getType(), upper.getBlock().getData());
                if(!CONFIG_HOPPER.isBreakAll()){
                    if (dropElement == null) continue;
                }

                if(dropElement == null && upper.getBlock().getType() == Material.BEDROCK){
                    continue;
                }

                Methods.breakBlock(upper.getBlock());
                upper.add(new Vector(0.5, 0, 0.5));

                final List<ItemStack> dropItems = new LinkedList<>();
                if(dropElement != null){
                    if (!dropElement.HasDamageValue) {
                        upper.getBlock().getDrops(tool).forEach(it -> dropItems.add(dropElement.Drop.getItem(it.getType())));
                    } else {
                        upper.getBlock().getDrops(tool).forEach(it -> {

                            ItemStack item = dropElement.Drop.getItem(it.getType());
                            item.setDurability(dropElement.DamageValue);
                            dropItems.add(item);
                        });
                    }
                }else {
                    upper.getBlock().getDrops(tool).forEach(it -> dropItems.add(it.clone()));
                }

                if (DATA.containsKey("collectDrops") && Boolean.valueOf(DATA.get("collectDrops").toString())) {
                    for (ItemStack item : dropItems) {
                        int amount = item.getAmount();
                        int added = Methods.addItem2(Arrays.asList(item), hopper);
                        item.setAmount(amount - added);
                    }
                }

                if (dropItems.stream().filter(it -> it.getAmount() > 0).collect(Collectors.toList()).size() > 0) {
                    dropItems.stream().filter(it -> it.getAmount() > 0).collect(Collectors.toList()).forEach(item -> Methods.drop(item, upper.getBlock().getLocation()));
                }

                if (DATA.containsKey("particle")) {
                    if (OVersion.isAfter(8)) {
                        for (Player player : Bukkit.getOnlinePlayers()){
                            try {
                                Particle particle = Particle.valueOf(DATA.get("particle").toString());
                                player.spawnParticle(particle, upper.getBlock().getLocation().add(0.5, 0, 0.5), 1);
                            } catch (IllegalArgumentException e){
                                MFHoppers.getInstance().getLogger().warning(String.format("%s is no valid particle for your version.", DATA.get("particle").toString()));
                            }
                        }
                    } else {
                        ParticleEffect effect = ParticleEffect.fromName(DATA.get("particle").toString());

                        if (effect != null) {
                            List<Player> onl = new ArrayList<>(Bukkit.getOnlinePlayers());

                            effect.display(0, 0, 0, 0, 1, upper.getBlock().getLocation().add(0.5, 0, 0.5), onl);
                        }
                    }

                }
                hopper.getData().remove("time");
                hopper.getData().put("time", DATA.get("breakEvery"));
            } else {
                hopper.getData().put("time", time);
            }

        }
    }

    public void runLinkTask() {
        Collection<IHopper> hoppers = DataManager.getInstance().getHoppersSet(hopper -> hopper.getData().containsKey("linked"));

        for (IHopper hopper : hoppers) {
            if (!hopper.isChunkLoaded()) continue;

            ConfigHopper configHopper = hopper.getConfigHopper();
            Map<String, Object> configData = configHopper.getDataOfHopper(hopper);

            if (configData.containsKey("linkedMoveEvery") && configData.containsKey("linkedMoveAmount")) {
                int time = hopper.getData().containsKey("linkedTime") ? (int) hopper.getData().get("linkedTime") : (int) configData.get("linkedMoveEvery");
                time--;

                if (time == 0) {
                    if (hopper.isLinked()) {
                        hopper.getData().remove("linkedTime");
                        hopper.getData().put("linkedTime", hopper.getData().containsKey("linkedTime") ? (int) hopper.getData().get("linkedTime") : (int) configData.get("linkedMoveEvery"));

                        List<Inventory> inventories = Methods.GetLinkedInventorys(hopper);

                        Inventory source = null;
                        try {
                            source = MContainer.getInventory(hopper.getLocation()).get().getInventory();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                        if (source == null) continue;

                        List<ItemStack> items = Arrays.asList(source.getContents());
                        if (items.isEmpty()) continue;

                        items = items.stream().filter(item -> item != null && item.getType() != Material.AIR).collect(Collectors.toList());

                        if (items.size() <= 0) continue;
                        int moveAmount = (int) configData.get("linkedMoveAmount");

                        List<ItemStack> tempList = new ArrayList<>();
                        int index = 0;
                        while (moveAmount > 0 && index < items.size()) {
                            ItemStack item = items.get(index);
                            tempList.add(new ItemStack(item.getType(), item.getAmount() < moveAmount ? item.getAmount() : moveAmount));

                            moveAmount -= item.getAmount();
                            index++;
                        }

                        final List<ItemStack> sendedItems = tempList;
                        for (ItemStack item : sendedItems) {
                            for (Inventory destination : inventories) {
                                if (Methods.removeItem(item, item.getAmount(), source)) {
                                    destination.addItem(item);
                                }
                            }
                        }
                    }
                } else {
                    hopper.getData().remove("linkedTime");
                    hopper.getData().put("linkedTime", time);
                }

            }

        }
    }

    public void runItemsTask() {
        Map<Chunk, List<IHopper>> hoppers = Methods.getMapHopperByTypeOfLoadedChunks(HopperEnum.Crop, HopperEnum.Mob);

        for (Chunk chunk : hoppers.keySet()) {
            final Set<Entity> entityList = EntitiesGatherer.from(chunk).accepts(Item.class).gather();
            final List<IHopper> chunkHoppers = hoppers.get(chunk);

            for (IHopper hopper : chunkHoppers) {
                Methods.addItem(
                        entityList
                                .stream()
                                .filter(item -> {
                                    ItemStack itemStack = ((Item) item).getItemStack();
                                    return hopper.ContainsInFilterMaterialList(itemStack.getType(), itemStack.getDurability());
                                })
                                .map(item -> MoveItem.getFrom((Item) item))
                                .collect(Collectors.toSet()),
                        hopper
                );
            }
        }
    }

    public void runSellTask() {
        Set<IHopper> hoppers = DataManager.getInstance().getHoppersSet(hopper -> hopper != null && hopper.getConfigHopper() != null && hopper.getConfigHopper().getDataOfHopper(hopper).get("sellEvery") != null && hopper.getConfigHopper().getDataOfHopper(hopper).get("sellAmount") != null);

        for (IHopper hopper : hoppers) {
            if (!hopper.isChunkLoaded()) continue;
            if (MFHoppers.getInstance().getEconomy() == null || hopper.getOwner() == null) continue;

            Player player = Bukkit.getPlayer(hopper.getOwner());
            Map<String, Object> configData = hopper.getConfigHopper().getDataOfHopper(hopper);

            int time = hopper.getData().containsKey("sellEvery") ? (int) hopper.getData().get("sellEvery") : (int) configData.get("sellEvery");
            time--;

            if (time == 0) {
                hopper.getInventory().whenComplete((inventory, thrw) -> {
                    List<OPair<ItemStack, Double>> items = Arrays
                            .stream(inventory.getContents())
                            .filter(item -> item != null && item.getType() != Material.AIR)
                            .map(item -> new OPair<>(item, SellManager.getInstance().getPrice(copy(item, 1), player)))
                            .filter(itemPair -> itemPair.getSecond() > 0.0)
                            .collect(Collectors.toList());
                    if (items.isEmpty()) return;

                    int sellAmount = (int) hopper.getConfigHopper().getDataOfHopper(hopper).get("sellAmount");
                    int finalPrice = 0;

                    for (OPair<ItemStack, Double> item : items) {
                        if (sellAmount <= 0) break;

                        int amount = Math.min(sellAmount, item.getFirst().getAmount());
                        boolean removed = Methods.removeItem(item.getFirst(), amount, inventory);

                        if (removed) {
                            finalPrice += item.getSecond() * amount;
                            sellAmount -= amount;
                        }
                    }

                    if (player == null) {
                        MFHoppers.getInstance().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(hopper.getOwner()), finalPrice);

                    } else
                        MFHoppers.getInstance().getEconomy().depositPlayer(player, finalPrice);

                    hopper.getData().remove("sellEvery");
                    hopper.getData().put("sellEvery", configData.get("sellEvery"));
                });
            } else {
                hopper.getData().remove("sellEvery");
                hopper.getData().put("sellEvery", time);

            }

        }

    }

    public ItemStack copy(ItemStack item, int setAmount) {
        ItemStack clone = item.clone();
        clone.setAmount(setAmount);
        return clone;
    }

    public void runAutoLinkTask(){
        Set<IHopper> hoppers = DataManager.getInstance().getHoppersSet(hopper -> hopper != null && hopper.getConfigHopper() != null && hopper.getConfigHopper().getDataOfHopper(hopper).get("autoLinkToChest") != null && ((boolean)hopper.getConfigHopper().getDataOfHopper(hopper).get("autoLinkToChest")));

        final HashMap<IHopper, ChunkSnapshot> map = new HashMap<>();
        for (IHopper hopper : hoppers) {
            if (!hopper.isChunkLoaded()) continue;

            map.put(hopper, hopper.getChunk().getChunkSnapshot());
        }

        new BukkitRunnable(){
        
            @Override
            public void run() {
                for (IHopper hopper : map.keySet()) {
                    int x = (int)Math.abs(hopper.getLocation().getX()) % 16;
                    int y = (int)hopper.getLocation().getY();
                    int z = (int)Math.abs(hopper.getLocation().getZ()) % 16;
                    ChunkSnapshot snapshot = map.get(hopper);
                    boolean stillChests = true;
                    while(y >= 0 && stillChests){
                        y--;

                        Material material = snapshot.getBlockType(x, y, z);
                        if (material.equals(Material.CHEST) || (OVersion.isOrAfter(14) && material.equals(XMaterial.matchXMaterial("BARREL").get().parseMaterial()))){
                            Location loc = hopper.getLocation().clone().add(0, y-hopper.getLocation().getY(), 0);
                            new BukkitRunnable(){
                                @Override
                                public void run() {
                                    if(!hopper.isLinkedTo(loc)){
                                        hopper.link(loc);
                                    }
                                }
                            }.runTask(MFHoppers.getInstance());
                        }
                        else {
                            stillChests = false;
                        }
                    }
                }
            }
        }.runTaskAsynchronously(MFHoppers.getInstance());
    }
}
