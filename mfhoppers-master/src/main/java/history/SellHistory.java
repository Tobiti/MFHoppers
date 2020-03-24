package history;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;

import lombok.Getter;

@Getter
public class SellHistory{
    private ConcurrentHashMap<Material, Integer> soldItems = new ConcurrentHashMap<Material, Integer>();

    public void AddSoldItem(Material mat, int amount){
        if(soldItems.containsKey(mat)){
            soldItems.put(mat,soldItems.get(mat) + amount);
        }
        else {
            soldItems.put(mat, amount);
        }
    }

}