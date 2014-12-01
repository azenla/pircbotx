/**
 * Copyright (C) 2010-2014 Leon Blakey <lord.quackstar at gmail.com>
 *
 * This file is part of PircBotX.
 *
 * PircBotX is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * PircBotX is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * PircBotX. If not, see <http://www.gnu.org/licenses/>.
 */
package org.pircbotx;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.SocketFactory;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.pircbotx.cap.CapHandler;
import org.pircbotx.cap.EnableCapHandler;
import org.pircbotx.dcc.DccHandler;
import org.pircbotx.dcc.ReceiveChat;
import org.pircbotx.dcc.ReceiveFileTransfer;
import org.pircbotx.dcc.SendChat;
import org.pircbotx.dcc.SendFileTransfer;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.CoreHooks;
import org.pircbotx.hooks.Listener;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.pircbotx.output.OutputCAP;
import org.pircbotx.output.OutputChannel;
import org.pircbotx.output.OutputDCC;
import org.pircbotx.output.OutputIRC;
import org.pircbotx.output.OutputRaw;
import org.pircbotx.output.OutputUser;

/**
 * Immutable configuration for PircBotX. Use {@link Configuration.Builder} to
 * create
 *
 * @author Leon Blakey
 */
@Data
@ToString(exclude = {"serverPassword", "nickservPassword"})
public class Configuration {
	//WebIRC
	protected final boolean webIrcEnabled;
	protected final String webIrcUsername;
	protected final String webIrcHostname;
	protected final InetAddress webIrcAddress;
	protected final String webIrcPassword;
	//Bot information
	protected final String name;
	protected final String login;
	protected final String version;
	protected final String finger;
	protected final String realName;
	protected final String channelPrefixes;
	protected final String channelModeMessagePrefixes;
	protected final boolean snapshotsEnabled;
	//DCC
	protected final boolean dccFilenameQuotes;
	protected final ImmutableList<Integer> dccPorts;
	protected final InetAddress dccLocalAddress;
	protected final int dccAcceptTimeout;
	protected final int dccResumeAcceptTimeout;
	protected final int dccTransferBufferSize;
	protected final boolean dccPassiveRequest;
	//Connect information
	protected final ImmutableList<ServerEntry> servers;
	protected final String serverPassword;
	protected final SocketFactory socketFactory;
	protected final InetAddress localAddress;
	protected final Charset encoding;
	protected final Locale locale;
	protected final int socketTimeout;
	protected final int maxLineLength;
	protected final boolean autoSplitMessage;
	protected final boolean autoNickChange;
	protected final long messageDelay;
	protected final boolean shutdownHookEnabled;
	protected final ImmutableMap<String, String> autoJoinChannels;
	protected final boolean identServerEnabled;
	protected final String nickservPassword;
	protected final String nickservOnSuccess;
	protected final String nickservNick;
	protected final boolean nickservDelayJoin;
	protected final boolean autoReconnect;
	protected final int autoReconnectDelay;
	protected final int autoReconnectAttempts;
	//Bot classes
	protected final ListenerManager listenerManager;
	protected final boolean capEnabled;
	protected final ImmutableList<CapHandler> capHandlers;
	protected final ImmutableSortedMap<Character, ChannelModeHandler> channelModeHandlers;
	protected final BotFactory botFactory;

