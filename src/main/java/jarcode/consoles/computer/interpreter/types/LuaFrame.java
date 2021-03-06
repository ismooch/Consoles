package jarcode.consoles.computer.interpreter.types;

import jarcode.consoles.api.CanvasGraphics;
import jarcode.consoles.computer.Computer;
import org.bukkit.ChatColor;
import org.bukkit.map.MapFont;
import org.bukkit.map.MinecraftFont;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LuaFrame {

	private static final MapFont FONT = MinecraftFont.Font;

	protected List<Consumer<CanvasGraphics>> operations = new ArrayList<>();
	private Computer computer;
	private int id;

	public LuaFrame(int id, Computer computer) {
		this.computer = computer;
		this.id = id;
	}
	public int id() {
		return id;
	}
	public void set(Integer x, Integer y, Integer c) {
		operations.add((g) -> g.draw(x, y, convert(c)));
	}
	public int len(String text) {
		text = ChatColor.translateAlternateColorCodes('&', text);
		return FONT.getWidth(ChatColor.stripColor(text).replace("\u00A7", "&"));
	}
	public void write(Integer x, Integer y, String text) {
		text = ChatColor.translateAlternateColorCodes('&', text);
		text = text.replace("\n", "");
		final String t = text;
		operations.add((g) -> g.drawFormatted(x, y, t));
	}
	public void box(Integer x, Integer y, Integer w, Integer h, Integer c) {
		byte converted = convert(c);
		operations.add((g) -> {
			for (int t = x; t < x + w; t++) {
				for (int j = y; j < y + h; j++) {
					g.draw(x, y, converted);
				}
			}
		});
	}
	public void fill(Integer c) {
		byte converted = convert(c);
		operations.add((g) -> {
			for (int t = 0; t < g.getWidth(); t++) {
				for (int j = 0; j < g.getHeight(); j++) {
					g.draw(t, j, converted);
				}
			}
		});
	}
	public int getWidth() {
		return computer.getViewWidth();
	}
	public int getHeight() {
		return computer.getViewHeight();
	}
	private byte convert(Integer c) {
		if (c >= 0 && c <= 127) {
			return (byte) (int) c;
		}
		else if (c > 127 && c <= 143) {
			return (byte) (-128 + (c - 127));
		}
		else return (byte) 0;
	}
}
