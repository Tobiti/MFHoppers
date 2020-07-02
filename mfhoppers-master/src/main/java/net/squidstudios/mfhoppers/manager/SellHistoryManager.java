package net.squidstudios.mfhoppers.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import history.SellHistory;
import history.SellHistory.SellHistoryEntry;
import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.util.Lang;

public class SellHistoryManager {
    public ConcurrentHashMap<UUID, SellHistory> historyList = new ConcurrentHashMap<>();

    private int Timer = 0;

    public SellHistoryManager(){
        Bukkit.getScheduler().runTaskTimerAsynchronously(MFHoppers.getInstance(), new Runnable(){
        
            @Override
            public void run() {
                if(!isEnabled()){
                    return;
                }
                Timer++;
                if(Timer >= MFHoppers.getInstance().getConfig().getInt("SellHistoryNotifierInterval", 600)){
                    for(Entry<UUID, SellHistory> entry : historyList.entrySet()) {
                        Player player = Bukkit.getPlayer(entry.getKey());
                        if(player != null){
                            SendSellHistoryMessage(player, entry.getValue());
                        }
                        historyList.remove(entry.getKey());
                    }
                    Timer = 0;
                    historyList.clear();
                }
            }
        }, 20, 20);
    }

    protected void SendSellHistoryMessage(Player player, SellHistory history) {
        Lang.SELLHISTORY_HEADER.send(player);
        double totalPrice = 0;
        for (Entry<ItemStack, SellHistoryEntry> entry : history.getSoldItems().entrySet()) {
            Map<String, Object> data = new HashMap<>();
            data.put("{amount}", entry.getValue().Amount);
            data.put("{itemtype}", entry.getKey().getType().toString());
            double price = entry.getValue().Price;
            totalPrice += Math.round(price * 100)/100;
            data.put("{price}", FormatMoney(price));
            Lang.SELLHISTORY_LINE.send(data, player);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("{totalprice}", FormatMoney(totalPrice));
        Lang.SELLHISTORY_FOOTER.send(data, player);
    }

    private boolean isEnabled() {
        return MFHoppers.getInstance().getConfig().getBoolean("SellHistory", false);
    }

    public void AddEntry(Player player, ItemStack item, int amount, double price){
        if(!isEnabled()){
            return;
        }
        if(!historyList.containsKey(player.getUniqueId())){
            historyList.put(player.getUniqueId(), new SellHistory());
        }
        historyList.get(player.getUniqueId()).AddSoldItem(item, amount, price);
    }

    public String FormatMoney(double money){
        return MFHoppers.getInstance().getEconomy().format(money);
    }

}