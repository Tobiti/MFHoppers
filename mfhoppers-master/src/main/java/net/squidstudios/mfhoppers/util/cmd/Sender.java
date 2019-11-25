package net.squidstudios.mfhoppers.util.cmd;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class Sender {

    private Player player;
    private ConsoleCommandSender console;
    private boolean isPlayer = false;
    private CommandSender cmdSender;

    public Sender(CommandSender sender){

        this.cmdSender = sender;

        if(sender instanceof Player){

            this.player = (Player)sender;
            this.isPlayer = true;

        } else{

            this.console = (ConsoleCommandSender)sender;

        }

    }

    public boolean isPlayer() {
        return isPlayer;
    }

    public Player getPlayer() {
        return player;
    }
    public void sendMessage(String text){

        if(isPlayer){

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', text));

        } else{

            console.sendMessage(ChatColor.translateAlternateColorCodes('&', text));

        }

    }

    public CommandSender getCmdSender() {
        return cmdSender;
    }
}
