package jarcode.consoles.util;

public class Allocation {
	public int x, z, w, d;
	public Allocation(int x, int z, int w, int d) {
		this.x = x;
		this.z = z;
		this.w = w;
		this.d = d;
	}
	public boolean inside(Allocation another) {
		for (LocalPosition pos : another.corners()) {
			if (!inside(pos)) return false;
		}
		return true;
	}
	public LocalPosition[] corners() {
		LocalPosition[] corners = new LocalPosition[4];
		corners[0] = new LocalPosition(x, 0, z);
		corners[1] = new LocalPosition(x + (w - 1), 0, z);
		corners[2] = new LocalPosition(x, 0, z + (d - 1));
		corners[3] = new LocalPosition(x + (w - 1), 0, z + (d - 1));
		return corners;
	}
	public boolean inside(LocalPosition point) {
		if ((point.x <= ((w + x) - 1)) && (point.x >= x)) {
			if ((point.z <= ((d + z) - 1)) && (point.z >= z)) {
				return true;
			}
		}
		return false;
	}
	public boolean overlap(Allocation another) {
		Range xr1 = new Range(x, (w + x) - 1);
		Range zr1 = new Range(z, (d + z) - 1);

		Range xr2 = new Range(another.x, (another.w + another.x) - 1);
		Range zr2 = new Range(another.z, (another.d + another.z) - 1);

		return (inRange(xr1, xr2) && inRange(zr1, zr2));
	}
	private boolean inRange(Range r1, Range r2) {
		return (r1.v1 >= r2.v1) && (r1.v1 <= r2.v2)
				|| (r1.v2 >= r2.v1) && (r1.v2 <= r2.v2)
				|| (r2.v1 >= r1.v1) && (r2.v1 <= r1.v2)
				|| ((r2.v2 >= r1.v1) && (r2.v2 <= r1.v2));
	}

	public Allocation shrink(int amount) {
		this.x += amount;
		this.z += amount;
		this.w -= 2 * amount;
		this.d -= 2 * amount;
		return this;
	}

	@Override
	public Allocation clone() {
		return new Allocation(x, z, w, d);
	}

	private class Range {
		int v1;
		int v2;

		public Range(int v1, int v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
	}
}