	/**
	 * Use {@link Configuration.Builder#buildConfiguration() }.
	 *
	 * @param builder
	 * @see Configuration.Builder#buildConfiguration()
	 */
	protected Configuration(Builder builder) {
		//Check for basics
		if (builder.isWebIrcEnabled()) {
			checkNotNull(builder.getWebIrcAddress(), "Must specify WEBIRC address if enabled");
			checkArgument(StringUtils.isNotBlank(builder.getWebIrcHostname()), "Must specify WEBIRC hostname if enabled");
			checkArgument(StringUtils.isNotBlank(builder.getWebIrcUsername()), "Must specify WEBIRC username if enabled");
			checkArgument(StringUtils.isNotBlank(builder.getWebIrcPassword()), "Must specify WEBIRC password if enabled");
		}
		checkNotNull(builder.getListenerManager());
		checkArgument(StringUtils.isNotBlank(builder.getName()), "Must specify name");
		checkArgument(StringUtils.isNotBlank(builder.getLogin()), "Must specify login");
		checkArgument(StringUtils.isNotBlank(builder.getRealName()), "Must specify realName");
		checkArgument(StringUtils.isNotBlank(builder.getChannelPrefixes()), "Must specify channel prefixes");
		checkNotNull(builder.getChannelModeMessagePrefixes(), "Channel mode message prefixes cannot be null");
		checkArgument(builder.getDccAcceptTimeout() > 0, "dccAcceptTimeout must be positive");
		checkArgument(builder.getDccResumeAcceptTimeout() > 0, "dccResumeAcceptTimeout must be positive");
		checkArgument(builder.getDccTransferBufferSize() > 0, "dccTransferBufferSize must be positive");
		for (ServerEntry serverEntry : builder.getServers()) {
			checkArgument(StringUtils.isNotBlank(serverEntry.getHostname()), "Must specify server hostname");
			checkArgument(serverEntry.getPort() > 0 && serverEntry.getPort() <= 65535, "Port must be between 1 and 65535");
		}
		checkNotNull(builder.getSocketFactory(), "Must specify socket factory");
		checkNotNull(builder.getEncoding(), "Must specify encoding");
		checkNotNull(builder.getLocale(), "Must specify locale");
		checkArgument(builder.getSocketTimeout() >= 0, "Socket timeout must be positive");
		checkArgument(builder.getMaxLineLength() > 0, "Max line length must be positive");
		checkArgument(builder.getMessageDelay() >= 0, "Message delay must be positive");
		checkArgument(builder.getAutoReconnectAttempts() >= 0, "setAutoReconnectAttempts must be positive");
		checkArgument(builder.getAutoReconnectDelay() >= 0, "setAutoReconnectAttempts must be positive");
		if (builder.getNickservPassword() != null)
			checkArgument(!builder.getNickservPassword().trim().equals(""), "Nickserv password cannot be empty");
		checkNotNull(builder.getListenerManager(), "Must specify listener manager");
		checkNotNull(builder.getBotFactory(), "Must specify bot factory");
		for (Map.Entry<String, String> curEntry : builder.getAutoJoinChannels().entrySet())
			if (StringUtils.isBlank(curEntry.getKey()))
				throw new RuntimeException("Channel must not be blank");
		if (builder.getNickservOnSuccess() != null) {
			checkArgument(StringUtils.isNotBlank(builder.getNickservNick()), "Must specify nickserv nick");
		}

		this.webIrcEnabled = builder.isWebIrcEnabled();
		this.webIrcUsername = builder.getWebIrcUsername();
		this.webIrcHostname = builder.getWebIrcHostname();
		this.webIrcAddress = builder.getWebIrcAddress();
		this.webIrcPassword = builder.getWebIrcPassword();
		this.name = builder.getName();
		this.login = builder.getLogin();
		this.version = builder.getVersion();
		this.finger = builder.getFinger();
		this.realName = builder.getRealName();
		this.channelPrefixes = builder.getChannelPrefixes();
		this.channelModeMessagePrefixes = builder.getChannelModeMessagePrefixes();
		this.snapshotsEnabled = builder.isSnapshotsEnabled();
		this.dccFilenameQuotes = builder.isDccFilenameQuotes();
		this.dccPorts = ImmutableList.copyOf(builder.getDccPorts());
		this.dccLocalAddress = builder.getDccLocalAddress();
		this.dccAcceptTimeout = builder.getDccAcceptTimeout();
		this.dccResumeAcceptTimeout = builder.getDccResumeAcceptTimeout();
		this.dccTransferBufferSize = builder.getDccTransferBufferSize();
		this.dccPassiveRequest = builder.isDccPassiveRequest();
		this.servers = ImmutableList.copyOf(builder.getServers());
		this.serverPassword = builder.getServerPassword();
		this.socketFactory = builder.getSocketFactory();
		this.localAddress = builder.getLocalAddress();
		this.encoding = builder.getEncoding();
		this.locale = builder.getLocale();
		this.socketTimeout = builder.getSocketTimeout();
		this.maxLineLength = builder.getMaxLineLength();
		this.autoSplitMessage = builder.isAutoSplitMessage();
		this.autoNickChange = builder.isAutoNickChange();
		this.messageDelay = builder.getMessageDelay();
		this.identServerEnabled = builder.isIdentServerEnabled();
		this.nickservPassword = builder.getNickservPassword();
		this.nickservOnSuccess = builder.getNickservOnSuccess();
		this.nickservNick = builder.getNickservNick();
		this.nickservDelayJoin = builder.isNickservDelayJoin();
		this.autoReconnect = builder.isAutoReconnect();
		this.autoReconnectDelay = builder.getAutoReconnectDelay();
		this.autoReconnectAttempts = builder.getAutoReconnectAttempts();
		this.listenerManager = builder.getListenerManager();
		this.autoJoinChannels = ImmutableMap.copyOf(builder.getAutoJoinChannels());
		this.capEnabled = builder.isCapEnabled();
		this.capHandlers = ImmutableList.copyOf(builder.getCapHandlers());
		ImmutableSortedMap.Builder<Character, ChannelModeHandler> channelModeHandlersBuilder = ImmutableSortedMap.naturalOrder();
		for (ChannelModeHandler curHandler : builder.getChannelModeHandlers())
			channelModeHandlersBuilder.put(curHandler.getMode(), curHandler);
		this.channelModeHandlers = channelModeHandlersBuilder.build();
		this.shutdownHookEnabled = builder.isShutdownHookEnabled();
		this.botFactory = builder.getBotFactory();
	}

