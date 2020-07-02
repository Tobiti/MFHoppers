package net.squidstudios.mfhoppers;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.wildchests.api.WildChestsAPI;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.google.common.collect.Sets;

import de.tr7zw.changeme.nbtapi.NBTEntity;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.milkbowl.vault.economy.Economy;
import net.squidstudios.mfhoppers.api.events.ItemsHopperCatchEvent;
import net.squidstudios.mfhoppers.hopper.ConfigHopper;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.hopper.convert.EntityPrice;
import net.squidstudios.mfhoppers.hopper.convert.HopperConvert;
import net.squidstudios.mfhoppers.hopper.filter.FilterInventory;
import net.squidstudios.mfhoppers.hopper.upgrades.UpgradeEnum;
import net.squidstudios.mfhoppers.hopper.upgrades.UpgradeInventory;
import net.squidstudios.mfhoppers.manager.DataManager;
import net.squidstudios.mfhoppers.manager.HookManager;
import net.squidstudios.mfhoppers.manager.SellHistoryManager;
import net.squidstudios.mfhoppers.manager.SellManager;
import net.squidstudios.mfhoppers.tasks.Listeners.BeastCoreListener;
import net.squidstudios.mfhoppers.tasks.TaskManager;
import net.squidstudios.mfhoppers.util.*;
import net.squidstudios.mfhoppers.util.cmd.Cmd;
import net.squidstudios.mfhoppers.util.cmd.Sender;
import net.squidstudios.mfhoppers.util.ent.Textures12;
import net.squidstudios.mfhoppers.util.ent.Textures13;
import net.squidstudios.mfhoppers.util.inv.InvManager;
import net.squidstudios.mfhoppers.util.inv.InvType;
import net.squidstudios.mfhoppers.util.inv.InventoryBuilder;
import net.squidstudios.mfhoppers.util.item.ItemBuilder;
import net.squidstudios.mfhoppers.util.moveableItem.MoveItem;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import net.squidstudios.mfhoppers.util.plugin.Tasks;
import objectexplorer.MemoryMeasurer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;

import static java.util.stream.Collectors.toList;

public class MFHoppers extends PluginBuilder {

    private static MFHoppers instance;
    private Economy economy = null;
    public YamlConfiguration cnf;

    public Map<String, ConfigHopper> configHoppers = new ConcurrentHashMap<>();
    public Map<String, HopperConvert> convertHoppers = new ConcurrentHashMap<>();

    public TaskManager taskManager;
    public SellHistoryManager SellHistoryManager;

