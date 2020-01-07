package net.squidstudios.mfhoppers.manager;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.sell.ISell;
import net.squidstudios.mfhoppers.sell.implementation.BuiltInSell;
import net.squidstudios.mfhoppers.sell.implementation.ShopGuiPlus;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellManager {

    private ISell currentSystem;

    public static SellManager getInstance() {
        return instance;
    }

    private static SellManager instance;
    private boolean loaded = false;

    private Map<String, ISell> loadedSellSystems = new HashMap<>();

    public SellManager(){

        if(instance != null) return;
        instance = this;
        YamlConfiguration config = MFHoppers.getInstance().cnf;

        new ShopGuiPlus();

        loadDefault();

        String name = config.getString("sellOptions.system");

        new BukkitRunnable() {
            @Override
            public void run() {

                if(loadedSellSystems.containsKey(name)){

                    currentSystem = loadedSellSystems.get(name);
                    loaded = true;
                    MFHoppers.getInstance().out("Sell system hooked! Using " + name + " sell system!", PluginBuilder.OutType.WITH_PREFIX);

                } else {
                    MFHoppers.getInstance().out("&cCannot find a sell system named: " + name + ", using default: BuiltIn", PluginBuilder.OutType.ERROR);
                    MFHoppers.getInstance().out("Available sell systems: " + loadedSellSystems.keySet().toString().replace("[", "").replace("[", ""), PluginBuilder.OutType.ERROR);


                }

            }
        }.runTaskLater(MFHoppers.getInstance(), 20);
    }

    public double getPrice(ItemStack item, Player player){
        if(!loaded) return 0.0;

        return currentSystem.getPrice(item, player);
    }

    public void loadDefault(){

        File pricesFile = new File(MFHoppers.getInstance().getDataFolder(), "prices.yml");

        if(!pricesFile.exists()){

            MFHoppers.getInstance().saveResource("prices.yml", true);

        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(pricesFile);
        new BuiltInSell(config.getStringList("prices"));

    }

    public void add(ISell iSell) {

        if(loadedSellSystems.containsKey(iSell.getName())){

            throw new ConcurrentModificationException("Tried to replace a sell system named: " + iSell.getName());

        }

        loadedSellSystems.put(iSell.getName(), iSell);
    }
    public void restart(){

        loadedSellSystems.remove("BuiltIn");

        YamlConfiguration config = MFHoppers.getInstance().cnf;

        loadDefault();

        String name = config.getString("sellOptions.system");

        new BukkitRunnable() {
            @Override
            public void run() {

                if(loadedSellSystems.containsKey(name)){

                    currentSystem = loadedSellSystems.get(name);
                    loaded = true;
                    MFHoppers.getInstance().out("Sell system hooked! Using " + name + " sell system!", PluginBuilder.OutType.WITH_PREFIX);

                } else {
                    MFHoppers.getInstance().out("&cCannot find a sell system named: " + name + ", using default: BuiltIn", PluginBuilder.OutType.ERROR);
                    MFHoppers.getInstance().out("Available sell systems: " + loadedSellSystems.keySet().toString().replace("[", "").replace("[", ""), PluginBuilder.OutType.ERROR);


                }

            }
        }.runTaskLater(MFHoppers.getInstance(), 20);
    }
}