	@Accessors(chain = true)
	@Data
	public static class Builder {
		//WebIRC
		/**
		 * Enable or disable sending WEBIRC line on connect
		 */
		protected boolean webIrcEnabled = false;
		/**
		 * Username of WEBIRC connection
		 */
		protected String webIrcUsername = null;
		/**
		 * Hostname of WEBIRC connection
		 */
		protected String webIrcHostname = null;
		/**
		 * IP address of WEBIRC connection
		 */
		protected InetAddress webIrcAddress = null;
		/**
		 * Password of WEBIRC connection
		 */
		protected String webIrcPassword = null;
		//Bot information
		/**
		 * The base name to be used for the IRC connection (nick!login@host)
		 */
		protected String name = "PircBotX";
		/**
		 * The login to be used for the IRC connection (nick!login@host)
		 */
		protected String login = "PircBotX";
		/**
		 * CTCP version response.
		 */
		protected String version = "PircBotX " + PircBotX.VERSION + ", a fork of PircBot, the Java IRC bot - pircbotx.googlecode.com";
		/**
		 * CTCP finger response
		 */
		protected String finger = "You ought to be arrested for fingering a bot!";
		/**
		 * The realName/fullname used for WHOIS info. Defaults to version
		 */
		protected String realName = version;
		/**
		 * Allowed channel prefix characters. Defaults to <code>#&+!</code>
		 */
		protected String channelPrefixes = "#&+!";
		/**
		 * Supported channel prefixes that restrict a sent message to users with
		 * this mode. Defaults to <code>+%&~!</code>
		 */
		protected String channelModeMessagePrefixes = "+%&~!";
		/**
		 * Enable creation of snapshots, default true. In bulk datasets or very
		 * lower power devices, creating snapshots can be a relatively expensive
		 * operation for every {@link GenericSnapshotEvent} (eg PartEvent,
		 * QuitEvent) since the entire UserChannelDao with all of its users and
		 * channels is cloned. This can optionally disabled by setting this to
		 * false, however this makes all
		 * {@link GenericSnapshotEvent#getUserChannelDaoSnapshot()} calls return
		 * null.
		 * <p>
		 * In regular usage disabling snapshots is not necessary because there
		 * relatively few user QUITs and PARTs per second.
		 */
		protected boolean snapshotsEnabled = true;
		//DCC
		/**
		 * If true sends filenames in quotes, otherwise uses underscores.
		 * Defaults to false
		 */
		protected boolean dccFilenameQuotes = false;
		/**
		 * Ports to allow DCC incoming connections. Recommended to set multiple
		 * as DCC connections will be rejected if no free port can be found
		 */
		protected List<Integer> dccPorts = Lists.newArrayList();
		/**
		 * The local address to bind DCC connections to. Defaults to {@link #getLocalAddress()
		 * }
		 */
		protected InetAddress dccLocalAddress = null;
		/**
		 * Timeout for user to accept a sent DCC request. Defaults to {@link #getSocketTimeout()
		 * }
		 */
		protected int dccAcceptTimeout = -1;
		/**
		 * Timeout for a user to accept a resumed DCC request. Defaults to {@link #getDccResumeAcceptTimeout()
		 * }
		 */
		protected int dccResumeAcceptTimeout = -1;
		/**
		 * Size of the DCC file transfer buffer. Defaults to 1024
		 */
		protected int dccTransferBufferSize = 1024;
		/**
		 * Weather to send DCC Passive/reverse requests. Defaults to false
		 */
		protected boolean dccPassiveRequest = false;
		//Connect information
		/**
		 * Hostname of the IRC server
		 */
		protected List<ServerEntry> servers = Lists.newLinkedList();
		/**
		 * Password for IRC server
		 */
		protected String serverPassword = null;
		/**
		 * Socket factory for connections. Defaults to {@link SocketFactory#getDefault()
		 * }
		 */
		protected SocketFactory socketFactory = SocketFactory.getDefault();
		/**
		 * Address to bind to when connecting to IRC server.
		 */
		protected InetAddress localAddress = null;
		/**
		 * Charset encoding to use for connection. Defaults to
		 * {@link Charset#defaultCharset()}
		 */
		protected Charset encoding = Charset.defaultCharset();
		/**
		 * Locale to use for connection. Defaults to {@link Locale#getDefault()
		 * }
		 */
		protected Locale locale = Locale.getDefault();
		/**
		 * Timeout of IRC connection before sending PING. Defaults to 5 minutes
		 */
		protected int socketTimeout = 1000 * 60 * 5;
		/**
		 * Maximum line length of IRC server. Defaults to 512
		 */
		protected int maxLineLength = 512;
		/**
		 * Enable or disable automatic message splitting to fit
		 * {@link #getMaxLineLength()}. Note that messages might be truncated by
		 * the IRC server if not set. Defaults to true
		 */
		protected boolean autoSplitMessage = true;
		/**
		 * Enable or disable automatic nick changing if a nick is in use by
		 * adding a number to the end. If this is false and a nick is already in
		 * use, a {@link IrcException} will be thrown. Defaults to false.
		 */
		protected boolean autoNickChange = false;
		/**
		 * Millisecond delay between sending messages with {@link OutputRaw#rawLine(java.lang.String)
		 * }. Defaults to 1000 milliseconds
		 */
		protected long messageDelay = 1000;
		/**
		 * Enable or disable creating a JVM shutdown hook which will properly
		 * QUIT the IRC server and shutdown the bot. Defaults to true
		 */
		protected boolean shutdownHookEnabled = true;
		/**
		 * Map of channels and keys to automatically join upon connecting.
		 */
		protected final Map<String, String> autoJoinChannels = Maps.newHashMap();
		/**
		 * Enable or disable use of an existing {@link IdentServer}. Note that
		 * the IdentServer must be started separately or else an exception will
		 * be thrown. Defaults to false
		 *
		 * @see IdentServer
		 */
		protected boolean identServerEnabled = false;
		/**
		 * If set, password to authenticate against NICKSERV
		 */
		protected String nickservPassword;
		/**
		 * Case-insensitive message a user with 
		 * {@link #setNickservNick(java.lang.String) } in its hostmask will
		 * always contain when we have successfully identified, defaults to "you
		 * are now" from "You are now identified for PircBotX". Known server
		 * responses:
		 * <ul>
		 * <li>ircd-seven (freenode) - You are now identified for PircBotX</li>
		 * <li>Unreal (swiftirc) - Password accepted - you are now
		 * recognized.</li>
		 * <li>InspIRCd (mozilla) - You are now logged in as PircBotX</li>
		 * </ul>
		 *
		 * @see PircBotX#isNickservIdentified()
		 * @see #setNickservNick(java.lang.String)
		 */
		protected String nickservOnSuccess = "you are now";
		/**
		 * The nick of the nickserv service account, default "nickserv".
		 *
		 * @see PircBotX#isNickservIdentified()
		 */
		protected String nickservNick = "nickserv";
		/**
		 * Delay joining channels until were identified to nickserv
		 */
		protected boolean nickservDelayJoin = false;
		/**
		 * Enable or disable automatic reconnecting. Note that you MUST call 
		 * {@link PircBotX#stopBotReconnect() } when you do not want the bot to
		 * reconnect anymore! Defaults to false
		 */
		protected boolean autoReconnect = false;
		/**
		 * Delay in milliseconds between reconnect attempts. Default 0.
		 */
		protected int autoReconnectDelay = 0;
		/**
		 * Number of times to attempt to reconnect
		 */
		protected int autoReconnectAttempts = 5;
		//Bot classes
		/**
		 * The {@link ListenerManager} to use to handle events.
		 */
		protected ListenerManager listenerManager = null;
		/**
		 * Enable or disable CAP handling. Defaults to true
		 */
		protected boolean capEnabled = true;
		/**
		 * Registered {@link CapHandler}'s.
		 */
		protected final List<CapHandler> capHandlers = Lists.newArrayList();
		/**
		 * Handlers for channel modes. By default is built-in handlers
		 */
		protected final List<ChannelModeHandler> channelModeHandlers = Lists.newArrayList();
		/**
		 * The {@link BotFactory} to use
		 */
		protected BotFactory botFactory = new BotFactory();

