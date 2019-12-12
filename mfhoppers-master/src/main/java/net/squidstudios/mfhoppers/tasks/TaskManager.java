package net.squidstudios.mfhoppers.tasks;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedEntity;
import de.Linus122.DropEdit.Main;
import de.Linus122.EntityInfo.EntityKeyInfo;
import de.Linus122.EntityInfo.KeyGetter;
import info.beastsoftware.beastcore.BeastCore;
import info.beastsoftware.beastcore.listener.MobMergerListener;
import info.beastsoftware.beastcore.mobstacker.IStackedMob;
import info.beastsoftware.beastcore.mobstacker.StackedMob;
import net.aminecraftdev.customdrops.CustomDropsAPI;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.manager.SellManager;
import net.squidstudios.mfhoppers.tasks.Listeners.BeastCoreListener;
import net.squidstudios.mfhoppers.util.item.nbt.NBTEntity;
import net.squidstudios.mfhoppers.util.moveableItem.MoveItem;
import net.squidstudios.mfhoppers.util.particles.ReflectionUtils;
import org.bukkit.*;
import org.bukkit.block.Hopper;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import net.squidstudios.mfhoppers.hopper.ConfigHopper;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.util.MContainer;
import net.squidstudios.mfhoppers.util.Methods;
import net.squidstudios.mfhoppers.util.particles.ParticleEffect;
import net.squidstudios.mfhoppers.util.plugin.Tasks;

