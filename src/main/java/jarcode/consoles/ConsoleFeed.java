package jarcode.consoles;

import org.bukkit.Bukkit;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

@SuppressWarnings("unused")
public class ConsoleFeed extends ConsoleTextArea implements Runnable {

	public static final FeedEncoder UTF_ENCODER = new FeedEncoder() {

		Charset charset = Charset.forName("UTF-8");

		@Override
		public String get(byte[] b) {
			return new String(b, charset);
		}
		@Override
		public byte[] encode(String text) {
			return text.getBytes(charset);
		}
	};

	private InputStream in = null;
	private OutputStream out = null;

	private Thread feed;

	// whether the IO thread is running
	private volatile boolean running = false;
	// whether the IO thread has ended
	private volatile boolean ended = true;
	private Exception exception = null;
	private final Object LOCK = new Object();
	private FeedEncoder encoder = null;

	private String prompt = null;

	private FeedCreator creator = null;
	private boolean initialized = false;

	private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	private ByteArrayOutputStream outgoing = new ByteArrayOutputStream();

	public ConsoleFeed(ConsoleRenderer renderer) {
		super(renderer.getWidth() - 4, renderer.getHeight() - 4, renderer);
	}
	public ConsoleFeed(int x, int y, ConsoleRenderer renderer) {
		super(x, y, renderer);
	}
	public void place() {
		getRenderer().putComponent(new Position2D(2, 2), this);
	}
	public void setIO(InputStream in, OutputStream out, FeedEncoder encoder) {
		this.in = in;
		this.out = out;
		this.encoder = encoder;
		initialized = true;
	}
	public void setFeedCreator(FeedCreator creator) {
		this.creator = creator;
	}
	public void setPrompt(String prompt) {
		synchronized (LOCK) {
			this.prompt = prompt;
		}
	}
	public void prompt() {
		if (getLastLine().length() > 0)
			advanceLine();
		print(prompt);
	}
	public void startFeed() {
		if (running && !ended)
			throw new IllegalStateException("Feed is already set up to IO");
		running = true;
		ended = false;
		feed = new Thread(this);
		feed.setName("Minecraft Console IO Feed");
		feed.setDaemon(true);
		feed.setPriority(Thread.MIN_PRIORITY);
		feed.start();
	}
	public Thread getFeedThread() {
		return feed;
	}
	public void stop() {
		running = false;
	}
	public boolean hasEnded() {
		return ended;
	}
	public Exception getException() {
		synchronized (LOCK) {
			return exception;
		}
	}
	@Override
	public void onRemove() {
		stop();
	}
	// non-blocking
	public void write(String string) {
		if (creator != null && ended) {
			print(string);
			creator.from(string);
			String result = creator.result();
			advanceLine();
			if (result == null) {
				setIO(creator.getInputStream(), creator.getOutputStream(), creator.getEncoder());
				startFeed();
			}
			else {
				if (!result.isEmpty())
					println(result);
				if (prompt != null)
					print(prompt);
			}
		}
		else if (initialized && running && !ended) {
			try {
				synchronized (LOCK) {
					byte[] arr = encoder.encode(string + "\n");
					outgoing.write(arr);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			advanceLine();
			print("Could not forward input to terminal (creator=" + creator + ", is=" + in + ", os=" + out + ")," +
					" running: " + running + ", ended: " + ended + ", initialized: " + initialized);
			advanceLine();
			print(prompt);
		}
		repaint();
	}

	@Override
	public ConsoleListener createListener() {
		return (sender, text) -> {
			write(text);
			return "Sent to console";
		};
	}

	@Override
	public void run() {
		try {
			while (running) {
				synchronized (LOCK) {
					if (outgoing.size() > 0) {
						out.write(outgoing.toByteArray());
						outgoing = new ByteArrayOutputStream();
					}
					boolean eof = false;
					while (in.available() > 0) {
						int v = in.read();
						if (v == -1) {
							eof = true;
							break;
						}
						else
							buffer.write(v);
					}
					if (buffer.size() > 0) {
						String result = encoder.get(buffer.toByteArray());
						if (result != null)
							writeConsole(result);
						buffer = new ByteArrayOutputStream();
					}
					if (eof) {
						break;
					}
				}
				Thread.sleep(50);
			}
		}
		catch (Exception e) {
			synchronized (LOCK) {
				if (Consoles.DEBUG)
					e.printStackTrace();
				exception = e;
			}
		}
		finally {
			ended = true;
			running = false;
			synchronized (LOCK) {
				if (prompt != null)
					writeConsole("\n" + prompt);
			}
		}
	}
	private void writeConsole(String text) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(Consoles.getInstance(), () -> {
			this.print(text);
			repaint();
		});
	}
	public interface FeedEncoder {
		public String get(byte[] read);
		public byte[] encode(String text);
	}
	public interface FeedCreator {
		public void from(String input);
		public String result();
		public InputStream getInputStream();
		public OutputStream getOutputStream();
		public FeedEncoder getEncoder();
	}
}