		/**
		 * Default constructor, adding a multi-prefix {@link EnableCapHandler}
		 */
		public Builder() {
			capHandlers.add(new EnableCapHandler("multi-prefix", true));
			capHandlers.add(new EnableCapHandler("away-notify", true));
			channelModeHandlers.addAll(InputParser.DEFAULT_CHANNEL_MODE_HANDLERS);
		}

		/**
		 * Copy values from an existing Configuration.
		 *
		 * @param configuration Configuration to copy values from
		 */
		public Builder(Configuration configuration) {
			this.webIrcEnabled = configuration.isWebIrcEnabled();
			this.webIrcUsername = configuration.getWebIrcUsername();
			this.webIrcHostname = configuration.getWebIrcHostname();
			this.webIrcAddress = configuration.getWebIrcAddress();
			this.webIrcPassword = configuration.getWebIrcPassword();
			this.name = configuration.getName();
			this.login = configuration.getLogin();
			this.version = configuration.getVersion();
			this.finger = configuration.getFinger();
			this.realName = configuration.getRealName();
			this.channelPrefixes = configuration.getChannelPrefixes();
			this.channelModeMessagePrefixes = configuration.getChannelModeMessagePrefixes();
			this.snapshotsEnabled = configuration.isSnapshotsEnabled();
			this.dccFilenameQuotes = configuration.isDccFilenameQuotes();
			this.dccPorts.addAll(configuration.getDccPorts());
			this.dccLocalAddress = configuration.getDccLocalAddress();
			this.dccAcceptTimeout = configuration.getDccAcceptTimeout();
			this.dccResumeAcceptTimeout = configuration.getDccResumeAcceptTimeout();
			this.dccTransferBufferSize = configuration.getDccTransferBufferSize();
			this.dccPassiveRequest = configuration.isDccPassiveRequest();
			this.servers.addAll(configuration.getServers());
			this.serverPassword = configuration.getServerPassword();
			this.socketFactory = configuration.getSocketFactory();
			this.localAddress = configuration.getLocalAddress();
			this.encoding = configuration.getEncoding();
			this.locale = configuration.getLocale();
			this.socketTimeout = configuration.getSocketTimeout();
			this.maxLineLength = configuration.getMaxLineLength();
			this.autoSplitMessage = configuration.isAutoSplitMessage();
			this.autoNickChange = configuration.isAutoNickChange();
			this.messageDelay = configuration.getMessageDelay();
			this.listenerManager = configuration.getListenerManager();
			this.nickservPassword = configuration.getNickservPassword();
			this.nickservOnSuccess = configuration.getNickservOnSuccess();
			this.nickservNick = configuration.getNickservNick();
			this.nickservDelayJoin = configuration.isNickservDelayJoin();
			this.autoReconnect = configuration.isAutoReconnect();
			this.autoReconnectDelay = configuration.getAutoReconnectDelay();
			this.autoReconnectAttempts = configuration.getAutoReconnectAttempts();
			this.autoJoinChannels.putAll(configuration.getAutoJoinChannels());
			this.identServerEnabled = configuration.isIdentServerEnabled();
			this.capEnabled = configuration.isCapEnabled();
			this.capHandlers.addAll(configuration.getCapHandlers());
			this.channelModeHandlers.addAll(configuration.getChannelModeHandlers().values());
			this.shutdownHookEnabled = configuration.isShutdownHookEnabled();
			this.botFactory = configuration.getBotFactory();
		}

