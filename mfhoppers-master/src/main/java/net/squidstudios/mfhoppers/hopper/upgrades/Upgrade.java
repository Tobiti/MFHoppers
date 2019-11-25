package net.squidstudios.mfhoppers.hopper.upgrades;

import net.squidstudios.mfhoppers.MFHoppers;
import net.squidstudios.mfhoppers.hopper.ConfigHopper;
import net.squidstudios.mfhoppers.hopper.HopperEnum;
import net.squidstudios.mfhoppers.util.Drop;
import net.squidstudios.mfhoppers.util.plugin.PluginBuilder;
import org.bukkit.Material;
import net.squidstudios.mfhoppers.util.MMaterial;
import net.squidstudios.mfhoppers.util.particles.ParticleEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Upgrade {
    private Map<String, Object> toUpgrade = new HashMap<>();
    private ConfigHopper hopper = null;
    boolean first = false;
    public Upgrade(Map<String, Object> data, Map<String, Object> previousUpgradeData, ConfigHopper hopper, int level, MFHoppers pl, boolean first) {

        if (!first) {
            if (!data.containsKey("priceType")) {
                pl.out(" !-> Failed to init hopper upgrade &c{lvl=" + level + ",hopper=" + hopper.getData().get("name") + "}&4 cannot find value: &cPriceType", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;
            }
            if (!data.containsKey("price")) {
                pl.out(" !-> Failed to init hopper upgrade &c{lvl=" + level + ",hopper=" + hopper.getData().get("name") + "}&4 cannot find value: &cprice", PluginBuilder.OutType.ERROR_NOPREFIX);
                return;
            }
        }
        if(data.containsKey("particle")){
            if(ParticleEffect.fromName(data.get("particle").toString()) == null){
                pl.out(" !-> Failed to init hopper upgrade &c{lvl=" + level + ",hopper=" + hopper.getData().get("name") + "}&4 cannot find particle named by: &c" + data.get("particle").toString(), PluginBuilder.OutType.ERROR_NOPREFIX);
                return;
            }
        }
        Map<String, Object> newData = new HashMap<>();

        if(previousUpgradeData != null) {
            for (String key : previousUpgradeData.keySet()) {
                newData.put(key, previousUpgradeData.get(key));
            }
        }
        for (String key : data.keySet())
        {
            if(newData.containsKey(key)){
                newData.remove(key);
            }
            newData.put(key, data.get(key));
        }

        if(hopper.getType() == HopperEnum.Break){
            if(data.containsKey("drops")) {
                final List<String> stringList = (ArrayList<String>) newData.get("drops");
                newData.remove("drops");
                List<ConfigHopper.BreakDropsElement> drops = new ArrayList<>();
                for (String drop : stringList) {
                    if (drop.contains("-") && drop.contains(":")) {

                        //Is random
                        String split1[] = drop.split(":");
                        String[] split2 = null;
                        if (split1.length > 2) {
                            split2 = split1[2].split("-");
                        } else {
                            split2 = split1[1].split("-");
                        }
                        Material mat = MMaterial.matchMaterial(split1[0]);
                        int min = Integer.valueOf(split2[0]);
                        int max = Integer.valueOf(split2[1]);
                        if (split1.length > 2) {
                            drops.add(new ConfigHopper.BreakDropsElement(mat, true, Integer.valueOf(split1[1]).shortValue(), new Drop(mat, min, max)));
                        } else {
                            drops.add(new ConfigHopper.BreakDropsElement(mat, false, (short) 0, new Drop(mat, min, max)));
                        }

                    } else {

                        String split1[] = drop.split(":");
                        Material mat = MMaterial.matchMaterial(split1[0]);
                        if (split1.length > 2) {
                            drops.add(new ConfigHopper.BreakDropsElement(mat, true, Integer.valueOf(split1[1]).shortValue(), new Drop(mat, Integer.valueOf(split1[2]))));
                        } else {
                            drops.add(new ConfigHopper.BreakDropsElement(mat, false, (short) 0, new Drop(mat, Integer.valueOf(split1[1]))));
                        }

                    }
                }
                newData.put("drops", drops);
            }
        }
        this.toUpgrade = newData;
        this.first = first;
        this.hopper = hopper;
        hopper.getUpgrades().put(level, this);

    }

    public Map<String, Object> getToUpgrade() {
        return toUpgrade;
    }
}
