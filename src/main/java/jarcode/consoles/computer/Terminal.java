package jarcode.consoles.computer;

import jarcode.consoles.ConsoleFeed;
import jarcode.consoles.InputComponent;
import jarcode.consoles.computer.filesystem.FSFolder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.function.Consumer;

public class Terminal extends ConsoleFeed implements InputComponent {

	private static final String[] JOKES = {
			"\"Some software money can't buy. For everything else, there's Microsoft.\"",
			"\"Those who do not understand UNIX are condemned to reinvent it, poorly.\" - Henry Spencer",
			"\"There is no place like ~\"",
			"\"Thou shalt not kill -9\"",
			"\"Software is like sex: it's better when it's free\" - Linus Torvalds",
			"\"Never trust an operating system you don't have sources for.\"",
			"\"Put a straw in your mouth and put your hands in your pockets. Type by hitting the " +
					"keys with the straw.\"",
			"\"I'd tell you a joke about UDP, but you probably wouldn't get it.\"",
			"\"A TCP packet walks into a bar and says, \'I'd like a beer.\' The bartender replies, " +
					"\'You want a beer?\' The TCP packet replies, \'Yes, I'd like a beer.\' \"",
			"\"I tried to come up with an IPv4 joke, but the good ones were all already exhausted...\"",
			"\"A programmer went out shopping and his wife said \"While you are out, get eggs\", " +
					"he never returned!\"",
			"\"The best thing about 404 jokes is... wait, damnit, it's around here somewhere...\"",
			"\"You have a problem, so you decide to use floating point numbers. Now you have " +
					"2.00000000001 problems.\"",
			"\"You have a problem, so you decide to use multithreading. have problems. Now 2 you\"",
			"\"There are 10 types of people in this world: those that understand binary, those that don't\"",
			"\"Q: How many Microsoft employees does it take to change a light bulb? A: None, " +
					"they just declare darkness the new standard.\"",
			"\"So I found out the other day that Ruby is a bit more mature than Java. Why's that, you ask? " +
					"Because Ruby calmly raises its exceptions. Java throws them.\"",
			"\"I know a great IPv6 joke, but I don't think you're ready for it.\"",
			"\"Q: What's the best way to accelerate a Mac? A: 9.8m/s^2\"",
			"\"VPNs. The printers of the networking world.\"",
			"\"Men communicate in TCP. Women in UDP.\"",
			"\"Check the log file at /dev/null\"",
			"\"This mod was a complete waste of my time.\" - Jarcode",
			"\"Real programmers write in C. No wait, real programmers write assembly. Or was it actual CPU " +
					"instructions? I forget, I have a Java project to finish.\"",
			"\"Are we supposed to write joke entries in this array?\""
	};

	public static Terminal newTerminal(Computer computer) {
		Terminal terminal = new Terminal(computer, false);
		terminal.onStart();
		return terminal;
	}
	private String user = "admin";
	private String currentDirectory = "/home/admin";
	private FSFolder root;
	private Computer computer;

	private volatile Consumer<String> handlerInterrupt = null;

	private final ProgramCreator creator = new ProgramCreator(this);

	private volatile boolean ignoreUnauthorizedSigterm = false;

	public void onStart() {
		println(ChatColor.GREEN + "LinuxCraft kernel " + Computer.VERSION + " (unstable)");
		randomJoke();
		advanceLine();
		updatePrompt();
		prompt();
	}
	public void randomJoke() {
		advanceLine();
		Random random = new Random();
		println(ChatColor.GRAY + JOKES[random.nextInt(JOKES.length)]);
	}
	Terminal(Computer computer, boolean setupPrompt) {
		super(computer.getViewWidth(), computer.getViewHeight(), computer.getConsole());
		this.root = computer.getRoot();
		this.computer = computer;
		setFeedCreator(creator);
		if (setupPrompt) {
			updatePrompt();
			prompt();
		}
	}
	public String getUser() {
		return user;
	}
	public void setIgnoreUnauthorizedSigterm(boolean ignore) {
		this.ignoreUnauthorizedSigterm = ignore;
	}
	public void sigTerm(Player player) {
		if (ignoreUnauthorizedSigterm && (player == null || !player.getUniqueId().equals(computer.getOwner())))
			return;
		ProgramInstance instance = getLastProgramInstance();
		if (instance != null) {
			instance.terminate();
		}
	}
	public ProgramInstance getLastProgramInstance() {
		return creator.getLastInstance();
	}
	@Override
	public void onRemove() {}
	public void setCurrentDirectory(String directory) {
		if (directory.endsWith("/"))
			directory = directory.substring(0, directory.length() - 1);
		currentDirectory = directory;
		updatePrompt();
	}
	public void setHandlerInterrupt(Consumer<String> consumer) {
		this.handlerInterrupt = consumer;
	}
	public String getCurrentDirectory() {
		return currentDirectory;
	}
	public Computer getComputer() {
		return computer;
	}
	public void updatePrompt() {
		setPrompt(String.format("admin@%s:%s$ ", computer.getHostname(), currentDirectory));
	}

	@Override
	public void handleInput(String input, String player) {
		if (handlerInterrupt != null) {
			handlerInterrupt.accept(input);
			handlerInterrupt = null;
			return;
		}
		write(input);
	}
}