		/**
		 * Copy values from another builder.
		 *
		 * @param otherBuilder
		 */
		public Builder(Builder otherBuilder) {
			this.webIrcEnabled = otherBuilder.isWebIrcEnabled();
			this.webIrcUsername = otherBuilder.getWebIrcUsername();
			this.webIrcHostname = otherBuilder.getWebIrcHostname();
			this.webIrcAddress = otherBuilder.getWebIrcAddress();
			this.webIrcPassword = otherBuilder.getWebIrcPassword();
			this.name = otherBuilder.getName();
			this.login = otherBuilder.getLogin();
			this.version = otherBuilder.getVersion();
			this.finger = otherBuilder.getFinger();
			this.realName = otherBuilder.getRealName();
			this.channelPrefixes = otherBuilder.getChannelPrefixes();
			this.channelModeMessagePrefixes = otherBuilder.getChannelModeMessagePrefixes();
			this.snapshotsEnabled = otherBuilder.isSnapshotsEnabled();
			this.dccFilenameQuotes = otherBuilder.isDccFilenameQuotes();
			this.dccPorts.addAll(otherBuilder.getDccPorts());
			this.dccLocalAddress = otherBuilder.getDccLocalAddress();
			this.dccAcceptTimeout = otherBuilder.getDccAcceptTimeout();
			this.dccResumeAcceptTimeout = otherBuilder.getDccResumeAcceptTimeout();
			this.dccTransferBufferSize = otherBuilder.getDccTransferBufferSize();
			this.dccPassiveRequest = otherBuilder.isDccPassiveRequest();
			this.servers.addAll(otherBuilder.getServers());
			this.serverPassword = otherBuilder.getServerPassword();
			this.socketFactory = otherBuilder.getSocketFactory();
			this.localAddress = otherBuilder.getLocalAddress();
			this.encoding = otherBuilder.getEncoding();
			this.locale = otherBuilder.getLocale();
			this.socketTimeout = otherBuilder.getSocketTimeout();
			this.maxLineLength = otherBuilder.getMaxLineLength();
			this.autoSplitMessage = otherBuilder.isAutoSplitMessage();
			this.autoNickChange = otherBuilder.isAutoNickChange();
			this.messageDelay = otherBuilder.getMessageDelay();
			this.listenerManager = otherBuilder.getListenerManager();
			this.nickservPassword = otherBuilder.getNickservPassword();
			this.nickservOnSuccess = otherBuilder.getNickservOnSuccess();
			this.nickservNick = otherBuilder.getNickservNick();
			this.nickservDelayJoin = otherBuilder.isNickservDelayJoin();
			this.autoReconnect = otherBuilder.isAutoReconnect();
			this.autoReconnectDelay = otherBuilder.getAutoReconnectDelay();
			this.autoReconnectAttempts = otherBuilder.getAutoReconnectAttempts();
			this.autoJoinChannels.putAll(otherBuilder.getAutoJoinChannels());
			this.identServerEnabled = otherBuilder.isIdentServerEnabled();
			this.capEnabled = otherBuilder.isCapEnabled();
			this.capHandlers.addAll(otherBuilder.getCapHandlers());
			this.channelModeHandlers.addAll(otherBuilder.getChannelModeHandlers());
			this.shutdownHookEnabled = otherBuilder.isShutdownHookEnabled();
			this.botFactory = otherBuilder.getBotFactory();
		}

