package net.squidstudios.mfhoppers.util.ent;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class OSimpleReflection {

    private static Map<Class<?>, Constructor<?>> CONSTRUCTOR_MAP = new HashMap<>();
    private static Map<Class<?>, Map<String, Method>> METHOD_MAP = new HashMap<>();
    private static Map<String, Class<?>> CLASS_MAP = new HashMap<>();

    private OSimpleReflection() {
    }

    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) throws Exception {

        if (CONSTRUCTOR_MAP.containsKey(clazz)) return CONSTRUCTOR_MAP.get(clazz);

        Class<?>[] primitiveTypes = Data.getPrimitive(parameterTypes);
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (!Data.compare(Data.getPrimitive(constructor.getParameterTypes()), primitiveTypes)) {
                continue;
            }
            CONSTRUCTOR_MAP.put(clazz, constructor);
            return constructor;
        }

        throw new NoSuchMethodException("There is no such constructor in this class with the specified parameter types");
    }

    public static Constructor<?> getConstructor(String className, Package packageType, Class<?>... parameterTypes) throws Exception {
        return getConstructor(packageType.getClass(className), parameterTypes);
    }

    public static Object initializeObject(Class<?> clazz, Object... arguments) throws Exception {
        return getConstructor(clazz, Data.getPrimitive(arguments)).newInstance(arguments);
    }

    public static Object initializeObject(String className, Package packageType, Object... arguments) throws Exception {
        return initializeObject(packageType.getClass(className), arguments);
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws Exception {

        if (METHOD_MAP.containsKey(clazz) && METHOD_MAP.get(clazz).containsKey(methodName))
            return METHOD_MAP.get(clazz).get(methodName);

        Class<?>[] primitiveTypes = Data.getPrimitive(parameterTypes);
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(methodName) || !Data.compare(Data.getPrimitive(method.getParameterTypes()), primitiveTypes)) {
                continue;
            }
            if (METHOD_MAP.containsKey(clazz)) METHOD_MAP.get(clazz).put(methodName, method);
            else {
                Map<String, Method> methodMap = new HashMap<>();
                methodMap.put(methodName, method);
                METHOD_MAP.put(clazz, methodMap);
            }
            return method;
        }
        throw new IllegalAccessException("There is no such getMethod in this class with the specified displayName and parameter types");
    }

    public static Method getMethod(String className, Package packageType, String methodName, Class<?>... parameterTypes) throws Exception {
        return getMethod(packageType.getClass(className), methodName, parameterTypes);
    }

    public static Object invokeMethod(Object instance, String methodName, Object... arguments) throws Exception {
        return getMethod(instance.getClass(), methodName, Data.getPrimitive(arguments)).invoke(instance, arguments);
    }

    public static Object invokeMethod(Object instance, Class<?> clazz, String methodName, Object... arguments) throws Exception {
        return getMethod(clazz, methodName, Data.getPrimitive(arguments)).invoke(instance, arguments);
    }

    public static Object invokeMethod(Object instance, String className, Package packageType, String methodName, Object... arguments) throws Exception {
        return invokeMethod(instance, packageType.getClass(className), methodName, arguments);
    }


    public static Field getField(Class<?> clazz, boolean declared, String fieldName) throws Exception {
        Field field = declared ? clazz.getDeclaredField(fieldName) : clazz.getField(fieldName);
        field.setAccessible(true);
        return field;
    }


    public static Field getField(String className, Package packageType, boolean declared, String fieldName) throws Exception {
        return getField(packageType.getClass(className), declared, fieldName);
    }

    public static void setValue(Object instance, Class<?> clazz, boolean declared, String fieldName, Object value) throws Exception {
        getField(clazz, declared, fieldName).set(instance, value);
    }

    public static void setValue(Object instance, String className, Package packageType, boolean declared, String fieldName, Object value) throws Exception {
        setValue(instance, packageType.getClass(className), declared, fieldName, value);
    }

    public static void setValue(Object instance, boolean declared, String fieldName, Object value) throws Exception {
        setValue(instance, instance.getClass(), declared, fieldName, value);
    }

    public enum Package {
        NMS("net.minecraft.server." + getServerVersion()),
        CB("org.bukkit.craftbukkit." + getServerVersion()),
        CB_BLOCK(CB, "block"),
        CB_CHUNKIO(CB, "chunkio"),
        CB_COMMAND(CB, "command"),
        CB_CONVERSATIONS(CB, "conversations"),
        CB_ENCHANTMENS(CB, "enchantments"),
        CB_ENTITY(CB, "entity"),
        CB_EVENT(CB, "event"),
        CB_GENERATOR(CB, "generator"),
        CB_HELP(CB, "help"),
        CB_INVENTORY(CB, "inventory"),
        CB_MAP(CB, "map"),
        CB_METADATA(CB, "metadata"),
        CB_POTION(CB, "potion"),
        CB_PROJECTILES(CB, "projectiles"),
        CB_SCHEDULER(CB, "scheduler"),
        CB_SCOREBOARD(CB, "scoreboard"),
        CB_UPDATER(CB, "updater"),
        CB_UTIL(CB, "util");

        private final String path;

        Package(String path) {
            this.path = path;
        }

        Package(Package parent, String path) {
            this(parent + "." + path);
        }

        public static String getServerVersion() {
            return Bukkit.getServer().getClass().getName().split("\\.")[3];
        }

        public String getPath() {
            return path;
        }

        public Class<?> getClass(String className) throws Exception {

            if (CLASS_MAP.containsKey(className)) return CLASS_MAP.get(className);
            Class<?> clazz = Class.forName(this + "." + className);
            CLASS_MAP.put(className, clazz);
            return clazz;

        }

        public Class<?> getClassIfFoundInCache(String className) {
            return CLASS_MAP.get(className);
        }

        @Override
        public String toString() {
            return path;
        }
    }

    public enum Data {
        BYTE(byte.class, Byte.class),
        SHORT(short.class, Short.class),
        INTEGER(int.class, Integer.class),
        LONG(long.class, Long.class),
        CHARACTER(char.class, Character.class),
        FLOAT(float.class, Float.class),
        DOUBLE(double.class, Double.class),
        BOOLEAN(boolean.class, Boolean.class);

        private static final Map<Class<?>, Data> CLASS_MAP = new HashMap<>();

        static {
            for (Data type : values()) {
                CLASS_MAP.put(type.primitive, type);
                CLASS_MAP.put(type.reference, type);
            }
        }

        private final Class<?> primitive;
        private final Class<?> reference;

        Data(Class<?> primitive, Class<?> reference) {
            this.primitive = primitive;
            this.reference = reference;
        }

        public static Data fromClass(Class<?> clazz) {
            return CLASS_MAP.get(clazz);
        }

        public static Class<?> getPrimitive(Class<?> clazz) {
            Data type = fromClass(clazz);
            return type == null ? clazz : type.getPrimitive();
        }

        public static Class<?> getReference(Class<?> clazz) {
            Data type = fromClass(clazz);
            return type == null ? clazz : type.getReference();
        }

        public static Class<?>[] getPrimitive(Class<?>[] classes) {
            int length = classes == null ? 0 : classes.length;
            Class<?>[] types = new Class<?>[length];
            for (int index = 0; index < length; index++) {
                types[index] = getPrimitive(classes[index]);
            }
            return types;
        }

        public static Class<?>[] getReference(Class<?>[] classes) {
            int length = classes == null ? 0 : classes.length;
            Class<?>[] types = new Class<?>[length];
            for (int index = 0; index < length; index++) {
                types[index] = getReference(classes[index]);
            }
            return types;
        }

        public static Class<?>[] getPrimitive(Object[] objects) {
            int length = objects == null ? 0 : objects.length;
            Class<?>[] types = new Class<?>[length];
            for (int index = 0; index < length; index++) {
                types[index] = getPrimitive(objects[index].getClass());
            }
            return types;
        }

        public static Class<?>[] getReference(Object[] objects) {
            int length = objects == null ? 0 : objects.length;
            Class<?>[] types = new Class<?>[length];
            for (int index = 0; index < length; index++) {
                types[index] = getReference(objects[index].getClass());
            }
            return types;
        }

        public static boolean compare(Class<?>[] primary, Class<?>[] secondary) {
            if (primary == null || secondary == null || primary.length != secondary.length) {
                return false;
            }
            for (int index = 0; index < primary.length; index++) {
                Class<?> primaryClass = primary[index];
                Class<?> secondaryClass = secondary[index];
                if (primaryClass.equals(secondaryClass) || primaryClass.isAssignableFrom(secondaryClass)) {
                    continue;
                }
                return false;
            }
            return true;
        }

        public Class<?> getPrimitive() {
            return primitive;
        }

        public Class<?> getReference() {
            return reference;
        }
    }

    public static class Player {

        private static Class<?>
                CRAFT_PLAYER_CLASS,
                ENTITY_PLAYER_CLASS,
                PLAYER_CONNECTION_CLASS,
                PACKET_CLASS;

        private static Method
                SEND_PACKET_METHOD,
                GET_HANDLE_METHOD;

        private static Field
                PLAYER_CONNECTION_FIELD;

        static {
            try {

                CRAFT_PLAYER_CLASS = Package.CB_ENTITY.getClass("CraftPlayer");
                ENTITY_PLAYER_CLASS = Package.NMS.getClass("EntityPlayer");
                PLAYER_CONNECTION_CLASS = Package.NMS.getClass("PlayerConnection");
                PACKET_CLASS = Package.NMS.getClass("Packet");

                PLAYER_CONNECTION_FIELD = getField(ENTITY_PLAYER_CLASS, true,"playerConnection");

                SEND_PACKET_METHOD = getMethod(PLAYER_CONNECTION_CLASS, "sendPacket", PACKET_CLASS);
                GET_HANDLE_METHOD = getMethod(CRAFT_PLAYER_CLASS, "getHandle");

            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        public static void sendPacket(org.bukkit.entity.Player player, Object packet) {
            try {

                Object entityPlayer = GET_HANDLE_METHOD.invoke(player);
                Object connection = PLAYER_CONNECTION_FIELD.get(entityPlayer);

                SEND_PACKET_METHOD.invoke(connection, packet);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

}
