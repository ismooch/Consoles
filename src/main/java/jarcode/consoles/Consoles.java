package jarcode.consoles;

import jarcode.classloading.loader.WrappedPlugin;
import jarcode.consoles.bungee.ConsoleBungeeHook;
import jarcode.consoles.command.*;
import jarcode.consoles.computer.ComputerHandler;
import jarcode.consoles.computer.interpreter.Lua;
import jarcode.consoles.util.MapInjector;
import jarcode.consoles.util.sync.SyncTaskScheduler;
import org.bukkit.event.Listener;

import java.util.function.Supplier;

public class Consoles extends WrappedPlugin {

	public static final boolean DEBUG = false;

	private static Consoles instance;

	// whether to enable the basic frame rendering for computers
	public static boolean frameRenderingEnabled = true;
	// whether to expose the raw component rendering to computers
	public static boolean componentRenderingEnabled = false;
	// allow the crafting of computers
	public static boolean allowCrafting = true;
	// maximum time that a program is allowed to run before being interrupted
	public static int maxTimeWithoutInterrupt = 7000;
	public static int maxComputers = 3;

	static {
		System.out.println("Instantiating plugin from: " + Consoles.class.getClassLoader().getClass().getSimpleName());
		if (!Consoles.class.getClassLoader().getClass().getSimpleName().equals("WrappedClassLoader")) {
			Thread.dumpStack();
			System.exit(0);
			throw new RuntimeException();
		}
		MapInjector.injectTypes();
		MapInjector.clearNBTMapFiles();
	}

	public static Consoles getInstance() {
		return instance;
	}

	{
		instance = this;
	}

	private CommandHandler commandHandler;

	@Override
	public void onEnable() {
		commandHandler = new CommandHandler(
				CommandConsole.class, CommandImage.class,
				// experimental, computers
				CommandComputer.class
		);
		Lua.killAll = false; // if this plugin was reloaded
		saveDefaultConfig();
		boolean forward = getConfig().getBoolean("bungee-forward", false);
		componentRenderingEnabled = getConfig().getBoolean("custom-components", false);
		frameRenderingEnabled = getConfig().getBoolean("frame-rendering", true);
		allowCrafting = getConfig().getBoolean("allow-computer-crafting", true);
		maxTimeWithoutInterrupt = getConfig().getInt("max-time-without-interrupt", 7000);
		maxComputers = getConfig().getInt("computer-limit", 3);
		ConsoleHandler.getInstance().local = !forward;
		if (DEBUG)
			try {
				commandHandler.addCommand(CommandMapTest.class);
			} catch (IllegalAccessException | InstantiationException e) {
				e.printStackTrace();
			}
		register(
				ConsoleHandler::getInstance, ConsoleBungeeHook::new, SyncTaskScheduler::create,
				this::getCommandHandler, ImageConsoleHandler::new, ComputerHandler::new
		);
		ImageConsoleHandler imageHandler = new ImageConsoleHandler();
		getServer().getScheduler().scheduleSyncDelayedTask(this, imageHandler::load);
	}

	@Override
	public void onDisable() {
		ConsoleHandler.getInstance().getPainter().stop();
		Lua.killAll = true;
		try {
			SyncTaskScheduler.getInstance().end();
		}
		catch (InterruptedException e) {
			getLogger().warning("Failed to end synchronized task scheduler!");
			e.printStackTrace();
		}
	}

	// this is made just because I like using :: and ->
	// no, you can't just pass references to the listeners.
	// that would be boring.
	private void register(Supplier... suppliers) {
		for (Supplier supplier : suppliers) {
			Object obj = supplier.get();
			if (obj instanceof Listener)
				this.getServer().getPluginManager().registerEvents((Listener) obj, this);
		}
	}
	public CommandHandler getCommandHandler() {
		return commandHandler;
	}
	public void registerCommand(Class<? extends CommandBase> base) throws InstantiationException, IllegalAccessException {
		commandHandler.addCommand(base);
	}
}