		/**
		 * The local address to bind DCC connections to. Defaults to {@link #getLocalAddress()
		 * }
		 */
		public InetAddress getDccLocalAddress() {
			return (dccLocalAddress != null) ? dccLocalAddress : localAddress;
		}

		/**
		 * Timeout for user to accept a sent DCC request. Defaults to {@link #getSocketTimeout()
		 * }
		 */
		public int getDccAcceptTimeout() {
			return (dccAcceptTimeout != -1) ? dccAcceptTimeout : socketTimeout;
		}

		/**
		 * Timeout for a user to accept a resumed DCC request. Defaults to {@link #getDccResumeAcceptTimeout()
		 * }
		 */
		public int getDccResumeAcceptTimeout() {
			return (dccResumeAcceptTimeout != -1) ? dccResumeAcceptTimeout : getDccAcceptTimeout();
		}

		/**
		 * Add a collection of cap handlers
		 *
		 * @see #getCapHandlers()
		 * @param handlers
		 * @return
		 */
		public Builder addCapHandlers(@NonNull Iterable<CapHandler> handlers) {
			for (CapHandler curHandler : handlers) {
				addCapHandler(curHandler);
			}
			return this;
		}

		/**
		 * Add a cap handler
		 *
		 * @see #getCapHandlers()
		 * @param handler
		 * @return
		 */
		public Builder addCapHandler(CapHandler handler) {
			getCapHandlers().add(handler);
			return this;
		}

