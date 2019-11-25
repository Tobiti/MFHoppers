package net.squidstudios.mfhoppers.util.plugin;

import com.google.common.base.Charsets;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import net.squidstudios.mfhoppers.util.cmd.Cmd;
import net.squidstudios.mfhoppers.util.cmd.Sender;

import java.io.*;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class PluginBuilder extends JavaPlugin {

    private boolean debugMode = false;
    private String markup = "&b";
    private File logsFile = null;
    private String prefix = "[" + getDescription().getName() + "]";
    private String textColour = "&7";


    public File initFile(String name, File dir){


        if(!dir.exists()){
            dir.mkdirs();
        }

        File _file = new File(dir, name);
        if(!_file.exists()){

            try {

                _file.createNewFile();

            } catch (Exception ex){

            }

        }
        return _file;

    }
    public YamlConfiguration initConfig(String name, File dir){


        if(!dir.exists()){
            dir.mkdirs();
        }

        File _file = new File(dir, name);
        if(!_file.exists()){

            try {

                saveResource(name, true);

            } catch (Exception ex){
                try {
                    _file.createNewFile();
                } catch (Exception e){

                }
            }

        }
        return YamlConfiguration.loadConfiguration(_file);

    }

    public JavaPlugin getPlugin() {
        return this;
    }
    @Override
    public void onEnable(){


        out("&8=-------------------------------------------=");
        out("");
        out("&b___  _________", OutType.WITHOUT_PREFIX);
        out("&3|  \\/  ||  ___| ", OutType.WITHOUT_PREFIX);
        out("&b| .  . || |_     &7Author: &3OOP-778 / Brian" , OutType.WITHOUT_PREFIX);
        out("&3| |\\/| ||  _|    &7Version: &3" + getDescription().getVersion(), OutType.WITHOUT_PREFIX);
        out("&b| |  | || |", OutType.WITHOUT_PREFIX);
        out("&3\\_|  |_/\\_|", OutType.WITHOUT_PREFIX);
        out("");

        this.init();

        out("");
        out("&8=-------------------------------------------=");

    }
    public void setDebugMode(boolean bool){

        this.debugMode = bool;

    }
    public void setMarkupColour(String markup){

        this.markup = markup;

    }
    public void setTextColour(String text){

        this.textColour = text;

    }
    public void debug(String text){

        if(logsFile == null){

            SimpleDateFormat df = new SimpleDateFormat("mm-dd-hh");
            String file = "BTB_DEBUG_" + df.format(new Date()) + ".txt";
            out(textColour + "Debug mode is on, out file: " + markup + file, OutType.WITH_PREFIX);
            logsFile = initFile(file,new File(getDataFolder() + "/Debugs"));
        }
        try{

            BufferedWriter writer = new BufferedWriter(new FileWriter(logsFile, true));
            writer.write(text);
            writer.newLine();
            writer.flush();
            writer.close();

        } catch (Exception ex){
            handleError(ex);
        }

    }
    public void setPrefix(String prefix){

        this.prefix = prefix;

    }
    public void out(Object obj, OutType type){
        if(obj instanceof Exception){

            Exception ex = (Exception)obj;
            handleError(ex);

        } else {
            String text = obj.toString();
            switch (type) {

                case ERROR:
                    getServer().getConsoleSender().sendMessage(c("&c" + prefix + " &4" + text));
                    break;
                case WITH_PREFIX:
                    getServer().getConsoleSender().sendMessage(c(markup + prefix + " " + textColour + text));
                    break;
                case WITHOUT_PREFIX:
                    getServer().getConsoleSender().sendMessage(c(text));
                    break;
                case ERROR_NOPREFIX:
                    getServer().getConsoleSender().sendMessage(c("&4" + text));
                    break;
                default:
                    getServer().getConsoleSender().sendMessage(c(text));
                    break;
            }

        }

    }
    public void out(Object obj){

        if(obj instanceof String){

            String text = obj.toString();
            Bukkit.getConsoleSender().sendMessage(c(textColour + text));


        } else if(obj instanceof Exception){

            Exception ex = (Exception)obj;
            handleError(ex);

        } else{
            String text = obj.toString();
            Bukkit.getConsoleSender().sendMessage(c(textColour + text));
        }

    }
    public enum OutType{
        ERROR,
        WITHOUT_PREFIX,
        WITH_PREFIX,
        ERROR_NOPREFIX
    }
    public String c(String string){

        return ChatColor.translateAlternateColorCodes('&', string);

    }
    protected void handleError(Exception ex){

        out("");
        out("&7---------- &c=[]= AQUA HOPPERS ERROR =[]= &7----------", OutType.ERROR);
        out("");
        out("&c" + ex.getLocalizedMessage());
        for(StackTraceElement ste : ex.getStackTrace()){


            String line = ste.toString();
                out(line, OutType.ERROR_NOPREFIX);

        }
        out("");
        /*

        try {

            //getting the date
            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            String _date = format.format(new Date()).replaceAll("\\s+", "@space@") + "@dateSplit@";

            //getting the error
            StringBuilder error = new StringBuilder();
            for (StackTraceElement ste : ex.getStackTrace()) {

                String line = ste.toString().replaceAll("\\s+", "@space@").replaceAll("\n", "@newLine@").replace("/", "@slash@");
                error.append(line).append("@newLine@");

            }

            StringBuilder urlB = new StringBuilder();
            urlB.append("http://54.38.225.66:5000/report/").append(_date).append("$").append(error.toString()).append("$").append(getDescription().getVersion()).append("$").append(getDescription().getName());
            URL url = new URL(urlB.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setUseCaches(false);
            con.setDoOutput(true);
            String inputLine;
            StringBuffer response = new StringBuffer();
            if (con.getResponseCode()== 200) {

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(con.getInputStream()));

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                if (response.toString().equalsIgnoreCase("Plugin doesn't exist in the whitelist!")) {

                    out("REPORT STATUS: (SENT) -> " + response.toString(), OutType.ERROR);
                    out("");
                    out("&7---------- &c=[]= ERROR =[]= &7----------", OutType.ERROR);
                    out("");
                    return;

                }

            }
            out( "REPORT (SENT) -> " +response.toString(), OutType.ERROR);

        } catch (Exception exs) {

            if(exs instanceof ConnectException){

                out( "REPORT (SENT) -> " + " Connection refused. The server might be turned off, contact developer now!", OutType.ERROR);
                out("");
                out("&7---------- &c=[]= ERROR =[]= &7----------", OutType.ERROR);
                out("");
                return;
            } else{
                // handleError(exs);
            }

        }
        */
        out("");
        out("&7---------- &c=[]= ERROR =[]= &7----------", OutType.ERROR);
        out("");


    }
    public abstract void init();

    public <T extends Event> Events addListener(
            Class<T> type,
            EventPriority priority,
            Consumer<T> listener) {

        return Events.listen(this, type, priority, listener);

    }
    private void checkForMissingKeys(String name, List<String> keysToIgnore){

        FileConfiguration internalConfig = YamlConfiguration.loadConfiguration(getResourceAsReader(name));
        FileConfiguration currentConfig = getPlugin().getConfig();

        Set<String> configKeys = currentConfig.getKeys(true);
        Set<String> internalConfigKeys = internalConfig.getKeys(true);

        Set<String> oldKeys = new HashSet<>(configKeys);
        oldKeys.removeAll(internalConfigKeys);

        Set<String> newKeys = new HashSet<>(internalConfigKeys);
        newKeys.removeAll(configKeys);
        newKeys.removeAll(keysToIgnore);

    }
    protected InputStreamReader getResourceAsReader(String fileName) {
        InputStream in = getPlugin().getResource(fileName);
        return in == null ? null : new InputStreamReader(in, Charsets.UTF_8);
    }

    public String getMarkup() {
        return markup;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getTextColour() {
        return textColour;
    }
    public void addCommand(String label, String desc, String usage, List<String> al, Consumer<Cmd> cmd){
        try {
            Field cMap = SimplePluginManager.class.getDeclaredField("commandMap");
            cMap.setAccessible(true);
            if(al == null){
                al = new ArrayList<>();
            }
            CommandMap map = (CommandMap) cMap.get(Bukkit.getPluginManager());
            map.register(getDescription().getName(), new org.bukkit.command.Command(label, desc, usage, al) {
                @Override
                public boolean execute(CommandSender sender, String unusedLabel, String[] args) {

                    Sender s = new Sender(sender);
                    cmd.accept(new Cmd(args, s));


                    return true;
                }
            });
        } catch (Exception ex){
            out(ex);
        }

    }
    public void addCommand(String label, String desc, String usage, List<String> al, Consumer<Cmd> cmd, Function<Cmd, List<String>> complete){
        try {
            Field cMap = SimplePluginManager.class.getDeclaredField("commandMap");
            cMap.setAccessible(true);
            if(al == null){
                al = new ArrayList<>();
            }
            CommandMap map = (CommandMap) cMap.get(Bukkit.getPluginManager());
            map.register(getDescription().getName(), new org.bukkit.command.Command(label, desc, usage, al) {
                @Override
                public boolean execute(CommandSender sender, String unusedLabel, String[] args) {

                    Sender s = new Sender(sender);
                    cmd.accept(new Cmd(args, s));


                    return true;
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                    Sender s = new Sender(sender);
                    return complete.apply(new Cmd(args, s));
                }
            });
        } catch (Exception ex){
            out(ex);
        }

    }
}
