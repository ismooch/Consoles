package jarcode.consoles;

import jarcode.consoles.api.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class ConsoleContainer extends ConsoleComponent implements CanvasContainer {

	protected static int maxHeightOf(ConsoleComponent[] list) {
		int max = 0;
		for (ConsoleComponent component : list) {
			if (max < component.getHeight())
				max = component.getHeight();
		}
		return max;
	}


	protected static int totalWidthOf(ConsoleComponent[] list, int margin) {
		int width = 0;
		for (int t = 0; t < list.length; t++) {
			width += list[t].getWidth();
			if (t != list.length - 1)
				width += margin;
		}
		return width;
	}

	protected List<ConsoleComponent> contained = new ArrayList<>();

	public ConsoleContainer(int w, int h, ConsoleRenderer renderer) {
		super(w, h, renderer);
	}
	public ConsoleContainer(int w, int h, ConsoleRenderer renderer, boolean enabled) {
		super(w, h, renderer, enabled);
	}
	public final void add(CanvasComponent comp) {
		if (comp instanceof RootComponent) {
			throw new IllegalArgumentException("You cannot add root components to a container.");
		}
		if (comp instanceof PreparedComponent) {
			((PreparedComponent) comp).prepare(getRenderer());
		}
		ConsoleComponent object = comp instanceof WrappedComponent ?
				((WrappedComponent) comp).underlying() : (ConsoleComponent) comp;
		object.setContained(true);
		contained.add(object);
		onAdd(comp);
	}
	public void onAdd(CanvasComponent comp) {}
	protected final int totalContainedWidth(int margin) {
		int width = 0;
		for (int t = 0; t < contained.size(); t++) {
			ConsoleComponent component = contained.get(t);
			width += component.getWidth();
			if (t != contained.size() - 1)
				width += margin;

		}
		return width;
	}
	public final void remove(CanvasComponent comp) {
		ConsoleComponent object = comp instanceof WrappedComponent ?
				((WrappedComponent) comp).underlying() : (ConsoleComponent) comp;
		contained.remove(object);
	}
	public final CanvasComponent[] components() {
		return contained.toArray(new CanvasComponent[contained.size()]);
	}
	protected final int maxContainedHeight() {
		int max = 0;
		for (ConsoleComponent component : contained) {
			if (max < component.getHeight())
				max = component.getHeight();
		}
		return max;
	}
	@Override
	public final void handleClick(int x, int y, Player player) {
		contained.stream().filter(ConsoleComponent::enabled).forEach(comp -> {
			Position2D pos = getUnderlingComponentCoordinates(comp);
			if (pos != null && x >= pos.getX() && y >= pos.getY()
					&& x < pos.getX() + comp.getWidth() && y < pos.getY() + comp.getHeight()) {
				comp.handleClick(x - pos.getX(), y - pos.getY(), player);
			}
		});
		onClick(x, y, player);
	}
	public abstract void onClick(int x, int y, Player player);
	public abstract Position2D getUnderlingComponentCoordinates(CanvasComponent component);
}