		/**
		 * Add a collection of listeners to the current ListenerManager
		 *
		 * @see #getListenerManager()
		 * @param listeners
		 * @return
		 */
		public Builder addListeners(@NonNull Iterable<Listener> listeners) {
			for (Listener curListener : listeners) {
				addListener(curListener);
			}
			return this;
		}

		/**
		 * Add a listener to the current ListenerManager
		 *
		 * @see #getListenerManager()
		 * @param listener
		 * @return
		 */
		public Builder addListener(Listener listener) {
			getListenerManager().addListener(listener);
			return this;
		}

		public Builder addAutoJoinChannels(@NonNull Iterable<String> channels) {
			for (String curChannel : channels) {
				addAutoJoinChannel(curChannel);
			}
			return this;
		}

		/**
		 * Add a channel to join on connect
		 *
		 * @see #getAutoJoinChannels()
		 * @param channel
		 * @return
		 */
		public Builder addAutoJoinChannel(@NonNull String channel) {
			if (StringUtils.isBlank(channel))
				throw new RuntimeException("Channel must not be blank");
			getAutoJoinChannels().put(channel, "");
			return this;
		}

		/**
		 * Utility method for <code>{@link #getAutoJoinChannels()}.put(channel,
		 * key)</code>
		 *
		 * @param channel
		 * @return
		 */
		public Builder addAutoJoinChannel(@NonNull String channel, @NonNull String key) {
			if (StringUtils.isBlank(channel))
				throw new RuntimeException("Channel must not be blank");
			if (StringUtils.isBlank(key))
				throw new RuntimeException("Key must not be blank");
			getAutoJoinChannels().put(channel, key);
			return this;
		}

		public Builder addServer(@NonNull String server) {
			servers.add(new ServerEntry(server, 6667));
			return this;
		}

		public Builder addServer(@NonNull String server, int port) {
			servers.add(new ServerEntry(server, port));
			return this;
		}

		public Builder addServer(@NonNull ServerEntry serverEntry) {
			servers.add(serverEntry);
			return this;
		}

		public Builder addServers(@NonNull Iterable<ServerEntry> serverEnteries) {
			for (ServerEntry curServerEntry : serverEnteries)
				servers.add(curServerEntry);
			return this;
		}

		/**
		 * Sets a new ListenerManager. <b>NOTE:</b> The {@link CoreHooks} are
		 * added when this method is called. If you do not want this, remove
		 * CoreHooks with
		 * {@link ListenerManager#removeListener(org.pircbotx.hooks.Listener) }
		 *
		 * @param listenerManager The listener manager
		 */
		@SuppressWarnings("unchecked")
		public Builder setListenerManager(ListenerManager listenerManager) {
			this.listenerManager = listenerManager;
			for (Listener curListener : this.listenerManager.getListeners())
				if (curListener instanceof CoreHooks)
					return this;
			listenerManager.addListener(new CoreHooks());
			return this;
		}

		public void replaceCoreHooksListener(CoreHooks extended) {
			//Find the corehooks impl
			CoreHooks orig = null;
			for (Listener curListener : this.listenerManager.getListeners())
				if (curListener instanceof CoreHooks)
					orig = (CoreHooks) curListener;

			//Swap
			if (orig != null)
				this.listenerManager.removeListener(orig);
			addListener(extended);
		}

