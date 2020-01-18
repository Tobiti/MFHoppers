package net.squidstudios.mfhoppers.manager;

import com.google.common.collect.Sets;
import de.tr7zw.changeme.nbtapi.NBTItem;
import lombok.NonNull;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.hopper.UnloadedHopper;
import net.squidstudios.mfhoppers.hopper.types.BreakHopper;
import net.squidstudios.mfhoppers.hopper.types.CropHopper;
import net.squidstudios.mfhoppers.hopper.types.GrindHopper;
import net.squidstudios.mfhoppers.hopper.types.MobHopper;
import net.squidstudios.mfhoppers.util.MChunk;
import net.squidstudios.mfhoppers.util.Methods;
import net.squidstudios.mfhoppers.util.hopperMap.HopperDataHandler;
import net.squidstudios.mfhoppers.util.hopperMap.HopperMap;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataManager {

    public static final HopperDataHandler data_handler = new HopperDataHandler() {
        @Override
        public void addNew(IHopper hopper) {
            getInstance().addedHopperQueue.remove(hopper);
            getInstance().addedHopperQueue.add(hopper);
        }

        @Override
        public void remove(IHopper hopper) {
            Queue<IHopper> added = getInstance().addedHopperQueue;
            ConcurrentLinkedQueue<IHopper> removed = getInstance().removedHopperQueue;
            ConcurrentLinkedQueue<IHopper> updated = getInstance().updatedHopperQueue;

            added.remove(hopper);
            updated.remove(hopper);

            if (!removed.contains(hopper)) {
                removed.offer(hopper);
            }
        }
    };

    private Map<String, WorldHolder> worldHolders = new ConcurrentHashMap<>();

    private PluginBuilder plugin;
    private BukkitTask saveTask;
    private ConnectionManager connectionManager;
    private static DataManager instance;

    private ConcurrentLinkedQueue<IHopper> addedHopperQueue = new ConcurrentLinkedQueue<>(), removedHopperQueue = new ConcurrentLinkedQueue<>(), updatedHopperQueue = new ConcurrentLinkedQueue<>();

    public DataManager(PluginBuilder builder) {

        Bukkit.getPluginManager().registerEvents(new WorldManager(this), builder);
        this.plugin = builder;
        instance = this;
        File file = new File(MFHoppers.getInstance().getDataFolder(), "data.db");
        if (!file.exists()) {
            this.connectionManager = new ConnectionManager(this);
            connectionManager.run("CREATE TABLE IF NOT EXISTS Grind (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, ent VARCHAR(255), isAuto BOOLEAN, isGlobal BOOLEAN, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Break (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Crop (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Mob (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            plugin.out("&cDatabase wasn't found, creating...");
        } else {
            this.connectionManager = new ConnectionManager(this);
            connectionManager.run("CREATE TABLE IF NOT EXISTS Grind (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, ent VARCHAR(255), isAuto BOOLEAN, isGlobal BOOLEAN, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Break (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Crop (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Mob (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            checkForMigration(true);
            load();
            MFHoppers.getInstance().getLogger().info("Database was loaded!!!");
        }
        startSaveTask();
    }

    public static DataManager getInstance() {
        return instance;
    }

    public void add(IHopper hopper, boolean newHopper) {
        Location location = hopper.getLocation();
        HopperMap hopperMap = insertIfAbsent(Objects.requireNonNull(Objects.requireNonNull(location, "Location cannot be null!").getWorld(), "World cannot be null!").getName()).insertIfAbsent(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        if (newHopper)
            hopperMap.putNew(location, hopper);
        else
            hopperMap.put(location, hopper);
    }

    public void remove(IHopper hopper) {
        remove(hopper.getLocation());
    }

    public void remove(Location location) {
        IHopper[] hopper = new IHopper[]{null};
        worldHolder(location).flatMap(holder -> holder.hoppersMapAt(location.getBlockX() >> 4, location.getBlockZ() >> 4)).ifPresent(map -> {
            hopper[0] = map.get(location);
            map.remove(location);
        });
        if (hopper[0] == null) return;

        IHopper removedHopper = hopper[0];
        if (removedHopper.getType() == HopperEnum.Grind) {
            MFHoppers.getInstance().taskManager.RemoveGrindHopper(removedHopper);
        }
    }

    public void update(IHopper hopper) {
        updatedHopperQueue.remove(hopper);
        updatedHopperQueue.add(hopper);
    }

    public boolean containsHoppersChunk(Chunk chunk) {
        boolean[] contains = new boolean[]{false};
        worldHolder(chunk).ifPresent(holder -> contains[0] = holder.hoppersMapAt(chunk.getX(), chunk.getZ()).isPresent());
        return contains[0];
    }

    public void checkForMigration(boolean sync) {

        File file = new File(MFHoppers.getInstance().getDataFolder(), "data.sql");
        if (!file.exists()) {
            plugin.out("&3Database is up to date, no migration needed!");
            return;
        }

        if (sync) {

            if (connectionManager.hasColumn("Mob", "data", "data.sql")) {
                plugin.out("&3Database is up to date, no migration needed!");
                connectionManager.closeAll();
                return;

            } else {

                plugin.out("&4Database is out of date, getting ready for migration!");
                connectionManager.closeAll();

            }

            plugin.out("");
            long then = System.currentTimeMillis();
            plugin.out("&3MIGRATION -> (STARTED)");
            plugin.out("");
            plugin.out("&b= &7CONVERTING HOPPERS...");

            Map<HopperEnum, HashMap<Integer, HashMap<String, Object>>> oldHoppers = new HashMap<>();
            for (HopperEnum henum : HopperEnum.values()) {

                oldHoppers.put(henum, connectionManager.getAllRows(henum.name(), "data.sql"));

            }
            connectionManager.destroy("data.sql");

            connectionManager.run("CREATE TABLE IF NOT EXISTS Grind (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, ent VARCHAR(255), isAuto BOOLEAN, isGlobal BOOLEAN, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Break (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Crop (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
            connectionManager.run("CREATE TABLE IF NOT EXISTS Mob (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");

            for (int id : oldHoppers.get(HopperEnum.Grind).keySet()) {

                HashMap<String, Object> data = oldHoppers.get(HopperEnum.Grind).get(id);

                boolean isAuto = Boolean.valueOf(data.get("isAuto").toString());
                boolean isGlobal = Boolean.valueOf(data.get("isGlobal").toString());
                Location loc = Methods.toLocation(data.get("loc").toString());
                EntityType type = null;

                if (data.containsKey("entity")) {
                    type = EntityType.valueOf(data.get("entity").toString());
                } else {
                    type = EntityType.valueOf(data.get("ent").toString());
                }
                String name = data.get("name").toString();
                add(new GrindHopper(loc, name, 1, type, isAuto, isGlobal), false);
                plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Grind);

            }
            for (int id : oldHoppers.get(HopperEnum.Mob).keySet()) {

                HashMap<String, Object> data = oldHoppers.get(HopperEnum.Mob).get(id);

                String name = data.get("name").toString();
                Location loc = Methods.toLocation(data.get("loc").toString());
                add(new MobHopper(loc, name, 1), false);
                plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Mob);

            }
            for (int id : oldHoppers.get(HopperEnum.Crop).keySet()) {

                HashMap<String, Object> data = oldHoppers.get(HopperEnum.Crop).get(id);

                String name = data.get("name").toString();
                Location loc = Methods.toLocation(data.get("loc").toString());
                add(new CropHopper(loc, name, 1), false);
                plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Crop);

            }
            for (int id : oldHoppers.get(HopperEnum.Break).keySet()) {

                HashMap<String, Object> data = oldHoppers.get(HopperEnum.Break).get(id);

                String name = data.get("name").toString();
                Location loc = Methods.toLocation(data.get("loc").toString());
                add(new BreakHopper(loc, name, 1), false);
                plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Break);

            }
            plugin.out("");
            plugin.out("&3MIGRATION -> (SUCCEED) TOOK " + (System.currentTimeMillis() - then) + "ms");
            connectionManager.closeAll();

        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (connectionManager.hasColumn("Mob", "data")) {
                        plugin.out("&3Database is up to date, no migration needed!");
                        connectionManager.closeAll();
                        return;

                    } else {

                        plugin.out("&4Database is out of date, getting ready for migration!");
                        connectionManager.closeAll();

                    }

                    plugin.out("");
                    long then = System.currentTimeMillis();
                    plugin.out("&3MIGRATION -> (STARTED)");
                    plugin.out("");
                    plugin.out("&b= &7CONVERTING HOPPERS...");

                    Map<HopperEnum, HashMap<Integer, HashMap<String, Object>>> oldHoppers = new HashMap<>();
                    for (HopperEnum henum : HopperEnum.values()) {

                        oldHoppers.put(henum, connectionManager.getAllRows(henum.name(), "data.sql"));

                    }
                    connectionManager.destroy("data");

                    connectionManager.run("CREATE TABLE IF NOT EXISTS Grind (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, ent VARCHAR(255), isAuto BOOLEAN, isGlobal BOOLEAN, data varchar(255))");
                    connectionManager.run("CREATE TABLE IF NOT EXISTS Break (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
                    connectionManager.run("CREATE TABLE IF NOT EXISTS Crop (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");
                    connectionManager.run("CREATE TABLE IF NOT EXISTS Mob (id INTEGER PRIMARY KEY AUTOINCREMENT, name varchar(255), loc varchar(255), lvl INTEGER, data varchar(255))");

                    for (int id : oldHoppers.get(HopperEnum.Grind).keySet()) {

                        HashMap<String, Object> data = oldHoppers.get(HopperEnum.Grind).get(id);

                        boolean isAuto = Boolean.valueOf(data.get("isAuto").toString());
                        boolean isGlobal = Boolean.valueOf(data.get("isGlobal").toString());
                        Location loc = Methods.toLocation(data.get("loc").toString());
                        EntityType type = null;

                        if (data.containsKey("entity")) {
                            type = EntityType.valueOf(data.get("entity").toString());
                        } else {
                            type = EntityType.valueOf(data.get("ent").toString());
                        }
                        String name = data.get("name").toString();
                        add(new GrindHopper(loc, name, 1, type, isAuto, isGlobal), false);
                        plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Grind);

                    }
                    for (int id : oldHoppers.get(HopperEnum.Mob).keySet()) {

                        HashMap<String, Object> data = oldHoppers.get(HopperEnum.Mob).get(id);

                        String name = data.get("name").toString();
                        Location loc = Methods.toLocation(data.get("loc").toString());
                        add(new MobHopper(loc, name, 1), false);
                        plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Mob);

                    }
                    for (int id : oldHoppers.get(HopperEnum.Crop).keySet()) {

                        HashMap<String, Object> data = oldHoppers.get(HopperEnum.Crop).get(id);

                        String name = data.get("name").toString();
                        Location loc = Methods.toLocation(data.get("loc").toString());
                        add(new CropHopper(loc, name, 1), false);
                        plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Crop);

                    }
                    for (int id : oldHoppers.get(HopperEnum.Break).keySet()) {

                        HashMap<String, Object> data = oldHoppers.get(HopperEnum.Break).get(id);

                        String name = data.get("name").toString();
                        Location loc = Methods.toLocation(data.get("loc").toString());
                        add(new BreakHopper(loc, name, 1), false);
                        plugin.out(" &b->&7 Converted hopper by id: &3" + id + "&7 and type: &3" + HopperEnum.Break);

                    }
                    plugin.out("");
                    plugin.out("&3MIGRATION -> (SUCCEED) TOOK " + (System.currentTimeMillis() - then) + "ms");
                    connectionManager.closeAll();
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    public void save(boolean sync, boolean backup, boolean clean) {
        if (sync) {
            SaveTask(backup, clean);

        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    SaveTask(backup, clean);
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    private void SaveTask(boolean backup, boolean cleanSave) {
        if (updatedHopperQueue.isEmpty() && addedHopperQueue.isEmpty() && removedHopperQueue.isEmpty()) return;

        //MFHoppers.getInstance().getLogger().info("Saving started.");

        if (backup) {
            try {
                MFHoppers.getInstance().getLogger().warning("Database Backup was created!");
                Files.copy(new File(MFHoppers.getInstance().getDataFolder(), "data.db").toPath(), new File(MFHoppers.getInstance().getDataFolder(), "data-backup.db").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                MFHoppers.getInstance().getLogger().warning("Database Backup couldn't be created!");
            }
        }

        long then = System.currentTimeMillis();
        try (Connection connection = connectionManager.getConnection()) {
            if (cleanSave) {
                connectionManager.run("DELETE FROM Grind");
                connectionManager.run("DELETE FROM Crop");
                connectionManager.run("DELETE FROM Break");
                connectionManager.run("DELETE FROM Mob");
                connectionManager.run("VACUUM");
            } else {
                PreparedStatement grindDeleteStat = connection.prepareStatement("DELETE FROM Grind WHERE name = ? AND loc = ?");
                PreparedStatement breakDeleteStat = connection.prepareStatement("DELETE FROM Break WHERE name = ? AND loc = ?");
                PreparedStatement cropDeleteStat = connection.prepareStatement("DELETE FROM Crop WHERE name = ? AND loc = ?");
                PreparedStatement mobDeleteStat = connection.prepareStatement("DELETE FROM Mob WHERE name = ? AND loc = ?");

                while (!removedHopperQueue.isEmpty()) {
                    IHopper hopper = removedHopperQueue.poll();
                    if (hopper != null) {
                        if (hopper.getType() == HopperEnum.Break) {
                            breakDeleteStat.setString(1, hopper.getData().get("name").toString());
                            breakDeleteStat.setString(2, hopper.getData().get("loc").toString());
                            breakDeleteStat.addBatch();
                        } else if (hopper.getType() == HopperEnum.Grind) {
                            grindDeleteStat.setString(1, hopper.getData().get("name").toString());
                            grindDeleteStat.setString(2, hopper.getData().get("loc").toString());
                            grindDeleteStat.addBatch();
                        } else if (hopper.getType() == HopperEnum.Mob) {
                            mobDeleteStat.setString(1, hopper.getData().get("name").toString());
                            mobDeleteStat.setString(2, hopper.getData().get("loc").toString());
                            mobDeleteStat.addBatch();
                        } else if (hopper.getType() == HopperEnum.Crop) {
                            cropDeleteStat.setString(1, hopper.getData().get("name").toString());
                            cropDeleteStat.setString(2, hopper.getData().get("loc").toString());
                            cropDeleteStat.addBatch();
                        }
                    }
                }

                grindDeleteStat.executeBatch();
                breakDeleteStat.executeBatch();
                cropDeleteStat.executeBatch();
                mobDeleteStat.executeBatch();
                grindDeleteStat.close();
                breakDeleteStat.close();
                cropDeleteStat.close();
                mobDeleteStat.close();
            }

            PreparedStatement grindInsertStat;
            PreparedStatement breakInsertStat;
            PreparedStatement cropInsertStat;
            PreparedStatement mobInsertStat;

            if (cleanSave) {
                grindInsertStat = connection.prepareStatement("INSERT INTO Grind (name,loc,lvl,ent,isAuto,isGlobal,data) VALUES(?,?,?,?,?,?,?)");
                breakInsertStat = connection.prepareStatement("INSERT INTO Break (name,loc,lvl,data) VALUES(?,?,?,?)");
                cropInsertStat = connection.prepareStatement("INSERT INTO Crop (name,loc,lvl,data) VALUES(?,?,?,?)");
                mobInsertStat = connection.prepareStatement("INSERT INTO Mob (name,loc,lvl,data) VALUES(?,?,?,?)");

                getHoppersSet().forEach(hopper -> {
                    if (hopper != null) {
                        if (hopper.getType() == HopperEnum.Break) {
                            hopper.save(breakInsertStat);
                        } else if (hopper.getType() == HopperEnum.Grind) {
                            hopper.save(grindInsertStat);
                        } else if (hopper.getType() == HopperEnum.Mob) {
                            hopper.save(mobInsertStat);
                        } else if (hopper.getType() == HopperEnum.Crop) {
                            hopper.save(cropInsertStat);
                        }
                    }
                });
            } else {
                grindInsertStat = connection.prepareStatement("INSERT INTO Grind (name,loc,lvl,ent,isAuto,isGlobal,data) VALUES(?,?,?,?,?,?,?)");
                breakInsertStat = connection.prepareStatement("INSERT INTO Break (name,loc,lvl,data) VALUES(?,?,?,?)");
                cropInsertStat = connection.prepareStatement("INSERT INTO Crop (name,loc,lvl,data) VALUES(?,?,?,?)");
                mobInsertStat = connection.prepareStatement("INSERT INTO Mob (name,loc,lvl,data) VALUES(?,?,?,?)");

                while (!addedHopperQueue.isEmpty()) {
                    IHopper hopper = addedHopperQueue.poll();
                    if (hopper != null) {
                        if (hopper.getType() == HopperEnum.Break) {
                            hopper.save(breakInsertStat);
                        } else if (hopper.getType() == HopperEnum.Grind) {
                            hopper.save(grindInsertStat);
                        } else if (hopper.getType() == HopperEnum.Mob) {
                            hopper.save(mobInsertStat);
                        } else if (hopper.getType() == HopperEnum.Crop) {
                            hopper.save(cropInsertStat);
                        }
                    }
                }
            }

            grindInsertStat.executeBatch();
            breakInsertStat.executeBatch();
            mobInsertStat.executeBatch();
            cropInsertStat.executeBatch();
            grindInsertStat.close();
            breakInsertStat.close();
            mobInsertStat.close();
            cropInsertStat.close();

            if (!cleanSave) {
                PreparedStatement grindUpdateStat = connection.prepareStatement("UPDATE Grind SET name = ?,loc = ?,lvl = ?,ent = ?,isAuto = ?,isGlobal = ?,data = ? WHERE name = ? AND loc = ?");
                PreparedStatement breakUpdateStat = connection.prepareStatement("UPDATE Break SET name = ?,loc = ?,lvl = ?,data = ? WHERE name = ? AND loc = ?");
                PreparedStatement cropUpdateStat = connection.prepareStatement("UPDATE Crop SET name = ?,loc = ?,lvl = ?,data = ? WHERE name = ? AND loc = ?");
                PreparedStatement mobUpdateStat = connection.prepareStatement("UPDATE Mob SET name = ?,loc = ?,lvl = ?,data = ? WHERE name = ? AND loc = ?");

                while (!updatedHopperQueue.isEmpty()) {
                    IHopper hopper = updatedHopperQueue.poll();
                    if (hopper != null) {
                        if (hopper.getType() == HopperEnum.Break) {
                            try {
                                breakUpdateStat.setString(5, hopper.getData().get("name").toString());
                                breakUpdateStat.setString(6, hopper.getData().get("loc").toString());
                                hopper.save(breakUpdateStat);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        } else if (hopper.getType() == HopperEnum.Grind) {
                            try {
                                grindUpdateStat.setString(8, hopper.getData().get("name").toString());
                                grindUpdateStat.setString(9, hopper.getData().get("loc").toString());
                                hopper.save(grindUpdateStat);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        } else if (hopper.getType() == HopperEnum.Mob) {
                            try {
                                mobUpdateStat.setString(5, hopper.getData().get("name").toString());
                                mobUpdateStat.setString(6, hopper.getData().get("loc").toString());
                                hopper.save(mobUpdateStat);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        } else if (hopper.getType() == HopperEnum.Crop) {
                            try {
                                cropUpdateStat.setString(5, hopper.getData().get("name").toString());
                                cropUpdateStat.setString(6, hopper.getData().get("loc").toString());
                                hopper.save(cropUpdateStat);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                grindUpdateStat.executeBatch();
                breakUpdateStat.executeBatch();
                cropUpdateStat.executeBatch();
                mobUpdateStat.executeBatch();
                grindUpdateStat.close();
                breakUpdateStat.close();
                cropUpdateStat.close();
                mobUpdateStat.close();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long now = System.currentTimeMillis();
        MFHoppers.getInstance().getLogger().info("Saving finished. Took " + (now - then) + "ms");
    }

    public boolean isHopper(Location loc) {
        boolean[] isHopper = new boolean[]{false};
        worldHolder(loc).ifPresent(holder -> isHopper[0] = holder.hopperAt(loc).isPresent());
        return isHopper[0];
    }

    public IHopper getHopper(Location loc) {
        IHopper[] hopper = new IHopper[]{null};
        worldHolder(loc).ifPresent(holder -> hopper[0] = holder.hopperAt(loc).get());
        return hopper[0];
    }

    public void add(ItemStack item, Location loc, Player p) {

        NBTItem nbt = new NBTItem(item);

        HopperEnum type = HopperEnum.match(nbt.getString("type"));
        String name = nbt.getString("name0");
        int lvl = Integer.valueOf(nbt.getString("lvl")) != null ? Integer.valueOf(nbt.getString("lvl")) : 1;

        IHopper hopper = null;

        if (type == HopperEnum.Mob) {
            hopper = new MobHopper(loc, name, lvl);
        } else if (type == HopperEnum.Crop) {
            hopper = new CropHopper(loc, name, lvl);
        } else if (type == HopperEnum.Grind) {
            EntityType ent = EntityType.valueOf(nbt.getString("ent"));
            hopper = new GrindHopper(loc, name, lvl, ent, Boolean.valueOf(nbt.getString("isAuto")), Boolean.valueOf(nbt.getString("isGlobal")));
        } else if (type == HopperEnum.Break) {
            hopper = new BreakHopper(loc, name, lvl);
        }
        hopper.getData().put("owner", p.getName());
        add(hopper, true);
    }

    public void load() {
        Connection connection = connectionManager.getConnection();

        for (HopperEnum en : HopperEnum.values()) {

            Map<Integer, HashMap<String, Object>> hoppers = connectionManager.getAllRows(en.name(), "data.db");
            plugin.out("&3Try to load " + hoppers.size() + " " + en.name() + "hoppers!", PluginBuilder.OutType.WITHOUT_PREFIX);

            for (int id : hoppers.keySet()) {

                HashMap<String, Object> data = hoppers.get(id);
                String worldName = Methods.worldName(data.get("loc").toString());

                if (Bukkit.getWorld(worldName) == null) {
                    WorldManager.getInstance().add(new UnloadedHopper(data, worldName, en));
                    continue;
                }
                Location loc = Methods.toLocation(data.get("loc").toString());

                String name = data.get("name").toString();
                Map<String, Object> data2 = Methods.deserialize(data.get("data").toString());

                if (!MFHoppers.getInstance().getConfigHoppers().containsKey(name) || loc.getBlock().getType() != Material.HOPPER) {
                    try {
                        PreparedStatement deleteStat = connection.prepareStatement("DELETE FROM Grind WHERE name = ? AND loc = ?");

                        switch (en) {
                            case Break:
                                deleteStat = connection.prepareStatement("DELETE FROM Break WHERE name = ? AND loc = ?");
                                break;
                            case Crop:
                                deleteStat = connection.prepareStatement("DELETE FROM Crop WHERE name = ? AND loc = ?");
                                break;
                            case Mob:
                                deleteStat = connection.prepareStatement("DELETE FROM Mob WHERE name = ? AND loc = ?");
                                break;
                        }

                        deleteStat.setString(1, name);
                        deleteStat.setString(2, data.get("loc").toString());

                        deleteStat.execute();
                        deleteStat.close();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (data2.containsKey("linked")) {

                    Object linked = data2.get("linked");

                    if (Methods.toLocation(linked.toString()) == null) {

                        List<String> locations = (List<String>) linked;
                        data2.remove("linked");
                        data2.put("linked", locations);

                    } else {

                        List<String> locations = new ArrayList<>();
                        locations.add(linked.toString());
                        data2.remove("linked");
                        data2.put("linked", locations);

                    }

                }

                int level = (int) data.get("lvl");

                if (en == HopperEnum.Grind) {

                    boolean isAuto = (int) data.get("isAuto") == 1;
                    boolean isGlobal = (int) data.get("isGlobal") == 1;
                    EntityType type = EntityType.valueOf(data.get("ent").toString());

                    add(new GrindHopper(loc, name, level, type, isAuto, isGlobal, data2), false);

                } else if (en == HopperEnum.Break) {
                    add(new BreakHopper(loc, name, level, data2), false);
                } else if (en == HopperEnum.Crop) {
                    add(new CropHopper(loc, name, level, data2), false);
                } else if (en == HopperEnum.Mob) {
                    add(new MobHopper(loc, name, level, data2), false);
                }

            }
        }
        int count = getHoppersSet().size();
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        plugin.out("&3Loaded (" + count + ") hoppers!", PluginBuilder.OutType.WITHOUT_PREFIX);

    }

    public void startSaveTask() {
        plugin.out("&3Started the auto save task!", PluginBuilder.OutType.WITHOUT_PREFIX);
        saveTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    save(false, false, false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, MFHoppers.getInstance().cnf.getInt("saveEvery") * 20, MFHoppers.getInstance().cnf.getInt("saveEvery") * 20);
    }

    public void end() {
        if (saveTask != null) {
            saveTask.cancel();
        }
    }

    public void link(Location hopperloc, Location loc) {

        IHopper hopper = getHopper(hopperloc);
        hopper.link(loc);

    }

    public Set<IHopper> getHoppersSet() {
        Set<IHopper> hoppers = new HashSet<>();
        worldHolders.values().forEach(holder -> hoppers.addAll(holder.hoppers()));
        return hoppers;
    }

    public Set<IHopper> getHoppersSet(Predicate<IHopper> filter) {
        Set<IHopper> hoppers = Sets.newHashSet();
        worldHolders.values().forEach(holder -> hoppers.addAll(holder.hoppers(filter)));
        return hoppers;
    }

    public Optional<WorldHolder> worldHolder(@NonNull Location location) {
        return worldHolder(Objects.requireNonNull(location.getWorld(), "World cannot be null!").getName());
    }

    public Optional<WorldHolder> worldHolder(@NonNull Chunk chunk) {
        return worldHolder(chunk.getWorld().getName());
    }

    public Optional<WorldHolder> worldHolder(String worldName) {
        return Optional.ofNullable(worldHolders.get(worldName));
    }

    public WorldHolder insertIfAbsent(String worldName) {
        return worldHolder(worldName).orElseGet(() -> {
            WorldHolder holder = new WorldHolder();
            worldHolders.put(worldName, holder);
            return holder;
        });
    }

    public static class WorldHolder {
        private Map<MChunk, HopperMap> hoppers = new ConcurrentHashMap<>();

        public Collection<IHopper> hoppersAt(int x, int z) {
            HopperMap hopperMap = hoppers.get(new MChunk(x, z));
            if (hopperMap == null)
                return Sets.newHashSet();

            return hopperMap.values();
        }

        public Collection<IHopper> hoppersAt(int x, int z, Predicate<IHopper> filter) {
            HopperMap hopperMap = hoppers.get(new MChunk(x, z));
            if (hopperMap == null)
                return Sets.newHashSet();

            return hopperMap.values().stream().filter(filter).collect(Collectors.toSet());
        }

        public Optional<HopperMap> hoppersMapAt(int x, int z) {
            return Optional.ofNullable(hoppers.get(new MChunk(x, z)));
        }

        public Collection<IHopper> hoppersAt(Chunk chunk) {
            return hoppersAt(chunk.getX(), chunk.getZ());
        }

        public Collection<IHopper> hoppersAt(Chunk chunk, Predicate<IHopper> filter) {
            return hoppersAt(chunk.getX(), chunk.getZ(), filter);
        }

        public Collection<IHopper> hoppers() {
            Set<IHopper> finalHoppers = Sets.newHashSet();
            hoppers.values().forEach(map -> finalHoppers.addAll(map.values()));
            return finalHoppers;
        }

        public Set<IHopper> hoppers(Predicate<IHopper> filter) {
            Set<IHopper> finalHoppers = Sets.newHashSet();
            hoppers.values().forEach(map -> finalHoppers.addAll(map.values().stream().filter(filter).collect(Collectors.toSet())));
            return finalHoppers;
        }

        public Optional<IHopper> hopperAt(Location location) {
            Optional<HopperMap> hopperMap = hoppersMapAt(location.getBlockX() >> 4, location.getBlockZ() >> 4);
            if (!hopperMap.isPresent()) return Optional.empty();

            HopperMap map = hopperMap.get();
            return Optional.ofNullable(map.get(location));
        }

        public HopperMap insertIfAbsent(int x, int z) {
            return hoppers.computeIfAbsent(new MChunk(x, z), chunk -> new HopperMap());
        }
    }

    public ConcurrentLinkedQueue<IHopper> getUpdatedHopperQueue() {
        return updatedHopperQueue;
    }
}
