package net.squidstudios.mfhoppers.util.ent;

import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

public class EntitiesGatherer {

    private static Class<?>
        NMS_ENTITY;

    private static Method
            CRAFT_CHUNK_GET_HANLDE,
            CHUNK_GET_ENTITY_SLICES_METHOD,
            WORLD_GET_CHUNK_IF_LOADED_METHOD,
            ENTITY_GET_BUKKIT_ENTITY_METHOD,
            WORLD_GET_HANDLE;

    static {
        try {
            Class<?> CRAFT_CHUNK_CLASS = OSimpleReflection.Package.CB.getClass("CraftChunk");
            Class<?> NMS_CHUNK_CLASS = OSimpleReflection.Package.NMS.getClass("Chunk");
            NMS_ENTITY = OSimpleReflection.Package.NMS.getClass("Entity");

            Class<?> CRAFT_WORLD_CLASS = OSimpleReflection.Package.CB.getClass("CraftWorld");
            Class<?> NMS_WORLD_CLASS = OSimpleReflection.Package.NMS.getClass("World");
            Class<?> NMS_ENTITY_CLASS = OSimpleReflection.Package.NMS.getClass("Entity");

            CRAFT_CHUNK_GET_HANLDE = OSimpleReflection.getMethod(CRAFT_CHUNK_CLASS, "getHandle");
            CHUNK_GET_ENTITY_SLICES_METHOD = OSimpleReflection.getMethod(NMS_CHUNK_CLASS, "getEntitySlices");

            WORLD_GET_HANDLE = OSimpleReflection.getMethod(CRAFT_WORLD_CLASS, "getHandle");
            WORLD_GET_CHUNK_IF_LOADED_METHOD = OSimpleReflection.getMethod(NMS_WORLD_CLASS, "getChunkIfLoaded", int.class, int.class);
            ENTITY_GET_BUKKIT_ENTITY_METHOD = OSimpleReflection.getMethod(NMS_ENTITY_CLASS, "getBukkitEntity");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private Set<OPair<Integer, Integer>> chunks = Sets.newHashSet();
    private World world;
    private Set<Class<?>> accepts = Sets.newHashSet();
    private Predicate<Entity> entityFilter;

    private EntitiesGatherer() {}

    public static EntitiesGatherer from(World world, int x, int z) {
        EntitiesGatherer entitiesGather = new EntitiesGatherer();
        entitiesGather.world = world;
        entitiesGather.chunks.add(new OPair<>(x, z));

        entitiesGather.entityFilter = (entity) -> entitiesGather.accepts.isEmpty() || entitiesGather.accepts.stream().anyMatch(clazz -> clazz.isAssignableFrom(entity.getClass()));

        return entitiesGather;
    }

    public static EntitiesGatherer from(Chunk chunk) {
        return from(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    public static EntitiesGatherer from(Location location) {
        return from(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static EntitiesGatherer from(Location pos1, Location pos2) {
        int xMin = Math.min(pos1.getChunk().getX(), pos2.getChunk().getX());
        int zMin = Math.min(pos1.getChunk().getZ(), pos2.getChunk().getZ());
        int xMax = Math.max(pos1.getChunk().getX(), pos2.getChunk().getX());
        int zMax = Math.max(pos1.getChunk().getZ(), pos2.getChunk().getZ());

        EntitiesGatherer entitiesGather = new EntitiesGatherer();
        entitiesGather.world = pos1.getWorld();

        for (int x = xMin; x <= xMax; x++)
            for (int z = zMin; z <= zMax; z++)
                entitiesGather.chunks.add(new OPair<>(x, z));

        entitiesGather.entityFilter = (entity) -> {
            if (!entitiesGather.accepts.isEmpty() && entitiesGather.accepts.stream().noneMatch(clazz -> clazz.isAssignableFrom(entity.getClass())))
                return false;

            Location location = entity.getLocation();
            return location.getBlockX() >= pos1.getBlockX() && location.getBlockX() <= pos2.getBlockX() && location.getBlockZ() >= pos1.getBlockZ() && location.getBlockZ() <= pos2.getBlockZ();
        };
        return entitiesGather;
    }

    public EntitiesGatherer accepts(Class<? extends Entity> clazz) {
        accepts.add(clazz);
        return this;
    }

    public Set<Entity> gather() {
        Objects.requireNonNull(world);
        Set<Entity> returnsEntities = Sets.newHashSet();

        try {

            Object worldServer = WORLD_GET_HANDLE.invoke(world);
            for (OPair<Integer, Integer> chunk : chunks) {
                Object nmsChunk = WORLD_GET_CHUNK_IF_LOADED_METHOD.invoke(worldServer, chunk.getKey(), chunk.getValue());
                if (nmsChunk == null) continue;

                List<Object>[] entitiesSlices = (List<Object>[]) CHUNK_GET_ENTITY_SLICES_METHOD.invoke(nmsChunk);
                for (int i = 0; i < 16; i++) {
                    final List<Object> entities = new ArrayList<>(Collections.synchronizedList(entitiesSlices[i]));

                    entities.forEach(entity -> {
                        try {
                            returnsEntities.add((Entity) ENTITY_GET_BUKKIT_ENTITY_METHOD.invoke(entity));
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }

            returnsEntities.removeIf(entity -> !entityFilter.test(entity));
        } catch (Exception ex){
            ex.printStackTrace();
        }

        return returnsEntities;
    }

    public static void register(JavaPlugin plugin) {
        new Registerer(plugin);
    }

    private static class Registerer implements Listener {
        /**
         * Register the entities registerer so it can inject concurrent lists into mc chunks
         *
         * @param plugin your plugin
         */
        public Registerer(JavaPlugin plugin) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        /*
        It injects own list into entity slices of chunk
        */
        @EventHandler(priority = EventPriority.LOWEST)
        public void onLoad(ChunkLoadEvent event) {
//            try {
//                Object nmsChunk = CRAFT_CHUNK_GET_HANLDE.invoke(event.getChunk());
//
//                List<Object>[] entitiesSlices = (List<Object>[]) CHUNK_GET_ENTITY_SLICES_METHOD.invoke(nmsChunk);
//                for (int i = 0; i < 16; i++) {
//                    entitiesSlices[i] = new ConcurrentList<>();
//                }
//            } catch (Exception ex){
//                ex.printStackTrace();
//            }
        }
    }

    private static class OPair<O, T> {
        private T key;
        private O value;

        public OPair(T key, O value) {
            this.key = key;
            this.value = value;
        }

        public O getValue() {
            return value;
        }

        public T getKey() {
            return key;
        }
    }

}