		/**
		 * Returns the current ListenerManager in use by this bot. Note that the
		 * default listener manager ({@link ListenerManager}) is lazy loaded
		 * here unless one was already set
		 *
		 * @return Current ListenerManager
		 */
		public ListenerManager getListenerManager() {
			if (listenerManager == null)
				setListenerManager(new ThreadedListenerManager());
			return listenerManager;
		}

		/**
		 * Build a new configuration from this Builder
		 *
		 * @return
		 */
		public Configuration buildConfiguration() {
			return new Configuration(this);
		}

		/**
		 * Create a <b>new</b> builder with the specified hostname then build a
		 * configuration. Useful for template builders
		 *
		 * @param hostname
		 * @return
		 */
		public Configuration buildForServer(String hostname) {
			return new Builder(this)
					.addServer(hostname)
					.buildConfiguration();
		}

		/**
		 * Create a <b>new</b> builder with the specified hostname and port then
		 * build a configuration. Useful for template builders
		 *
		 * @param hostname
		 * @return
		 */
		public Configuration buildForServer(String hostname, int port) {
			return new Builder(this)
					.addServer(hostname, port)
					.buildConfiguration();
		}

		/**
		 * Create a <b>new</b> builder with the specified hostname, port, and
		 * password then build a configuration. Useful for template builders
		 *
		 * @param hostname
		 * @return
		 */
		public Configuration buildForServer(String hostname, int port, String password) {
			return new Builder(this)
					.addServer(hostname, port)
					.setServerPassword(password)
					.buildConfiguration();
		}
	}

	/**
	 * Factory for various bot classes.
	 */
	public static class BotFactory {
		public UserChannelDao createUserChannelDao(PircBotX bot) {
			return new UserChannelDao(bot, bot.getConfiguration().getBotFactory());
		}

		public OutputRaw createOutputRaw(PircBotX bot) {
			return new OutputRaw(bot);
		}

		public OutputCAP createOutputCAP(PircBotX bot) {
			return new OutputCAP(bot);
		}

		public OutputIRC createOutputIRC(PircBotX bot) {
			return new OutputIRC(bot);
		}

		public OutputDCC createOutputDCC(PircBotX bot) {
			return new OutputDCC(bot);
		}

		public OutputChannel createOutputChannel(PircBotX bot, Channel channel) {
			return new OutputChannel(bot, channel);
		}

		public OutputUser createOutputUser(PircBotX bot, UserHostmask user) {
			return new OutputUser(bot, user);
		}

		public InputParser createInputParser(PircBotX bot) {
			return new InputParser(bot);
		}

		public DccHandler createDccHandler(PircBotX bot) {
			return new DccHandler(bot);
		}

		public SendChat createSendChat(PircBotX bot, User user, Socket socket) throws IOException {
			return new SendChat(user, socket, bot.getConfiguration().getEncoding());
		}

		public ReceiveChat createReceiveChat(PircBotX bot, User user, Socket socket) throws IOException {
			return new ReceiveChat(user, socket, bot.getConfiguration().getEncoding());
		}

		public SendFileTransfer createSendFileTransfer(PircBotX bot, Socket socket, User user, File file, long startPosition) {
			return new SendFileTransfer(bot.getConfiguration(), socket, user, file, startPosition);
		}

		public ReceiveFileTransfer createReceiveFileTransfer(PircBotX bot, Socket socket, User user, File file, long startPosition, long fileSize) {
			return new ReceiveFileTransfer(bot.getConfiguration(), socket, user, file, startPosition, fileSize);
		}

		public ServerInfo createServerInfo(PircBotX bot) {
			return new ServerInfo(bot);
		}

		public UserHostmask createUserHostmask(PircBotX bot, String hostmask, String nick, String login, String hostname) {
			return new UserHostmask(bot, hostmask, nick, login, hostname);
		}

		public User createUser(UserHostmask userHostmask) {
			return new User(userHostmask);
		}

		public Channel createChannel(PircBotX bot, String name) {
			return new Channel(bot, bot.getUserChannelDao(), name);
		}
	}

	@Data
	public static class ServerEntry {
		@NonNull
		private final String hostname;
		private final int port;

		@Override
		public String toString() {
			return hostname + ":" + port;
		}
	}
}
