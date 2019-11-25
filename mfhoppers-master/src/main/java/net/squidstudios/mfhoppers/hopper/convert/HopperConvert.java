package net.squidstudios.mfhoppers.hopper.convert;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.upgrades.UpgradeEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HopperConvert {
    private int price;
    private String hopperName;
    private UpgradeEnum priceType;

    HopperConvert(int price, String hopperName, UpgradeEnum priceType){
        this.hopperName = hopperName;
        this.price = price;
        this.priceType = priceType;
    }

    public static HopperConvert decode(String split[]){
        if(split.length < 3) {
            return new HopperConvert(Integer.valueOf(split[1]), split[0], UpgradeEnum.ECO);
        }
        else {
            return new HopperConvert(Integer.valueOf(split[2]), split[0], UpgradeEnum.valueOf(split[1].toUpperCase()));
        }
    }
    public static Map<String, HopperConvert> decode(List<String> decode){

        Map<String, HopperConvert> ret = new HashMap<>();

        for(String d : decode){
            String split[] = d.split(":");
            ret.put(split[0], decode(split));
        }

        List<String> keysToRemove = new ArrayList<>();

        for(String key : ret.keySet()){
            if(!MFHoppers.getInstance().configHoppers.containsKey(key)){

                keysToRemove.add(key);
                MFHoppers.getInstance().out("&c!-> Failed to initialize a convert hopper named: &4" + key + "&c, cannot find a config hopper by that name.");

            }
        }

        keysToRemove.forEach(it -> ret.remove(it));

        return ret;
    }

    public int getPrice() {
        return price;
    }

    public UpgradeEnum getPriceType() {
        return priceType;
    }

    public String getHopperName() {
        return hopperName;
    }
}
