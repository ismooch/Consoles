package jarcode.consoles.util;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import jarcode.consoles.Pkg;
import jarcode.consoles.util.unsafe.UnsafeTools;
import net.minecraft.server.v1_8_R2.*;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

// This is a wrapper for a client's network manager
// This is a much safer alternative than using instrumentation to
// get information about packet destinations (ProtocolLib)
//
// However, I don't know if this will screw ProtocolLib up. If it
// does, good. It's terrible.
//
// We can also effectively listen to any method in this class
public class NetworkManagerWrapper extends NetworkManager {

	private static final Method CHANNEL_READ_0;
	private static final Method HANDLE_LISTENER;
	private static final Field PROTOCOL_DIRECTION;

	static {
		try {
			if (Pkg.is("v1_8_R2")) {
				CHANNEL_READ_0 = NetworkManager.class
						.getDeclaredMethod("channelRead0", ChannelHandlerContext.class, Object.class);
				CHANNEL_READ_0.setAccessible(true);
				HANDLE_LISTENER = NetworkManager.class.getDeclaredMethod("a", ChannelHandlerContext.class, Packet.class);
				HANDLE_LISTENER.setAccessible(true);
				PROTOCOL_DIRECTION = NetworkManager.class.getDeclaredField("h");
				PROTOCOL_DIRECTION.setAccessible(true);
			}
			else throw new RuntimeException("Unsupported server version: " + Pkg.VERSION);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>Wraps a network manager in an instance of this class. The wrapper
	 * contains uninitialized fields, and overloads every method to
	 * pass to the underlying manager.</p>
	 *
	 * <p>You can wrap a wrapper, just in case you were wondering.</p>
	 *
	 * @param manager the manager to wrap
	 * @return a wrapped manager
	 */
	public static NetworkManagerWrapper wrap(NetworkManager manager) {

		// check protocol state, this wrapper is only meant for 'play'
		Object protocol = manager.k.attr(c).get();
		if (protocol != EnumProtocol.PLAY) {
			throw new RuntimeException("Wrong protocol: " + protocol);
		}

		NetworkManagerWrapper wrapper;
		try {
			// create wrapper, and avoid calling <init> code and constructors
			// this avoids creating pointless netty channels in the network manager
			// that the wrapper extends
			wrapper = UnsafeTools.allocateInstance(NetworkManagerWrapper.class);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		}
		wrapper.init(manager);
		return wrapper;
	}

	private NetworkManager underlying;
	private HashMap<Class<? extends Packet>, List<Predicate<? extends Packet>>> listeners;

	private void init(NetworkManager underlying) {
		// initialize this here, otherwise we get problems with dead references
		listeners = new HashMap<>();
		// underlying, 'real' network manager
		this.underlying = underlying;
		// copy references to public fields in original object

		this.l = underlying.l;
		this.k = underlying.k;

		this.spoofedUUID = underlying.spoofedUUID;
		this.spoofedProfile = underlying.spoofedProfile;
		this.preparing = underlying.preparing;
		try {
			// copy protocol direction
			PROTOCOL_DIRECTION.set(this, PROTOCOL_DIRECTION.get(underlying));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// this is never called, because we allocate an instance of this wrapper directly.
	@SuppressWarnings("unused")
	private NetworkManagerWrapper(EnumProtocolDirection enumprotocoldirection) {
		super(enumprotocoldirection);
	}

	/*
	 * We overload EVERY method here so that this acts exactly like the underlying wrapper.
	 * There's only a few instances where we need to flip the 'preparing' flag in addition
	 * to passing the method call to the underlying manager. This way the 'preparing' flag
	 * corresponds with the underlying flag.
	 */

	
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		underlying.channelRegistered(ctx);
	}

	
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		underlying.channelUnregistered(ctx);
	}

	
	public void channelActive(ChannelHandlerContext channelhandlercontext) throws Exception {
		underlying.channelActive(channelhandlercontext);
		// flip the preparing flag to correspond with the underlying object
		this.preparing = false;
	}

	
	public void a(EnumProtocol enumprotocol) {
		underlying.a(enumprotocol);
	}

	
	public void channelInactive(ChannelHandlerContext channelhandlercontext) throws Exception {
		underlying.channelInactive(channelhandlercontext);
	}

	
	public void exceptionCaught(ChannelHandlerContext channelhandlercontext, Throwable throwable) throws Exception {
		underlying.exceptionCaught(channelhandlercontext, throwable);
	}

	
	protected void a(ChannelHandlerContext channelhandlercontext, Packet packet) {
		try {
			HANDLE_LISTENER.invoke(underlying, channelhandlercontext, packet);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
	public void a(PacketListener packetlistener) {
		underlying.a(packetlistener);
	}
	public <T extends Packet> void registerOutgoingListener(Class<T> type, Predicate<T> function) {
		if (!listeners.containsKey(type))
			listeners.put(type, new ArrayList<>());
		List<Predicate<? extends Packet>> list = listeners.get(type);
		list.add(function);
	}
	@SuppressWarnings("unchecked")
	private <T extends Packet> boolean handle(Predicate<T> listener, Packet packet) {
		return listener.test((T) packet);
	}
	@SuppressWarnings("SuspiciousMethodCalls")
	private List<Predicate<? extends Packet>> listenersFor(Class<? extends Packet> type) {
		ArrayList<Predicate<? extends Packet>> list = new ArrayList<>();
		Class<?> at = type;
		while (at != Packet.class && at != null) {
			List<Predicate<? extends Packet>> from = listeners.get(at);
			if (from != null)
				list.addAll(from);
			at = at.getSuperclass();
		}
		return list;
	}
	
	public void handle(Packet packet) {
		List<Predicate<? extends Packet>> list = listenersFor(packet.getClass());
		for (Predicate<? extends Packet> listener : list) {
			try {
				if (!handle(listener, packet))
					return;
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
		underlying.handle(packet);
	}

	@SuppressWarnings("unchecked")
	
	public final void a(Packet packet, GenericFutureListener<? extends Future<? super Void>> future,
	                    GenericFutureListener<? extends Future<? super Void>>... listeners) {
		underlying.a(packet, future, listeners);
	}

	
	public void a() {
		underlying.a();
	}

	
	public SocketAddress getSocketAddress() {
		return underlying.getSocketAddress();
	}

	
	public void close(IChatBaseComponent ichatbasecomponent) {
		// flip the preparing flag to correspond with the underlying object
		preparing = false;
		underlying.close(ichatbasecomponent);
	}

	
	public boolean c() {
		return underlying.c();
	}

	
	public void a(SecretKey secretkey) {
		underlying.a(secretkey);
	}

	
	public boolean g() {
		return underlying.g();
	}

	
	public boolean h() {
		return underlying.h();
	}

	
	public PacketListener getPacketListener() {
		return underlying.getPacketListener();
	}

	
	public IChatBaseComponent j() {
		return underlying.j();
	}

	
	public void k() {
		underlying.k();
	}

	
	public void a(int i) {
		underlying.a(i);
	}

	
	public void l() {
		underlying.l();
	}

	
	public boolean acceptInboundMessage(Object msg) throws Exception {
		return underlying.acceptInboundMessage(msg);
	}

	
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		underlying.channelRead(ctx, msg);
	}

	
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		underlying.channelReadComplete(ctx);
	}

	
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		underlying.userEventTriggered(ctx, evt);
	}

	
	public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
		underlying.channelWritabilityChanged(ctx);
	}

	
	protected void channelRead0(ChannelHandlerContext channelhandlercontext, Packet object) {
		// we can't access protected methods, use reflection
		try {
			CHANNEL_READ_0.invoke(underlying, channelhandlercontext, object);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
	public SocketAddress getRawAddress() {
		return underlying.getRawAddress();
	}

	
	public boolean isSharable() {
		return underlying.isSharable();
	}

	
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		underlying.handlerAdded(ctx);
	}

	
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		underlying.handlerRemoved(ctx);
	}
	
	public int hashCode() {
		return underlying.hashCode();
	}

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	
	public boolean equals(Object obj) {
		return underlying.equals(obj);
	}
	
	public String toString() {
		return underlying.toString();
	}
}
