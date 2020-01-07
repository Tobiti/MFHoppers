package net.squidstudios.mfhoppers.util.hopperMap;

import net.squidstudios.mfhoppers.hopper.IHopper;
import net.squidstudios.mfhoppers.manager.DataManager;
import org.bukkit.Location;

import java.util.concurrent.ConcurrentHashMap;

public class HopperMap extends ConcurrentHashMap<Location, IHopper> {

    private HopperDataHandler handler = DataManager.data_handler;

    @Override
    public IHopper put(Location key, IHopper value) {
        return super.put(key, value);
    }

    public IHopper putNew(Location key, IHopper value) {
        put(key, value);
        handler.addNew(value);
        return value;
    }

    @Override
    public IHopper remove(Object key) {
        IHopper hopper = get(key);
        super.remove(key);

        if (key instanceof Location)
            handler.remove(hopper);

        return hopper;
    }

    public void setHandler(HopperDataHandler handler) {
        this.handler = handler;
    }
}
