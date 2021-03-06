package jarcode.consoles.util;

import org.bukkit.Location;
import org.bukkit.World;

public class LocalPosition {
	public int x, y, z;
	public LocalPosition(Location location) {
		this.x = location.getBlockX();
		this.y = location.getBlockY();
		this.z = location.getBlockZ();
	}
	public LocalPosition(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public LocalPosition add(int x, int y, int z) {
		this.x += x;
		this.y += y;
		this.z += z;
		return this;
	}
	@Override
	public LocalPosition clone() {
		return new LocalPosition(x, y, z);
	}
	public Location global(World world) {
		return new Location(world, x, y, z);
	}
	@Override
	public boolean equals(Object other) {
		return other instanceof LocalPosition && ((LocalPosition) other).x == x &&
				((LocalPosition) other).y == y && ((LocalPosition) other).z == z;
	}
}