import java.util.*;
import java.util.concurrent.*;
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
            int count = 0;

            @Override
            public void run() {

                count++;
                runGrind();
                runAutoKillTask();
                runBreakTask();
                runLinkTask();

                if (count == 3) {
                    runItemsTask();
                    count = 0;
                }
            }
        }.runTaskTimerAsynchronously(MFHoppers, 0, 25));

        add(new BukkitRunnable(){
            @Override
            public void run() {
                runSellTask();
            }
        }.runTaskTimer(MFHoppers, 0, 25));
    }

    public void add(BukkitTask task) {

        tasks.add(task);

    }


    Map<Location, EntityType> types = new ConcurrentHashMap<>();

    public void runGrind() {

        final Collection<IHopper> hoppers = Collections.unmodifiableCollection(Methods.getHopperByType(HopperEnum.Grind));

        for (IHopper hopper : hoppers) {

            if (!hopper.isChunkLoaded()) {
                continue;
            }

            boolean IS_GLOBAL = (boolean) hopper.getData().get("isGlobal");
            ConfigHopper CONFIG_HOPPER = pl.configHoppers.get(hopper.getData().get("name").toString());
            List<EntityType> BLACKLIST = Methods.toEntityType((List<String>) CONFIG_HOPPER.getDataOfHopper(hopper).get("mob-blacklist"));

            final ArrayList<Entity> entityList = new ArrayList<>();
            try {
                for (Entity entity : hopper.getChunk().getEntities()) {
                    entityList.add(entity);
                }
            } catch (NoSuchElementException ignore) {
            }


            final List<LivingEntity> LIVING_ENTITIES = new ArrayList<>(Methods.getSortedEntities(entityList, BLACKLIST));
            Location MIDDLE = hopper.getLocation().clone().add(0.5, 0.7, 0.5);

            if(!Methods.materialEqualsTo(hopper.getLocation().clone().add(0, 1, 0), Material.AIR, 2)) continue;

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
                    if (nbt.getByte("NoAI") == 1 && entity.getType() != EntityType.ENDERMAN) {
                        continue;
                    }

                    if (TYPE == null) {

                        if (NEAREST == null) {
                            continue;
                        }
                        TYPE = NEAREST.getType();
                        Methods.addSlownessAndTeleport(entity, MIDDLE);

                    } else {
                        if (NEAREST != null) {
                            TYPE = NEAREST.getType();
                            if (entity.getType() == TYPE) {
                                Methods.addSlownessAndTeleport(entity, MIDDLE);
                            }
                        }
                    }

                }
                if (TYPE != null) {

                    types.put(MIDDLE, TYPE);

                }

            } else {

                EntityType type = EntityType.valueOf(hopper.getData().get("ent").toString());

                final ArrayList<Entity> savedEntityList = new ArrayList<>();
                try {
                    for (Entity entity : MIDDLE.getChunk().getEntities()) {
                        savedEntityList.add(entity);
                    }
                } catch (NoSuchElementException ignored) {
                }

                final List<LivingEntity> entities = Methods.getSortedEntities(savedEntityList, BLACKLIST).stream().filter(e -> e.getType() == type).collect(Collectors.toList());


                for (LivingEntity entity : entities) {
                    Methods.addSlownessAndTeleport(entity, MIDDLE);

                }

            }

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
        final List<IHopper> hoppers = Methods.getHopperByType(HopperEnum.Grind);

        for (IHopper hopper : hoppers) {

            if (!hopper.isChunkLoaded()) {
                continue;
            }

            if (hopper == null) {
                continue;
            }

            if (hopper.getData().get("isAuto").toString().equalsIgnoreCase("true")) {

                int time = 0;

                if (autoKillTask.containsKey(hopper)) time = autoKillTask.get(hopper);
                else time = (int) pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("time");

                time--;
                if (time <= 0) {
                    autoKillTask.remove(hopper);
                    autoKillTask.put(hopper, (int) pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("time"));
                    ConfigHopper CONFIG_HOPPER = pl.configHoppers.get(hopper.getData().get("name").toString());
                    List<EntityType> BLACKLIST = Methods.toEntityType((List<String>) CONFIG_HOPPER.getDataOfHopper(hopper).get("mob-blacklist"));


                    final ArrayList<Entity> savedEntityList = new ArrayList<>();
                    try {
                        for (Entity entity : hopper.getChunk().getEntities()) {
                            savedEntityList.add(entity);
                        }
                    } catch (NoSuchElementException ignore) {
                    }

                    List<LivingEntity> entities = Methods.getSortedEntities(savedEntityList, BLACKLIST).stream().filter(e -> e.getLocation().distance(hopper.getLocation()) < 1).collect(Collectors.toList());
                    for (LivingEntity ent : entities) {
                        if (Bukkit.getPluginManager().isPluginEnabled("WildStacker") || Bukkit.getPluginManager().isPluginEnabled("BeastCore")) {
                            if (pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).containsKey("stack_kill") && Integer.valueOf(pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("stack_kill").toString()) > 1) {

                                int stackKill = 1;
                                if (Bukkit.getPluginManager().isPluginEnabled("BeastCore")) {
                                    if (MobMergerListener.getStackedMobsManager().isStacked(ent)) {
                                        if (MobMergerListener.getStackedMobsManager().getStack(ent) != null) {
                                            stackKill = MobMergerListener.getStackedMobsManager().getStack(ent).getSize();
                                        }
                                    }
                                }
                                if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
                                    stackKill = WildStackerAPI.getEntityAmount(ent);
                                }
                                stackKill = Math.min(Integer.valueOf(pl.configHoppers.get(hopper.getName()).getDataOfHopper(hopper).get("stack_kill").toString()), stackKill);

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
                                        entDrops = stackedEnt.getDrops(0);
                                    }
                                    if (Bukkit.getPluginManager().isPluginEnabled("DropEdit2")) {
                                        EntityKeyInfo var2 = (EntityKeyInfo) Main.data.getKeyInfo(KeyGetter.getKey(ent.getType()));
                                        if (var2 != null) {
                                            isSingleItem = true;
                                            entDrops = Arrays.asList(var2.getDrops());
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
                                    List<ItemStack> finalEntDrops = entDrops;
                                    entDrops.forEach(itemStack ->
                                    {
                                        if (itemStack != null) {
                                            if (finalIsAllItems) {
                                                dropList.add(new DropElement(ent.getWorld(), ent.getLocation(), itemStack));
                                            } else {
                                                if (finalIsDropEdit) {
                                                    if (finalEntDrops.indexOf(itemStack) != 49) {
                                                        dropList.add(new DropElement(ent.getWorld(), ent.getLocation(), new ItemStack(itemStack.getType(), finalStackKill * itemStack.getAmount())));
                                                    }

                                                } else {

                                                    if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
                                                        dropList.add(new DropElement(ent.getWorld(), ent.getLocation(), new ItemStack(itemStack.getType(), finalStackKill * (itemStack.getAmount() / WildStackerAPI.getStackedEntity(ent).getStackAmount()))));
                                                    }
                                                }
                                            }
                                        }
                                    });
                                    //stackedEnt.setStackAmount(stackedEnt.getStackAmount() - (stackKill - 1), false);
                                }
                                //Methods.damage(ent.getHealth(), ent);
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

                                            WildStackerAPI.getStackedEntity(ent).tryUnstack(finalStackKill);
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

        final List<IHopper> hoppers = Methods.getHopperByType(HopperEnum.Break);

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
                if (!Methods.materialEqualsTo(upper, Material.AIR)) {
                    final ConfigHopper.BreakDropsElement dropElement = CONFIG_HOPPER.GetBreakDropELement(hopper, upper.getBlock().getType(), upper.getBlock().getData());
                    if (dropElement == null) continue;

                    Methods.breakBlock(upper.getBlock());
                    upper.add(new Vector(0.5, 0, 0.5));

                    final List<ItemStack> dropItems = new LinkedList<>();
                    if (!dropElement.HasDamageValue) {
                        upper.getBlock().getDrops().forEach(it -> {
                            if(it.getType() != Material.AIR) {
                                dropItems.add(dropElement.Drop.getItem(it.getType()));
                            }
                        });
                    } else {
                        upper.getBlock().getDrops().forEach(it -> {
                            if(it.getType() != Material.AIR) {
                                ItemStack item = dropElement.Drop.getItem(it.getType());
                                item.setDurability(dropElement.DamageValue);
                                dropItems.add(item);
                            }
                        });
                    }

                    if (DATA.containsKey("collectDrops") && Boolean.valueOf(DATA.get("collectDrops").toString())) {
                        Bukkit.getScheduler().runTask(MFHoppers.getInstance(), new Runnable(){
                        
                            @Override
                            public void run() {
                                for (ItemStack item : dropItems) {
                                    int amount = item.getAmount();
                                    int added = Methods.addItem2(Arrays.asList(item), hopper);
                                    item.setAmount(amount - added);
                                }
                            }
                        });
                    }

                    if(dropItems.stream().filter(it -> it.getAmount() > 0).collect(Collectors.toList()).size() > 0){
                        dropItems.stream().filter(it -> it.getAmount() > 0).collect(Collectors.toList()).forEach( item -> Methods.drop(item, upper.getBlock().getLocation()));
                    }

                    if (DATA.containsKey("particle")) {
                        int version = Integer.parseInt(ReflectionUtils.PackageType.getServerVersion().split("_")[1]);
                        if (version > 8) {
                            for (Player player : Bukkit.getOnlinePlayers())
                                player.spawnParticle(Particle.valueOf(DATA.get("particle").toString()), upper.getBlock().getLocation().add(0.5, 0, 0.5), 1);
                        } else {
                            ParticleEffect effect = ParticleEffect.fromName(DATA.get("particle").toString());

                            if (effect != null) {
                                List<Player> onl = new ArrayList<>(Bukkit.getOnlinePlayers());
                                effect.display(0, 0, 0, 0, 1, upper.getBlock().getLocation().add(0.5, 0, 0.5), onl);
                            }
                        }

                    }
                }
                hopper.getData().put("time", DATA.get("breakEvery"));
            } else {
                hopper.getData().put("time", time);
            }

        }
    }

    public void runLinkTask(){

        List<String> toCompare = new ArrayList<>();
        toCompare.add("linked");

        List<IHopper> hoppers = Methods.getHopperByData(toCompare);

        for(IHopper hopper : hoppers) {

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

                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                List<Inventory> inventories = Methods.GetLinkedInventorys(hopper);

                                if (!(hopper.getLocation().getBlock().getState() instanceof Hopper)) {
                                    return;
                                }

                                Inventory source = ((Hopper) hopper.getLocation().getBlock().getState()).getInventory();

                                if (source == null) return;

                                List<ItemStack> items = Arrays.asList(source.getContents());

                                if (items.isEmpty()) return;

                                items = items.stream().filter(item -> item != null && item.getType() != Material.AIR).collect(Collectors.toList());

                                if (items.size() <= 0) return;
                                int moveAmount = (int) configData.get("linkedMoveAmount");

                                List<ItemStack> tempList = new ArrayList<ItemStack>();
                                int index = 0;
                                while (moveAmount > 0 && index < items.size()) {
                                    ItemStack item = items.get(index);

                                    tempList.add(new ItemStack(item.getType(), item.getAmount() < moveAmount ? item.getAmount() : moveAmount));

                                    moveAmount -= item.getAmount();

                                    index++;
                                }

                                final List<ItemStack> sendedItems = tempList;

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {

                                        for (ItemStack item : sendedItems) {
                                            for (Inventory destination : inventories) {

                                                if (Methods.canFit(item, item.getAmount(), destination)) {

                                                    if (item == null || !Methods.containsInInventory(item, source))
                                                        continue;

                                                    if (Methods.removeItem(item, item.getAmount(), source)) {
                                                        destination.addItem(item);
                                                    }

                                                }
                                            }
                                        }

                                    }
                                }.runTaskLater(MFHoppers.getInstance(), 2);

                            }
                        }.runTask(pl);
                    }
                } else {
                    hopper.getData().remove("linkedTime");
                    hopper.getData().put("linkedTime", time);
                }

            }

        }
    }

    public void runItemsTask() {

        Map<Chunk, List<IHopper>> hoppers = Methods.getMapHopperByType(HopperEnum.Crop, HopperEnum.Mob);

        for (Chunk chunk : hoppers.keySet()) {
            Tasks.getInstance().runTask(() -> {
                final ArrayList<Entity> entityList = new ArrayList<>();
                try {
                    for (Entity entity : chunk.getEntities()) {
                        entityList.add(entity);
                    }
                } catch(Exception ignored) {}
                try {
                    List<Item> itemsList = Methods.getItems(entityList).stream().map(e -> (Item)e).collect(Collectors.toList());
                    Methods.addItem(itemsList.stream().map(item -> MoveItem.getFrom(item)).collect(Collectors.toList()), hoppers.get(chunk));

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            });
        }
    }

    public void runSellTask(){

        List<IHopper> hoppers = new ArrayList<>();
        DataManager.getInstance().getHoppers().values().forEach(locationIHopperMap -> hoppers.addAll(locationIHopperMap.values()));

        for(IHopper hopper : hoppers.stream().filter(hopper -> hopper != null && hopper.getConfigHopper() != null && hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("sellEvery") && hopper.getConfigHopper().getDataOfHopper(hopper).containsKey("sellAmount")).collect(Collectors.toList())){

            if(!hopper.isChunkLoaded()) continue;

            Map<String, Object> configData = hopper.getConfigHopper().getDataOfHopper(hopper);

            int time = hopper.getData().containsKey("sellEvery") ? (int)hopper.getData().get("sellEvery") : (int)configData.get("sellEvery");
            time--;
            if(time == 0) {

                if(hopper == null || hopper.getInventory() == null){
                    continue;
                }

                List<ItemStack> items = Arrays.asList(hopper.getInventory().getContents());
                if (items.isEmpty()) continue;

                int sellAmount = (int) hopper.getConfigHopper().getDataOfHopper(hopper).get("sellAmount");

                items = items.stream().filter(item -> item != null && item.getType() != Material.AIR).collect(Collectors.toList());
                items = items.stream().filter(it -> SellManager.getInstance().getPrice(it) > 0.0).collect(Collectors.toList());

                while(items.stream().findFirst().orElse(null) != null && sellAmount > 0) {
                    ItemStack first = items.stream().findFirst().orElse(null);
                    double price = SellManager.getInstance().getPrice(first);

                    if (MFHoppers.getInstance().getEconomy() != null && hopper.getOwner() != null) {
                        int amount = Math.min(sellAmount, first.getAmount());
                        sellAmount -= amount;

                        price = price * amount;

                        Methods.removeItem(first, amount, hopper.getInventory());

                        Player player = Bukkit.getPlayer(hopper.getOwner());
                        if (player == null) {
                            MFHoppers.getInstance().getEconomy().depositPlayer(Bukkit.getOfflinePlayer(hopper.getOwner()), price);
                        } else MFHoppers.getInstance().getEconomy().depositPlayer(player, price);

                    }
                }

                hopper.getData().remove("sellEvery");
                hopper.getData().put("sellEvery", configData.get("sellEvery"));

            } else {

                hopper.getData().remove("sellEvery");
                hopper.getData().put("sellEvery", time);

            }

        }

    }

}
