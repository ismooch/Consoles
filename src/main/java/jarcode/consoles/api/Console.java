package jarcode.consoles.api;

import jarcode.consoles.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a console in the game world
 */
public class Console {
	private final BlockFace face;
	private final Location location;
	private final int width, height;

	private boolean created = false;
	protected ManagedConsole underlying = null;

	/**
	 * Wraps an internal console
	 *
	 * @param console the {@link jarcode.consoles.ManagedConsole} to wrap
	 * @return the wrapped console
	 */
	public static Console wrap(ManagedConsole console) {
		Console ret = new Console(console.getDirection(), console.getLocation(),
				console.getFrameWidth(), console.getFrameHeight());
		ret.created = true;
		ret.underlying = console;
		return ret;
	}

	/**
	 * Prepares a console to be created
	 *
	 * @param face the direction this canvas will face
	 * @param location the origin of the canvas
	 * @param width the width, in frames
	 * @param height the height, in frames
	 */
	public Console(BlockFace face, Location location, int width, int height) {
		this.location = location;
		this.face = face;
		this.width = width;
		this.height = height;
	}

	/**
	 * Removes the console from the world
	 */
	public final void remove() {
		if (underlying != null)
			underlying.remove();
	}

	/**
	 * Gets the location of this console
	 *
	 * @return the location of the console
	 */
	public Location getLocation() {
		return location.clone();
	}

	/**
	 * Gets the width of this console, in frames
	 *
	 * @return the width, in frames
	 */
	public int getFrameWdith() {
		return width;
	}

	/**
	 * Gets the height of this console, in frames
	 *
	 * @return the height, in frames
	 */
	public int getFrameHeight() {
		return height;
	}

	/**
	 * Returns the {@link jarcode.consoles.api.Canvas} for this object
	 *
	 * @return the {@link jarcode.consoles.api.Canvas} for this object
	 */
	public final Canvas getCanvas() {
		return underlying;
	}

	/**
	 * Creates the console and adds it to the world.
	 */
	public final void create() {
		if (created) return;
		underlying = new ManagedConsole(width, height, false);
		underlying.setType("Custom");
		underlying.create(face, location);
		created = true;
	}

	/**
	 * Returns a component builder with the given dimensions
	 *
	 * @param width width of the resultant component
	 * @param height width of the resultant component
	 * @return a {@link jarcode.consoles.api.CanvasComponentBuilder} with the given dimensions
	 */
	public CanvasComponentBuilder newComponent(int width, int height) {
		return getCanvas().newComponent(width, height);
	}
}
