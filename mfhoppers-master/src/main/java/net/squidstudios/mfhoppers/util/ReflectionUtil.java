package net.squidstudios.mfhoppers.util;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Various utilities regarding reflection.
 * Warning: Potentially unsafe when common sense is missing.
 *
 * @author kangarko
 */
@SuppressWarnings("unchecked")
public class ReflectionUtil {

	private static Method getHandle;
	private static Field fieldPlayerConnection;
	private static Method sendPacket;

	private static String SERVER_VERSION;
	public static int SERVER_VERSION_NUM;

	static {
		try {
			final String packageName = Bukkit.getServer() == null ? "" : Bukkit.getServer().getClass().getPackage().getName();
			SERVER_VERSION = packageName.substring(packageName.lastIndexOf('.') + 1);
			SERVER_VERSION_NUM = Integer.parseInt(SERVER_VERSION.split("_")[1]);

			getHandle = getOFCClass("entity.CraftPlayer").getMethod("getHandle");
			fieldPlayerConnection = getNMSClass("EntityPlayer").getField("playerConnection");
			sendPacket = getNMSClass("PlayerConnection").getMethod("sendPacket", getNMSClass("Packet"));

		} catch (final Throwable t) {
			System.out.println("Unable to find setup reflection. Plugin will still function.");
			System.out.println("Error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
			System.out.println("Ignore this if using Cauldron. Otherwise check if your server is compatibible.");

			fieldPlayerConnection = null;
			sendPacket = null;
			getHandle = null;
		}
	}

	/**
	 * Send a packet to the player.
	 *
	 * @param player
	 * @param packet, must be the NMS class
	 */
	public static void sendPacket(Player player, Object packet) {
		if (getHandle == null || fieldPlayerConnection == null || sendPacket == null) {
			System.out.println("Cannot send packet " + packet.getClass().getSimpleName() + " on your server sofware (known to be broken on Cauldron).");
			return;
		}

		try {
			final Object handle = getHandle.invoke(player);
			final Object playerConnection = fieldPlayerConnection.get(handle);

			sendPacket.invoke(playerConnection, packet);

		} catch (final ReflectiveOperationException ex) {
			throw new ReflectionException("Could not send " + packet.getClass().getSimpleName() + " to " + player.getName(), ex);
		}
	}

	/**
	 * Return class in the net.minecraft.server with the current version prefix included.
	 */
	public static Class<?> getNMSClass(String name) {
		return lookupClass("net.minecraft.server." + SERVER_VERSION + "." + name);
	}

	/**
	 * Return class in the org.bukkit.craftbukkit with the current version prefix included.
	 */
	public static Class<?> getOFCClass(String name) {
		return lookupClass("org.bukkit.craftbukkit." + SERVER_VERSION + "." + name);
	}

	/**
	 * Return a field inside of a class' instance (this instance is assumed to be the class where the field is).
	 */
	public static <T> T getField(Object instance, String field) {
		for (final Field f : getAllFields(instance.getClass()))
			if (f.getName().equals(field))
				return getField(f, instance);

		throw new ReflectionException("No such field " + field + " in " + instance.getClass());
	}

	/**
	 * Get a field with an instance (this instance can be different then the class).
	 */
	public static <T> T getField(Class<?> clazz, String field, Object instance) {
		for (final Field f : getAllFields(clazz))
			if (f.getName().equals(field))
				return getField(f, instance);

		throw new ReflectionException("No such field " + field + " in " + instance.getClass());
	}

	// Retrieve all fields in a class and its subclasses, including private fields.
	private final static Field[] getAllFields(Class<?> cl) {
		final List<Field> list = new ArrayList<>();

		do
			list.addAll( Arrays.asList( cl.getDeclaredFields() ) );
		while ( !(cl = cl.getSuperclass()).isAssignableFrom(Object.class) );

		return list.toArray( new Field[ list.size() ] );
	}

	/**
	 * Get a field with an nullable instance, or throws an error on failure.
	 */
	public static <T> T getField(Field f, Object instance) {
		try {
			f.setAccessible(true);

			return (T) f.get(instance);

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not get field " + f.getName() + " in instance " + instance.getClass().getSimpleName());
		}
	}

	/**
	 * Makes a new instance of a class, or throws an error on failure.
	 */
	public static <T> T instatiate(Class<T> clazz) {
		try {
			final Constructor<T> c = clazz.getDeclaredConstructor();
			c.setAccessible(true);

			return c.newInstance();

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not make instance of: " + clazz, e);
		}
	}

	/**
	 * Makes a new instance of a class, or throws an error on failure.
	 *
	 * Each of args must not be null.
	 */
	public static <T> T instatiate(Class<T> clazz, Object... args) {
		try {
			final List<Class<?>> classes = new ArrayList<>();

			for (final Object o : args) {
				Objects.requireNonNull(o, "Argument cannot be null when instatiating " + clazz);

				classes.add(o.getClass());
			}

			final Constructor<T> c = clazz.getDeclaredConstructor(classes.toArray( new Class[classes.size()] ));
			c.setAccessible(true);

			return c.newInstance(args);

		} catch (final ReflectiveOperationException e) {
			throw new ReflectionException("Could not make instance of: " + clazz, e);
		}
	}

	/**
	 * Find a class and cast it to a specific type.
	 */
	public static <T> Class<T> lookupClass(String path, Class<T> type) {
		return (Class<T>) lookupClass(path);
	}

	// Find a class with the fully qualified name.
	// Throws error when not found.
	private static Class<?> lookupClass(String path) {
		try {
			return Class.forName(path);

		} catch (final ClassNotFoundException ex) {
			throw new ReflectionException("Could not find class: " + path);
		}
	}

	/**
	 * See {@link #lookupEnum(Class, String, String)}, except for that here the error message is supplied.
	 */
	public static <E extends Enum<E>> E lookupEnum(Class<E> enumType, String name) {
		return lookupEnum(enumType, name, "The enum '" + enumType.getSimpleName() + "' does not contain '" + name + "'! Available values: {available}");
	}

	/**
	 * Search for an enum. Try to uppercase the name, replace spaces with _ and event remove the ending S to find the correct enum.
	 * Throws an error on fail.
	 *
	 * Use %available% in {errMessage} to get all enum values.
	 */
	public static <E extends Enum<E>> E lookupEnum(Class<E> enumType, String name, String errMessage) {
		Objects.requireNonNull(enumType, "Type missing for " + name);
		Objects.requireNonNull(name, "Name missing for " + enumType);

		final String oldName = name;
		E result = lookupEnumSilent(enumType, name);

		if (result == null) {
			name = name.toUpperCase();
			result = lookupEnumSilent(enumType, name);
		}

		if (result == null) {
			name = name.replace(" ", "_");
			result = lookupEnumSilent(enumType, name);
		}

		if (result == null)
			result = lookupEnumSilent(enumType, name.replace("_", ""));

		if (result == null) {
			name = name.endsWith("S") ? name.substring(0, name.length() - 1) : name + "S";
			result = lookupEnumSilent(enumType, name);
		}

		if (result == null)
			throw new MissingEnumException(oldName, errMessage.replace("{available}", StringUtils.join(enumType.getEnumConstants(), ", ")));

		return result;
	}

	/**
	 * Search for an enum, return null if not found without exception.
	 *
	 * Makes the name uppercase.
	 */
	public static <E extends Enum<E>> E lookupEnumSilent(Class<E> enumType, String name) {
		try {
			return Enum.valueOf(enumType, name.toLowerCase());
		} catch (final IllegalArgumentException ex) {
			return null;
		}
	}

	/**
	 * Attempts to send an inventory title update while the inventory is still opened.
	 */
	public static void updateInventoryTitle(Player pl, String title, String inventoryType) {
		try {

			if(title.length() > 32){
				title = title.substring(0,32);
			}

			final Object entityPlayer = pl.getClass().getMethod("getHandle").invoke(pl);

			final Constructor<?> packetConst = getNMSClass("PacketPlayOutOpenWindow").getConstructor(int.class, String.class, getNMSClass("IChatBaseComponent"), int.class);


			final Object activeContainer = entityPlayer.getClass().getField("activeContainer").get(entityPlayer);
			final Constructor<?> chatMessageConst = getNMSClass("ChatMessage").getConstructor(String.class, Object[].class);

			final Object windowId = activeContainer.getClass().getField("windowId").get(activeContainer);
			final Object chatMessage = chatMessageConst.newInstance(Methods.colorize(title), new Object[0]);

			final Object packet = packetConst.newInstance(windowId, inventoryType, chatMessage, pl.getOpenInventory().getTopInventory().getSize() );
			sendPacket(pl, packet);

			entityPlayer.getClass().getMethod("updateInventory", getNMSClass("Container")).invoke(entityPlayer, activeContainer);
		} catch (final ReflectiveOperationException ex) {

			ex.printStackTrace();

		}
	}


	public static class MissingEnumException extends ReflectionException {
		private static final long serialVersionUID = 1L;

		private final String enumName;

		public MissingEnumException(String enumName, String msg) {
			super(msg);

			this.enumName = enumName;
		}

		public MissingEnumException(String enumName, String msg, Exception ex) {
			super(msg, ex);

			this.enumName = enumName;
		}

		public String getEnumName() {
			return enumName;
		}
	}
	public static class ReflectionException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public ReflectionException(String msg) {
			super(msg);
		}

		public ReflectionException(String msg, Exception ex) {
			super(msg, ex);
		}
	}
}