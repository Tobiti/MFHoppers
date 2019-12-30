package net.squidstudios.mfhoppers.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.squidstudios.mfhoppers.MFHoppers;

import java.io.File;
import java.util.Map;

public enum Lang {

    PLACE("&b&l(!)&7 You have successfully placed a hopper by type: &b%type% &7by lvl: &b%lvl%"),
    BROKE("&b&l(!)&7 You have successfully broke an hopper by type: &b%type% &7by lvl: &b%lvl%"),
    HOPPER_IS_NOT_UPGRADABLE("&c&l(!)&7 hopper is not upgradable!"),
    HOPPER_NOT_ENOUGH_VALUE_TO_UPGRADE("&c&l(!)&7 You don't have enough &c%type%&7 to upgrade hopper! (&c%needed%&8/&c%current%&8/&c%missing%&7)"),
    HOPPER_UPGRADED("&b&l(!)&7 You successfully upgraded your hopper from level &b%lastlvl% &7to &b%newlvl%"),
    HOPPER_ALREADY_IS_MAX_LEVEL("&c&l(!)&7 The hopper is already maxed out!"),
    HOPPER_LINK_MAKE_SURE_TO_LOOK_AT_HOPPER("&c&l(!)&7 Make sure you're looking at hopper to link it!"),
    HOPPER_LINK_NOW_SELECT_CONTAINER("&b&l(!)&7 Hopper location set, now shift click on an container!"),
    HOPPER_LINK_NOT_OWNER("&c&l(!)&7 You aren't the owner of the hopper and can't connect it to a other object!"),
    HOPPER_LINK_SUCCESSFULLY_LINKED("&b&l(!)&7 Your hopper has been successfully linked!"),
    HOPPER_LINK_CLICKED_BLOCK_IS_NOT_CONTAINER("&c&l(!)&7 Your clicked block isn't an container!"),
    HOPPER_GIVE("&b&l(!)&7 You have received a x%amount% of &b%type% &7hopper!"),
    HOPPER_LIMIT_REACHED("&c&l(!)&7 You have reached chunk limit of hopper &c(%name%, %type%, %limit%, %level%)"),
    HOPPER_CONVERT_MUST_HOLD("&c&l(!)&7 Please hold a hopper in your hand in order to convert it!"),
    HOPPER_CONVERT_CAN_ONLY_CONVERT_GRIND_OR_DEFAULT("&c&l(!)&7 You can only convert &cDefault &7or &cGrind hoppers!"),
    HOPPER_CONVERT_CANT_CONVERT_UPGRADED_HOPPERS("&c&l(!)&7 You can't convert upgraded hopper!"),
    HOPPER_CONVERT_NOT_ENOUGH_FUNDS_GRIND("&c&l(!)&7 You don't have enough money to convert your hopper to &c%type% &7(&c%required%&7/&c%have%)"),
    HOPPER_CONVERT_GRIND_SUCCESSFUL("&b&l(!)&7 Successfully converted your hopper to &b%type%"),
    HOPPER_CONVERT_CANT_CONVERT_GLOBAL("&c&l(!)&7 You can't convert &cGlobal &7type hoppers!"),
    HOPPER_CONVERT_NOT_ENOUGH_FUNDS_DEFAULT("&c&l(!)&7 You don't have money to convert your hopper to %name%"),
    HOPPER_CONVERT_DEFAULT_SUCCESSFUL("&b&l(!)&7 You successfully converted hopper into %name%"),
    CONVERT_HOPPER_CANNOT_FIND_ANY_CONVERT_HOPPERS("&c&l(!) &7 Cannot find any convertable hoppers!"),
    LINKING_CONTAINER_REACHED_LIMIT("&c&l(!)&7 You have reached limit of linked containers for this hopper! &c(%limit%)"),
    CONTAINER_IS_ALREADY_LINKED("&c&l(!)&7 You can only link one hopper per container!"),
    HOPPER_GIVE_INVENTORY_FULL("&c&l(!)&7 Your inventory is full, so the hopper got dropped!");


    protected static String NOT_SEND = "99999999999999999";

    private String text;
    Lang(String text){

        this.text = text;

    }
    public static void init(){

        File lang_file = new File(MFHoppers.getInstance().getDataFolder(), "lang.yml");

        if(!lang_file.exists()){

            try{

                lang_file.createNewFile();

            } catch (Exception ex){
                ex.printStackTrace();
            }

        }

        YamlConfiguration cnf = YamlConfiguration.loadConfiguration(lang_file);

        for(Lang lang : values()){

            if(!cnf.contains(lang.name())){

                cnf.set(lang.name(), lang.text);

            }
            String value = cnf.getString(lang.name());
            if(value == null && value.toCharArray().length == 0){

                lang.text = NOT_SEND;

            } else{

                lang.text = cnf.getString(lang.name());

            }
            try{

                cnf.save(lang_file);

            } catch (Exception ex){
                ex.printStackTrace();
            }

        }


    }
    public void send(Map<String, Object> data, Player player){

        if(!text.equalsIgnoreCase(NOT_SEND)){

            String t = this.text;
            for(String key : data.keySet()){

                t = t.replace(key, data.get(key).toString());

            }
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', t));

        }

    }
    public void send(Player player){

        if(!text.equalsIgnoreCase(NOT_SEND)){

            String t = this.text;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', t));

        }

    }
}