    @Override
    public void init() {
        MinecraftVersion.logger.setLevel(Level.OFF);

        if (ReflectionUtil.SERVER_VERSION_NUM < 8 || ReflectionUtil.SERVER_VERSION_NUM > 16) {
            out("This Jar is for server versions between 1.8-1.16.X", OutType.ERROR);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        instance = this;

        File oldName = new File(getDataFolder().getAbsolutePath().replace("MFHoppers", "AquaHoppers"));

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        if (oldName.exists() && oldName.isDirectory()) {

            for (File source : oldName.listFiles()) {
                File dest = new File(getDataFolder(), source.getName());
                try {
                    InputStream is = null;
                    OutputStream os = null;
                    try {
                        is = new FileInputStream(source);
                        os = new FileOutputStream(dest);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            os.write(buffer, 0, length);
                        }
                    } finally {
                        is.close();
                        os.close();
                    }
                } catch (Exception ex) {
                    out(ex);
                }
            }
        }

        cnf = initConfig("config.yml", getDataFolder());
        initConfig();
        //updateConfig();

        new Tasks(this);
        new Methods(this);
        new DataManager(this);
        taskManager = new TaskManager(this);
        SellHistoryManager = new SellHistoryManager();
        new InvManager(this);
        new SellManager();
        new HookManager(this);

        initListeners();
        initCommands();
        Lang.init();

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {

            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                out("&3Vault found, hooked!");
            }

        } else {
            out("&cVault not found, economy support disabled.");
            economy = null;

        }
    }

    public static MFHoppers getInstance() {
        return instance;
    }

    public Map<String, ConfigHopper> getConfigHoppers() {
        return configHoppers;
    }

    void initConfig() {

        for (String hopperName : cnf.getConfigurationSection("Hoppers").getKeys(false)) {

            if (cnf.contains("Hoppers." + hopperName + ".upgrades")) {

                HashMap<Integer, Map<String, Object>> upgrades = new HashMap<>();

                for (String level : cnf.getConfigurationSection("Hoppers." + hopperName + ".upgrades").getKeys(false)) {

                    Map<String, Object> wrongData = cnf.getConfigurationSection("Hoppers." + hopperName + ".upgrades." + level).getValues(true);
                    Map<String, Object> rightData = new HashMap<>();

                    for (String key : wrongData.keySet()) {
                        rightData.put(key.replaceAll("upgrades." + level + ".", ""), wrongData.get(key));
                    }
                    upgrades.put(Integer.valueOf(level), rightData);

                }
                Map<String, Object> data = cnf.getConfigurationSection("Hoppers." + hopperName).getValues(true);
                new ConfigHopper(data, this, hopperName, upgrades);

            } else {
                new ConfigHopper(cnf.getConfigurationSection("Hoppers." + hopperName).getValues(true), this, hopperName);
            }

        }
        out("&3Loaded (" + configHoppers.size() + ") config hoppers!");
        new UpgradeInventory(cnf.getConfigurationSection("UpgradeInventory").getValues(true));
        new FilterInventory();
        convertHoppers = HopperConvert.decode(cnf.getStringList("HopperConvert.hoppers"));

    }

    void initListeners() {
        if (Bukkit.getPluginManager().isPluginEnabled("BeastCore")) {
            BeastCoreListener listener = new BeastCoreListener();
            listener.Init();
            MFHoppers.getInstance().getServer().getPluginManager().registerEvents(listener, this);
        }

        addListener(ItemSpawnEvent.class, EventPriority.LOWEST, event -> {
            if (event.isCancelled()) return;

            if (event.getEntityType() == EntityType.DROPPED_ITEM) {
                Item item = event.getEntity();
                if(Bukkit.getPluginManager().isPluginEnabled("ProCosmetics")){
                    NBTEntity nbtEntity = new NBTEntity(event.getEntity());
                    if(nbtEntity.hasKey("PROCOSMETICS_ITEM")){
                        return;
                    }
                }

                ItemStack itemStack = Objects.requireNonNull(item.getItemStack(), "ItemStack cannot be null!");

                if(itemStack == null || itemStack.getType() == Material.AIR){
                    return;
                }

                if(Bukkit.getPluginManager().isPluginEnabled("WildChests")){
                    NBTItem nbtItem = new NBTItem(itemStack);
                    if(nbtItem.hasKey("WildChests")){
                        return;
                    }
                }

                Collection<IHopper> hoppers = Sets.newHashSet();
                DataManager.getInstance().worldHolder(event.getLocation()).ifPresent(holder -> hoppers.addAll(holder.hoppersAt(
                        Objects.requireNonNull(event.getLocation().getChunk(), "Chunk cannot be null!"),
                        hopper -> (hopper.getType() == HopperEnum.Mob || hopper.getType() == HopperEnum.Crop) && hopper.ContainsInFilterMaterialList(itemStack.getType(), itemStack.getDurability()))));
                if (hoppers.isEmpty()) return;

                List<MoveItem> items = new ArrayList<>();
                items.add(MoveItem.getFrom(item));

                ItemsHopperCatchEvent catchEvent = new ItemsHopperCatchEvent(items, hoppers);
                Bukkit.getPluginManager().callEvent(catchEvent);
                if (catchEvent.isCancelled()) return;
      
                List<MoveItem> items2 = Methods.addItem(catchEvent.getItemList(), catchEvent.getHopperList());
                if (items2.stream().map(MoveItem::getAmount).max(Integer::compare).get() <= 0) {
                    Methods.forceSync(() -> event.getEntity().remove());

                } else {
                    Methods.forceSync(() -> item.getItemStack().setAmount(items.get(0).getAmount()));
                }
            }
        });

        addListener(EntityExplodeEvent.class, EventPriority.NORMAL, event -> {
            if (DataManager.getInstance().containsHoppersChunk(event.getLocation().getChunk())) {

                ArrayList<Block> removedBlocks = new ArrayList();

                for (Block b : event.blockList()) {
                    if (b.getType() == Material.HOPPER) {
                        if (DataManager.getInstance().isHopper(b.getLocation())) {
                            if(event.getEntityType().equals(EntityType.CREEPER)){
                                removedBlocks.add(b);
                            }

                            b.getLocation().getWorld().dropItem(b.getLocation(), configHoppers.get(DataManager.getInstance().getHopper(b.getLocation()).getName()).getItemOfData(DataManager.getInstance().getHopper(b.getLocation())));
                            DataManager.getInstance().remove(b.getLocation());
                        }
                    }
                }
                for (Block block : removedBlocks) {
                    event.blockList().remove(block);
                }

            }
        });

        addListener(ChunkLoadEvent.class, EventPriority.HIGHEST, event -> {
            DataManager dm = DataManager.getInstance();
            dm.worldHolder(event.getChunk()).ifPresent(worldHolder -> Bukkit.getScheduler().runTaskLater(this, () -> worldHolder.hoppersAt(event.getChunk().getX(), event.getChunk().getZ()).forEach(IHopper::recacheInventory), 1));
        });

        addListener(BlockBreakEvent.class, EventPriority.HIGHEST, event -> {
            if (event.isCancelled() || !DataManager.getInstance().isHopper(event.getBlock().getLocation())) return;
            if (event.getBlock().getType() == Material.AIR) return;

            event.setCancelled(true);
            event.getBlock().getDrops().clear();

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (DataManager.getInstance().isHopper(event.getBlock().getLocation())) {
                        IHopper hopper = DataManager.getInstance().getHopper(event.getBlock().getLocation());
                        Lang.BROKE.send(new MapBuilder().add("%type%", hopper.getType().name()).add("%lvl%", hopper.getLevel()).add("%name%", hopper.getName()).add("%displayName%", hopper.getConfigHopper().getItemOfData(hopper).getItemMeta().getDisplayName()).getMap(), event.getPlayer());

                        if (hopper.getType() == HopperEnum.Grind) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        for (LivingEntity entity : Methods.nearest(event.getBlock().getLocation(), 0.9)) {
                                            Methods.removeSlow(entity);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }.runTask(getInstance());
                        }

                        DataManager.getInstance().remove(event.getBlock().getLocation());
                        boolean alreadyDropped = false;
                        Map<String, Object> dataOfHopper = configHoppers.get(hopper.getName()).getDataOfHopper(hopper);
                        if (dataOfHopper.containsKey("DropToInventory") && (boolean) dataOfHopper.get("DropToInventory")) {
                            if (event.getPlayer().getInventory().firstEmpty() != -1) {
                                if (hopper.getType() == HopperEnum.Grind) {
                                    event.getPlayer().getInventory().addItem(configHoppers.get(hopper.getName()).buildItemByLevel(hopper.getLevel(), (EntityType) hopper.getData().get("ent"), (Boolean) hopper.getData().get("isAuto"), (Boolean) hopper.getData().get("isGlobal")));
                                } else {
                                    event.getPlayer().getInventory().addItem(configHoppers.get(hopper.getName()).buildItemByLevel(hopper.getLevel()));
                                }
                                alreadyDropped = true;
                            }
                        }
                        if (!alreadyDropped) {
                            Methods.drop(configHoppers.get(hopper.getName()).getItemOfData(hopper), hopper.getLocation());
                        }

                        Methods.breakBlock(event.getBlock());
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getPluginManager().callEvent(new BlockBreakEvent(event.getBlock(), event.getPlayer()));
                            }
                        }.runTask(instance);

                        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
                            if(SuperiorSkyblockAPI.getPlayer(event.getPlayer()).getIsland() != null){
                                SuperiorSkyblockAPI.getPlayer(event.getPlayer()).getIsland().handleBlockBreak(event.getBlock(), 1);
                            }
                        }

                    } else if (MContainer.isContainer(event.getBlock().getLocation())) {
                        if (Methods.getLinkedHopper(event.getBlock().getLocation()) != null) {
                            Methods.getLinkedHopper(event.getBlock().getLocation()).unlink(event.getBlock().getLocation());
                        }
                    }
                }
            }.runTaskAsynchronously(this);
        });
        addListener(BlockPlaceEvent.class, EventPriority.HIGHEST, event -> {
            if (event.isCancelled()) return;

            if (Methods.isHopper(event.getItemInHand())) {
                ItemStack item = event.getItemInHand().clone();
                if(OVersion.isOrAfter(15)){
                    // Moved here because on 1.15 the hopper is placed before it can get the item. So it gets the empty item slot and breaks!
                    PlaceHopper(event, item);
                }
                else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if(item == null || item.getType() == Material.AIR){
                                return;
                            }
                            PlaceHopper(event, item);
                        }
                    }.runTaskAsynchronously(this);
                }
            }
        });
        addListener(PlayerInteractEvent.class, EventPriority.NORMAL, event -> {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

                if (!event.getPlayer().isSneaking()) {
                    return;
                }
                
                if(OVersion.isBefore(14)){
                    if (Methods.isHopper(event.getPlayer().getEquipment().getItemInHand())) {
                        return;
                    }
                }
                else {
                    if (Methods.isHopper(event.getPlayer().getEquipment().getItemInMainHand())) {
                        return;
                    }
                }

                if (DataManager.getInstance().isHopper(event.getClickedBlock().getLocation())) {

                    IHopper HOPPER = DataManager.getInstance().getHopper(event.getClickedBlock().getLocation());
                    ConfigHopper CONFIG_HOPPER = configHoppers.get(HOPPER.getData().get("name").toString());
                    if (CONFIG_HOPPER.isUpgradable()) {

                        Map<String, Object> upgrade = CONFIG_HOPPER.getNextHopperUpgrade(HOPPER);
                        if (upgrade == null) {
                            if (event.getItem() == null || (event.getItem().getType() != Material.HOPPER && event.getItem().getType() != Material.CHEST)) {
                                Lang.HOPPER_ALREADY_IS_MAX_LEVEL.send(event.getPlayer());
                                event.setCancelled(true);
                                return;
                            }
                        }

                        event.getPlayer().openInventory(UpgradeInventory.getInstance().build(HOPPER, event.getPlayer()));
                        event.setCancelled(true);

                    }
                    if (CONFIG_HOPPER.isEditableFilter() || event.getPlayer().getName().equals("Tobiti22")) {
                        if (HOPPER.getType() == HopperEnum.Mob || HOPPER.getType() == HopperEnum.Crop) {
                            event.getPlayer().openInventory(FilterInventory.getInstance().build(HOPPER));
                            event.setCancelled(true);
                        }
                    }
                }

                if (event.getPlayer().hasMetadata("link")) {

                    Location hopperLocation = Methods.toLocation(event.getPlayer().getMetadata("link").get(0).asString());

                    event.setCancelled(true);

                    if (MContainer.isContainer(event.getClickedBlock().getLocation())) {

                        IHopper linkedHopper = DataManager.getInstance().getHopper(hopperLocation);

                        int currentlyHas = linkedHopper.getLinked().size();

                        int limit = linkedHopper.getConfigHopper().getDataOfHopper(linkedHopper).containsKey("linkedLimit") ? (int) linkedHopper.getConfigHopper().getDataOfHopper(linkedHopper).get("linkedLimit") : -1;

                        if (limit > -1) {
                            if (limit == currentlyHas) {
                                Lang.LINKING_CONTAINER_REACHED_LIMIT.send(new MapBuilder().add("%limit%", limit).getMap(), event.getPlayer());
                                event.getPlayer().removeMetadata("link", this);
                                return;
                            }

                        }

                        if (Methods.getLinkedHopper(event.getClickedBlock().getLocation()) != null) {

                            if (!cnf.getBoolean("allowMultipleHoppersToLinkOneContainer")) {
                                Lang.CONTAINER_IS_ALREADY_LINKED.send(event.getPlayer());
                                event.getPlayer().removeMetadata("link", this);
                                return;
                            }

                        }

                        Lang.HOPPER_LINK_SUCCESSFULLY_LINKED.send(event.getPlayer());
                        DataManager.getInstance().link(hopperLocation, event.getClickedBlock().getLocation());
                        event.getPlayer().removeMetadata("link", this);

                    } else {
                        Lang.HOPPER_LINK_CLICKED_BLOCK_IS_NOT_CONTAINER.send(event.getPlayer());
                    }
                }

            }
        });
        addListener(InventoryOpenEvent.class, EventPriority.NORMAL, event -> {

            if (event.getInventory().getHolder() instanceof Hopper) {
                Hopper mcHopper = (Hopper) event.getInventory().getHolder();

                if (mcHopper.getBlock().getLocation() != null && DataManager.getInstance().isHopper(mcHopper.getBlock().getLocation())) {

                    IHopper hopper = DataManager.getInstance().getHopper(mcHopper.getBlock().getLocation());

                    String title = hopper.getConfigHopper().getTitle(hopper);
                    Tasks.getInstance().runTaskLater(() -> ReflectionUtil.updateInventoryTitle((Player) event.getPlayer(), title, "minecraft:hopper"), 1);

                }
            } else {
                if (cnf.contains("EnableLinkedContainerRenaming") && cnf.getBoolean("EnableLinkedContainerRenaming")) {
                    MContainer container = MContainer.getFromHolder(event.getInventory().getHolder());
                    if (container == null) return;

                    Location containerLocation = container.getLocation(event.getInventory().getHolder());
                    if (containerLocation == null) return;

                    IHopper hopper = Methods.getLinkedHopper(containerLocation);

                    if (hopper == null) {
                        return;
                    }

                    String title = cnf.getString("LinkedContainer");
                    String mcName = event.getView().getTitle();

                    Tasks.getInstance().runTaskLater(() -> ReflectionUtil.updateInventoryTitle((Player) event.getPlayer(), getTitle(mcName, title), MContainer.getMinecraftName(event.getInventory().getHolder())), 0);
                }
            }
        });
        
        /*if(Bukkit.getPluginManager().isPluginEnabled("WildChests")){
            addListener(InventoryMoveItemEvent.class, EventPriority.HIGHEST, event -> {

                if(event.getSource().getHolder() instanceof Hopper)
                {
                    Chest chest = WildChestsAPI.getChest(event.getDestination().getLocation());
                    if(chest != null){
                        Map<Integer, ItemStack> integerItemStackMap = chest.addItems(event.getItem());

                        //Debug
                        /*MFHoppers.getInstance().getLogger().info(String.format("InventoryMoveItemEvent Add to WildChest: (%s, %f, %f, %f)", chest.getLocation().getWorld().getName(), chest.getLocation().getX(), chest.getLocation().getY(), chest.getLocation().getZ()));
                        MFHoppers.getInstance().getLogger().info("\tSent  Items:");
                        MFHoppers.getInstance().getLogger().info(String.format("\t\t -%s %d", event.getItem().getType().toString(), event.getItem().getAmount()));
                        MFHoppers.getInstance().getLogger().info("\tNot added Items:");
                        for (ItemStack itemStack : integerItemStackMap.values()) {
                            MFHoppers.getInstance().getLogger().info(String.format("\t\t -%s %d", itemStack.getType().toString(), itemStack.getAmount()));
                        }*//*
                        
                        if (integerItemStackMap.isEmpty()) {
                            event.getSource().removeItem(event.getItem());
                        } else {
                            ItemStack itemStack = integerItemStackMap.values().stream().findFirst().orElse(null);
                            ItemStack removeItem = event.getItem().clone();
                            removeItem.setAmount(event.getItem().getAmount() - itemStack.getAmount());
                            event.getSource().removeItem(removeItem);
                        }
                        event.setCancelled(true);
                    }
                }

            });
        }*/
    }

    private String getTitle(String mcName, String title) {

        if (mcName.contains(".")) {
            mcName = StringUtils.capitalize(mcName.split("\\.")[1].toLowerCase());
        } else {
            mcName = StringUtils.capitalize(mcName.toLowerCase());
        }
        return title.replace("%name%", mcName);
    }


    //public Map<Player, Map<String, Object>> linkData = new HashMap<>();

    void initCommands() {
        List<String> al = new ArrayList<>();
        al.add("mfh");
        addCommand("mfhoppers", "Main command for MFHoppers", "", al, cmd -> {
            Sender sender = cmd.getSender();

            if (cmd.args().length == 0) {
                if(sender instanceof Player){
                    if (!(sender.getPlayer().hasPermission("mfh.adminhelp") || sender.getPlayer().hasPermission("mfh.help"))) {
                        Lang.NO_PERMISSION.send(sender);
                        return;
                    }
                }
                Lang.HELP.send(sender);

                if (!sender.getPlayer().hasPermission("mfh.adminhelp")) {
                    sender.sendMessage(c("&c&l(!)&7 You don't have permission!"));
                    return;
                }
                Lang.ADMIN_HELP.send(sender);
            } else if (cmd.args().length > 0) {

                if (cmd.args()[0].equalsIgnoreCase("reload")) {

                    if (sender.isPlayer()) {

                        if (!sender.getPlayer().hasPermission("mfh.reload")) {
                            Lang.NO_PERMISSION.send(sender);
                            return;
                        }

                    }

                    restart();
                    cmd.getSender().sendMessage("&b&l(!)&7 The plugin was successfully reloaded!");

                } else if (cmd.args()[0].equalsIgnoreCase("replacefilter")) {
                    if (sender.isPlayer()) {
                        if (!sender.getPlayer().hasPermission("mfh.replacefilter")) {
                            Lang.NO_PERMISSION.send(sender);
                            return;
                        }
                    }

                    DataManager.getInstance().getHoppersSet().forEach(hopper -> hopper.ResetFilterList());
                    cmd.getSender().sendMessage("&b&l(!)&7 The command replaced all filter lists of all existing hopper!");

                } else if (cmd.args()[0].equalsIgnoreCase("cleanWrongHoppers")) {
                    if (sender.isPlayer()) {
                        if (!sender.getPlayer().hasPermission("mfh.cleanWrongHoppers")) {
                            Lang.NO_PERMISSION.send(sender);
                            return;
                        }
                    }

                    Set<IHopper> hoppers = DataManager.getInstance().getHoppersSet();
                    for (IHopper hopper : hoppers) {
                        if (hopper == null) continue;
                        if (hopper.getConfigHopper() == null) {
                            DataManager.getInstance().remove(hopper);
                            continue;
                        }

                        while(!hopper.getConfigHopper().getUpgrades().containsKey(hopper.getLevel())){
                            hopper.setLevel(hopper.getLevel() - 1);
                        }
                    }
                    cmd.getSender().sendMessage("&b&l(!)&7 The command removes or sets the level down of all hoppers without data!");
                    return;

                } else if (cmd.args()[0].equalsIgnoreCase("dump")) {
                    if (sender.isPlayer()) {
                        if (!sender.getPlayer().hasPermission("mfh.dump")) {
                            Lang.NO_PERMISSION.send(sender);
                            return;
                        }
                    }
                    Tasks.getInstance().runTaskAsync(() -> {
                        MFHoppers.getInstance().getLogger().info("-------- MFHopper DUMP START --------");
                        MFHoppers.getInstance().getLogger().info("MFHopper Overview: ");

                        for (HopperEnum en : HopperEnum.values()) {
                            MFHoppers.getInstance().getLogger().info("\t" + en.name() + ": " + Methods.getHopperByType(en).stream().count());
                        }
                        MFHoppers.getInstance().getLogger().info("------------------------");

                        for (HopperEnum en : HopperEnum.values()) {
                            MFHoppers.getInstance().getLogger().info(en.name() + ": " + Methods.getHopperByType(en).stream().count());

                            Map<Chunk, List<IHopper>> chunkHopperLink = Methods.getMapHopperByType(en);
                            for (Chunk chunk : chunkHopperLink.keySet()) {
                                MFHoppers.getInstance().getLogger().info(String.format("Chunk (%s, %d, %d) :", chunk.getWorld(), chunk.getX(), chunk.getZ()));
                                MFHoppers.getInstance().getLogger().info(String.format("\t Entities: %d", chunk.getEntities().length));
                                MFHoppers.getInstance().getLogger().info("\t Hoppers:");
                                for (IHopper hopper : chunkHopperLink.get(chunk)) {
                                    MFHoppers.getInstance().getLogger().info(String.format("\t \t Hopper (%f, %f, %f):", hopper.getLocation().getX(), hopper.getLocation().getY(), hopper.getLocation().getZ()));
                                    MFHoppers.getInstance().getLogger().info(String.format("\t \t \t Name: %s", hopper.getName()));
                                    MFHoppers.getInstance().getLogger().info(String.format("\t \t \t Owner: %s", hopper.getOwner()));
                                    MFHoppers.getInstance().getLogger().info(String.format("\t \t \t Level: %s", hopper.getLevel()));
                                    try {
                                        MFHoppers.getInstance().getLogger().info(String.format("\t \t \t Memory: %s", MemoryMeasurer.measureBytes(hopper)));
                                    } catch (Exception ignored) {
                                    } catch (NoClassDefFoundError ignored) {
                                    }
                                    MFHoppers.getInstance().getLogger().info(String.format("\t \t \t Chunk Loaded: %s", String.valueOf(hopper.isChunkLoaded())));
                                    if (en == HopperEnum.Mob || en == HopperEnum.Crop) {
                                        MFHoppers.getInstance().getLogger().info("\t \t \t Filter:");
                                        for (IHopper.FilterElement element : hopper.getFilterMaterialList()) {
                                            MFHoppers.getInstance().getLogger().info(String.format("\t \t \t \t - %s", element.Material + (element.HasDamageValue ? ":" + String.valueOf(element.DamageValue) : "")));
                                        }
                                    }
                                    if(hopper.isLinked()){
                                        MFHoppers.getInstance().getLogger().info("\t \t \t Linked Chests:");
                                        for (Location loc : hopper.getLinked()) {
                                            boolean isWildChest = Bukkit.getPluginManager().isPluginEnabled("WildChests") && WildChestsAPI.getChest(loc) != null;
                                            MFHoppers.getInstance().getLogger().info(String.format("\t \t \t \t Wildchest: %s Loc: (%s, %f, %f, %f)", String.valueOf(isWildChest), loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
                                        }
                                    }
                                }
                            }
                            MFHoppers.getInstance().getLogger().info("--------");
                        }
                        MFHoppers.getInstance().getLogger().info("--------  MFHopper DUMP END  --------");
                    });
                } else if (cmd.args()[0].equalsIgnoreCase("give")) {

                    if (sender.isPlayer()) {

                        if (!sender.getPlayer().hasPermission("mfh.give")) {
                            Lang.NO_PERMISSION.send(sender);
                            return;
                        }

                    }

                    if (cmd.args().length >= 3) {

                        String stringPlayer = cmd.args()[1];
                        String hopperName = cmd.args()[2];

                        Player player = Bukkit.getPlayer(stringPlayer);
                        if (player == null) {
                            cmd.getSender().sendMessage(c("&c&l(!)&7 Can't find player by name: &c" + stringPlayer));
                            return;
                        }

                        if (configHoppers.containsKey(hopperName)) {

                            ConfigHopper hopper = configHoppers.get(hopperName);
                            int amount = 1;
                            boolean isAuto = false;
                            boolean isGlobal = false;

                            if (cmd.args().length >= 4) {
                                amount = Integer.parseInt(cmd.args()[3]);
                            }
                            if (cmd.args().length >= 5) {
                                isGlobal = Boolean.valueOf(cmd.args()[4]);
                            }
                            if (cmd.args().length == 6) {
                                isAuto = Boolean.valueOf(cmd.args()[5]);
                            }

                            ItemStack item = null;

                            if (hopper.getType() == HopperEnum.Grind) {

                                if (isAuto || isGlobal) {
                                    item = hopper.getItem(isAuto, isGlobal);
                                } else {
                                    item = hopper.getItem();
                                }

                            } else {
                                if (isAuto || isGlobal) {

                                    cmd.getSender().sendMessage(c("&c&l(!)&7 You can't add args &c[IsAuto, IsGlobal] on non grind hoppers!"));
                                    return;

                                }

                                item = hopper.getItem();
                            }
                            item.setAmount(amount);
                            if (player.getInventory().firstEmpty() != -1) {
                                player.getInventory().setItem(player.getInventory().firstEmpty(), item);
                            } else {
                                Methods.drop(item, player.getLocation());
                                Lang.HOPPER_GIVE_INVENTORY_FULL.send(player);
                            }

                            Lang.HOPPER_GIVE.send(new MapBuilder().add("%type%", hopper.getType()).add("%amount%", amount).add("%displayName%", item.getItemMeta().getDisplayName()).add("%name%", hopperName).getMap(), player);

                        } else {
                            cmd.getSender().sendMessage(c("&c&l(!)&7 Can't find hopper by name: " + hopperName));
                        }
                    }

                }

            }
        }, new Function<Cmd, List<String>>() {
            @Override
            public List<String> apply(Cmd cmd) {
                if (cmd.args().length == 2) {
                    return Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).collect(toList());
                } else if (cmd.args().length == 3) {
                    return configHoppers.keySet().stream().filter(s -> s.startsWith(cmd.args()[2])).collect(toList());
                } else if (cmd.args().length == 5) {
                    String name = cmd.args()[2];
                    if (configHoppers.get(name).getType() == HopperEnum.Grind) {
                        String strings[] = {"true", "false"};
                        return Arrays.stream(strings).collect(toList());
                    } else {
                        String strings[] = {"It's not a grind hopper :/"};
                        return Arrays.stream(strings).collect(toList());
                    }
                } else if (cmd.args().length == 6) {
                    String name = cmd.args()[2];
                    if (configHoppers.get(name).getType() == HopperEnum.Grind) {
                        String strings[] = {"true", "false"};
                        return Arrays.stream(strings).collect(toList());
                    } else {
                        String strings[] = {"It's not a grind hopper :/"};
                        return Arrays.stream(strings).collect(toList());
                    }
                }
                return null;
            }
        });
        addCommand("linkHopper", "Link Hoppers to Containers!", "", null, command -> {

            if (command.getSender().isPlayer() && (command.getSender().getPlayer().hasPermission("mfh.linkhopper"))) {

                Player player = command.getSender().getPlayer();
                Block b = player.getTargetBlock(null, 50);

                if (b != null && DataManager.getInstance().isHopper(b.getLocation())) {
                    String owner = DataManager.getInstance().getHopper(b.getLocation()).getOwner();

                    if ((owner != null && owner.contentEquals(player.getName())) || command.getSender().getPlayer().hasPermission("mfh.adminlinkhopper")) {
                        player.setMetadata("link", new FixedMetadataValue(this, Methods.toString(b.getLocation())));
                        Lang.HOPPER_LINK_NOW_SELECT_CONTAINER.send(player);
                    } else {
                        Lang.HOPPER_LINK_NOT_OWNER.send(player);
                    }
                } else {
                    Lang.HOPPER_LINK_MAKE_SURE_TO_LOOK_AT_HOPPER.send(player);
                }
            }

        });
        addCommand("converthopper", "Convert your hoppers!", "", null, command -> {

            if (command.getSender().isPlayer()) {


                Player player = command.getSender().getPlayer();

                if (!player.hasPermission("mfh.convert")) {
                    Lang.NO_PERMISSION.send(player);
                    return;
                }

                ItemStack hand;
                if (OVersion.isAfter(8)) {
                    hand = player.getInventory().getItemInMainHand();
                } else {
                    hand = player.getInventory().getItemInHand();
                }


                final ItemStack handClone = hand.clone();
                int slot = player.getInventory().first(hand);
                final int amount = handClone.getAmount();

                if (hand == null || hand.getType() == Material.AIR || hand.getType() != Material.HOPPER) {
                    Lang.HOPPER_CONVERT_MUST_HOLD.send(player);
                    return;
                }

                NBTItem nbt = new NBTItem(hand);
                if (nbt.hasKey("lvl") && nbt.hasKey("type")) {
                    if (!nbt.getString("type").equalsIgnoreCase("Grind")) {
                        Lang.HOPPER_CONVERT_CAN_ONLY_CONVERT_GRIND_OR_DEFAULT.send(player);
                        return;
                    }

                    boolean isGlobal = Boolean.valueOf(nbt.getString("isGlobal"));
                    if (isGlobal) {
                        Lang.HOPPER_CONVERT_CANT_CONVERT_GLOBAL.send(player);
                        return;
                    }

                    //IT IS A GRIND HOPPER!
                    boolean shouldLevelResetAfterConvert = cnf.getBoolean("GrindConvert.shouldLevelResetAfterConvert");
                    boolean allowConvertUpgradedHoppers = cnf.getBoolean("GrindConvert.allowConvertUpgradedHoppers");

                    List<EntityType> BLACKLIST1 = Methods.toEntityType(cnf.getStringList("GrindConvert.mob-blacklist"));
                    ConfigHopper hopper = configHoppers.get(nbt.getString("name0"));
                    List<EntityType> BLACKLIST2 = Methods.toEntityType((List<String>) hopper.getData().get("mob-blacklist"));
                    EntityType currentType = EntityType.valueOf(nbt.getString("ent"));

                    int defPrice = cnf.getInt("GrindConvert.default-price");
                    Map<EntityType, EntityPrice> prices = EntityPrice.decode(cnf.getStringList("GrindConvert.prices"));

                    InventoryBuilder builder = new InventoryBuilder(cnf.getString("GrindConvert.convert-inventory.title"), 27, InvType.PAGED);
                    builder.setBack(new ItemBuilder(Material.ARROW).setName("&b<<").buildItem());
                    builder.setForward(new ItemBuilder(Material.ARROW).setName("&b>>").buildItem());

                    String placeholderName = cnf.getString("GrindConvert.convert-inventory.item-placeholder.name");
                    List<String> placeholderLore = cnf.getStringList("GrindConvert.convert-inventory.item-placeholder.lore");

                    int level = Integer.valueOf(nbt.getString("lvl"));
                    if (level != 1 && !allowConvertUpgradedHoppers) {
                        Lang.HOPPER_CONVERT_CANT_CONVERT_UPGRADED_HOPPERS.send(player);
                        return;
                    }
                    for (EntityType type : Arrays.stream(EntityType.values()).filter(e -> e.isAlive() && !BLACKLIST1.contains(e) && !BLACKLIST2.contains(e) && e != currentType).sorted(new EntityTypeComparator()).collect(toList())) {
                        if (OVersion.isOrAfter(13)) {
                            if (Textures13.matchEntity(type.name()) != null) {

                                int price = defPrice;
                                UpgradeEnum priceType = UpgradeEnum.ECO;

                                if (prices.containsKey(type)) {
                                    price = prices.get(type).getPrice();
                                    priceType = prices.get(type).getPriceType();
                                }

                                builder.addItem(new ItemBuilder(Textures13.matchEntity(type.name()).getItem()).
                                        addNbt("type", type.name()).
                                        addNbt("price", price).
                                        addNbt("priceType", priceType).
                                        setName(placeholderName.replaceAll("%type%", StringUtils.capitalize(type.name().replace("_", " ").toLowerCase()))).
                                        setLore(placeholderLore, true).
                                        replaceInLore("%price%", String.valueOf(price)).
                                        replaceInLore("%priceType%", String.valueOf(priceType)).
                                        replaceInLore("%type%", StringUtils.capitalize(type.name().replace("_", " ").toLowerCase())).buildItem());

                            }
                        } else {
                            if (Textures12.matchEntity(type.name()) != null) {

                                int price = defPrice;
                                UpgradeEnum priceType = UpgradeEnum.ECO;

                                if (prices.containsKey(type)) {
                                    price = prices.get(type).getPrice();
                                    priceType = prices.get(type).getPriceType();
                                }

                                builder.addItem(new ItemBuilder(Textures12.matchEntity(type.name()).getItem()).
                                        addNbt("type", type.name()).
                                        addNbt("price", String.valueOf(price)).
                                        addNbt("priceType", priceType).
                                        setName(placeholderName.replaceAll("%type%", StringUtils.capitalize(type.name().replace("_", " ").toLowerCase()))).
                                        setLore(placeholderLore, true).
                                        replaceInLore("%price%", String.valueOf(price)).
                                        replaceInLore("%priceType%", String.valueOf(priceType)).
                                        replaceInLore("%type%", StringUtils.capitalize(type.name().replace("_", " ").toLowerCase())).buildItem());

                            }
                        }

                    }
                    builder.setClickListener(event -> {

                        if (event.getClickedInventory().equals(event.getWhoClicked().getOpenInventory().getTopInventory()) && event.getCurrentItem() != null) {

                            event.setCancelled(true);

                            NBTItem nbtitem = new NBTItem(event.getCurrentItem());
                            EntityType type = EntityType.valueOf(nbtitem.getString("type"));
                            double price = Double.valueOf(nbtitem.getString("price"));
                            UpgradeEnum priceType = UpgradeEnum.valueOf(nbtitem.getString("priceType"));

                            if (economy == null && priceType == UpgradeEnum.ECO) {
                                player.sendMessage(c("&c&l(!)&7 Economy is disabled, failed to convert."));
                                return;
                            }

                            price = handClone.getAmount() * price;
                            double currentAmount = 0;

                            switch (priceType) {
                                case ECO:
                                    currentAmount = getEconomy().getBalance(player);
                                    break;
                                default:
                                    currentAmount = ExperienceManager.getTotalExperience(player);
                                    break;
                            }


                            if (!isSimilar(handClone, player.getInventory().getItem(slot))) {
                                player.closeInventory();
                                return;
                            }

                            if (currentAmount >= price) {

                                switch (priceType) {
                                    case ECO:
                                        getEconomy().withdrawPlayer(player, price);
                                        break;
                                    default:
                                        ExperienceManager.setTotalExperience(player, (int) currentAmount - (int) price);
                                }
                                Methods.convertGrind(player, handClone, type, shouldLevelResetAfterConvert, amount);
                                Lang.HOPPER_CONVERT_GRIND_SUCCESSFUL.send(new MapBuilder().add("%type%", StringUtils.capitalize(type.name().replace("_", " ").toLowerCase())).add("%pricetype%", priceType).getMap(), player);
                                event.getWhoClicked().closeInventory();

                            } else {
                                Lang.HOPPER_CONVERT_NOT_ENOUGH_FUNDS_GRIND.send(new MapBuilder().add("%type%", StringUtils.capitalize(type.name().replace("_", " ").toLowerCase())).add("%required%", price).add("%have%", currentAmount).add("%pricetype%", priceType).getMap(), player);
                            }
                        } else {
                            event.setCancelled(true);
                        }

                    });
                    player.openInventory(builder.buildInventory());

                } else {

                    //IS DEFAULT
                    InventoryBuilder builder = new InventoryBuilder(cnf.getString("HopperConvert.title"), 27, InvType.PAGED);
                    builder.setBack(new ItemBuilder(Material.ARROW).setName("&b<<").buildItem());
                    builder.setForward(new ItemBuilder(Material.ARROW).setName("&b>>").buildItem());

                    if (convertHoppers.keySet().stream().filter(it -> configHoppers.containsKey(it)).collect(toList()).isEmpty()) {
                        Lang.CONVERT_HOPPER_CANNOT_FIND_ANY_CONVERT_HOPPERS.send(player);
                        return;
                    }

                    for (HopperConvert hopper : convertHoppers.values()) {

                        ConfigHopper configHopper = configHoppers.get(hopper.getHopperName());
                        int price = hopper.getPrice();

                        ItemStack item = configHopper.getItem();
                        builder.addItem(new ItemBuilder(item).
                                addNbt("price", price).
                                addToLore("").
                                addToLore(cnf.getString("HopperConvert.itemPricePlaceholder").replace("%price%", String.valueOf(price))).
                                buildItem());

                    }
                    builder.setClickListener(event -> {
                        if (event.getClickedInventory().equals(event.getWhoClicked().getOpenInventory().getTopInventory())) {
                            event.setCancelled(true);
                            if (economy == null) {

                                player.sendMessage(c("&c&l(!)&7 Economy is disabled, failed to convert."));
                                return;

                            }

                            NBTItem nbtitem = new NBTItem(event.getCurrentItem());
                            double price = Double.valueOf(nbtitem.getString("price"));
                            double currentMoney = getEconomy().getBalance(player);
                            if (!isSimilar(handClone, player.getInventory().getItem(slot))) {
                                player.closeInventory();
                                return;
                            }

                            price = price * handClone.getAmount();

                            ConfigHopper hopper = configHoppers.get(nbtitem.getString("name0"));
                            String name = hopper.getData().get("name").toString();

                            if (currentMoney >= price) {

                                getEconomy().withdrawPlayer(player, price);

                                ItemStack toGive = hopper.getItem();
                                toGive.setAmount(amount);
                                player.getInventory().setItem(slot, toGive);
                                Lang.HOPPER_CONVERT_DEFAULT_SUCCESSFUL.send(new MapBuilder().add("%name%", name).getMap(), player);

                                event.getWhoClicked().closeInventory();

                            } else {
                                Lang.HOPPER_CONVERT_NOT_ENOUGH_FUNDS_DEFAULT.send(new MapBuilder().add("%name%", name).getMap(), player);
                            }
                        }
                    });
                    player.openInventory(builder.buildInventory());

                }

            }

        });
    }

    void restart() {

        out("&8=-------------------------------------------=");
        out("");
        out("Reloading the plugin...");
        configHoppers.clear();
        cnf = initConfig("config.yml", getDataFolder());
        initConfig();
        Lang.init();
        SellManager.getInstance().restart();
        out("");
        out("&8=-------------------------------------------=");
        out("");


    }

    @Override
    public void onDisable() {

        DataManager.getInstance().end();
        DataManager.getInstance().save(true, true, false);

    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean isSimilar(ItemStack first, ItemStack second) {
        if (first == null || second == null) {
            return false;
        }

        if (first.getType() != second.getType()) {
            return false;
        }

        boolean sameDurability = (first.getDurability() == second.getDurability());
        boolean sameAmount = (first.getAmount() == second.getAmount());
        boolean sameHasItemMeta = (first.hasItemMeta() == second.hasItemMeta());
        boolean sameEnchantments = (first.getEnchantments().equals(second.getEnchantments()));
        boolean sameItemMeta = true;

        if (sameHasItemMeta) {
            sameItemMeta = Bukkit.getItemFactory().equals(first.getItemMeta(), second.getItemMeta());
        }

        if (sameDurability && sameAmount && sameHasItemMeta && sameEnchantments && sameItemMeta) {
            return true;
        }

        return false;

    }

    public void updateConfig() {

        //cnf.addDefault();

        cnf.options().copyDefaults(true);
        try {
            cnf.save(new File(getDataFolder(), "config.yml"));
        } catch (Exception ex) {

        }
        cnf = initConfig("config.yml", getDataFolder());

    }

    private void PlaceHopper(BlockPlaceEvent event, ItemStack item) {
        NBTItem nbt = new NBTItem(item);
        Map<String, Object> data = new HashMap<>();
        data.put("type", nbt.getString("type"));
        data.put("name", nbt.getString("name0"));
        data.put("lvl", nbt.getString("lvl"));

        if (!Methods.hasReachedLimit(data, event.getBlock().getLocation().getChunk(), event.getPlayer())) {
            DataManager.getInstance().add(item, event.getBlock().getLocation(), event.getPlayer());
            Lang.PLACE.send(new MapBuilder().add("%type%", nbt.getString("type")).add("%lvl%", nbt.getString("lvl")).add("%name%", nbt.getString("name0")).add("%displayName%", event.getItemInHand().getItemMeta().getDisplayName()).getMap(), event.getPlayer());

        } else {
            if(OVersion.isOrAfter(15)){
                event.setCancelled(true);
            }
            else {
                Methods.breakBlock(event.getBlock());
                if (nbt.getString("type").equalsIgnoreCase(HopperEnum.Grind.toString())) {
                    event.getPlayer().getInventory().addItem(configHoppers.get(nbt.getString("name0")).buildItemByLevel(Integer.valueOf(nbt.getString("lvl")), EntityType.valueOf(nbt.getString("ent")), Boolean.valueOf(nbt.getString("isAuto")), Boolean.valueOf(nbt.getString("isGlobal"))));
                } else {
                    event.getPlayer().getInventory().addItem(configHoppers.get(nbt.getString("name0")).buildItemByLevel(Integer.valueOf(nbt.getString("lvl"))));
                }
            }
        }
    }

}
