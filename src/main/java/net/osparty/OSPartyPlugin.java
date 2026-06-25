package net.osparty;

import net.osparty.api.PartyApiClient;
import net.osparty.api.PartyService;
import net.osparty.model.Activity;
import net.osparty.model.Applicant;
import net.osparty.party.LiveParty;
import net.osparty.party.MemberCommand;
import net.osparty.party.PartyStateMessage;
import net.osparty.party.PlayerUpdate;
import net.osparty.runewatch.RuneWatchService;
import net.osparty.ui.OSPartyPanel;
import net.osparty.ui.ApplicantOverlay;
import com.google.inject.Provides;
import java.awt.Color;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "OSParty",
	description = "Search, queue and join parties for activities around the game",
	tags = {"party", "group", "raid", "minigame", "boss", "lfg"}
)
public class OSPartyPlugin extends Plugin implements HostApplicationHandler
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private PartyApiClient apiClient;

	@Inject
	private ItemManager itemManager;

	@Inject
	private LiveParty liveParty;

	@Inject
	private RuneWatchService runeWatchService;

	@Inject
	private OSPartyConfig config;

	private OSPartyPanel panel;
	private NavigationButton navButton;
	private ApplicantOverlay applicantOverlay;

	/**
	 * Last known logged in player name. Updated on the client thread and read
	 * (volatile) from the EDT by the panel.
	 */
	private volatile String playerName;

	/** Owner (name) of the friends chat the player is currently in, or null. */
	private volatile String friendsChatOwner;
	/** Current world, or 0 when not logged in. */
	private volatile int world;

	@Override
	protected void startUp()
	{
		// Discovery/advertising goes through the real HTTP API (osrs-party-api);
		// the live party itself runs peer-to-peer (see LiveParty).
		PartyService partyService = apiClient;

		applicantOverlay = new ApplicantOverlay();
		overlayManager.add(applicantOverlay);

		// Stand up the live P2P party layer (real roster + gear/inv/stats + host
		// management); the API only advertises the room.
		liveParty.register();

		// Pull the scammer watchlist now; it refreshes periodically (see schedule).
		runeWatchService.refresh();

		panel = new OSPartyPanel(partyService, config, this::getPlayerName, this,
			this::getFriendsChatOwner, this::getCurrentWorld, itemManager, liveParty, runeWatchService);

		navButton = NavigationButton.builder()
			.tooltip("OSParty")
			.icon(ImageUtil.loadImageResource(getClass(), "panel_icon.png"))
			.priority(7)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.info("OSParty started (API {})", config.apiBaseUrl());
	}

	@Override
	protected void shutDown()
	{
		liveParty.leave();
		liveParty.unregister();
		clientToolbar.removeNavigation(navButton);
		overlayManager.remove(applicantOverlay);
		applicantOverlay = null;
		panel = null;
		navButton = null;
		playerName = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// The local player (and its name) is usually not yet available at the
		// LOGGED_IN event, so capturing it here is unreliable; we instead read
		// it each tick (see onGameTick) and only clear it on logout here.
		if (event.getGameState() == GameState.LOGIN_SCREEN
			|| event.getGameState() == GameState.HOPPING)
		{
			playerName = null;
			friendsChatOwner = null;
			world = 0;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player local = client.getLocalPlayer();
		if (local != null && local.getName() != null)
		{
			playerName = local.getName();
		}

		world = client.getWorld();

		FriendsChatManager fcm = client.getFriendsChatManager();
		friendsChatOwner = fcm != null ? fcm.getOwner() : null;

		// Push pending host state / our own live snapshot (client thread).
		liveParty.tick();
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		// Inventory or equipment changed — re-broadcast our loadout next tick.
		liveParty.markLocalDirty();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		liveParty.markLocalDirty();
	}

	@Subscribe
	public void onPlayerUpdate(PlayerUpdate event)
	{
		liveParty.onPlayerUpdate(event);
	}

	@Subscribe
	public void onPartyStateMessage(PartyStateMessage event)
	{
		liveParty.onPartyState(event);
	}

	@Subscribe
	public void onMemberCommand(MemberCommand event)
	{
		liveParty.onMemberCommand(event);
	}

	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		liveParty.onPeerJoined(event.getMemberId());
	}

	@Subscribe
	public void onUserPart(UserPart event)
	{
		liveParty.onPeerLeft(event.getMemberId());
	}

	@Schedule(period = 15, unit = ChronoUnit.MINUTES, asynchronous = true)
	public void refreshRuneWatch()
	{
		runeWatchService.refresh();
	}

	/**
	 * @return the currently logged in player's name, or {@code null} if not
	 * logged in. Safe to call from the EDT.
	 */
	public String getPlayerName()
	{
		return playerName;
	}

	/** @return owner of the friends chat the player is in, or null. Safe from the EDT. */
	public String getFriendsChatOwner()
	{
		return friendsChatOwner;
	}

	/** @return the current world, or 0 when not logged in. Safe from the EDT. */
	public int getCurrentWorld()
	{
		return world;
	}

	@Override
	public void onApplicantShown(Applicant applicant, Activity activity)
	{
		applicantOverlay.setApplicant(applicant, activity);

		StringBuilder line = new StringBuilder()
			.append(applicant.getName())
			.append(" applied to your ").append(activity.getDisplayName())
			.append(" party (cb ").append(applicant.getCombatLevel())
			.append(", KC ").append(applicant.getKillCount());
		if (activity.hasHardMode() && applicant.getHardModeKillCount() >= 0)
		{
			line.append(", ").append(activity.getHardModeLabel())
				.append(' ').append(applicant.getHardModeKillCount());
		}
		line.append("). Accept or decline in the side panel.");

		gameMessage(line.toString());
	}

	@Override
	public void onApplicantResolved(Applicant applicant, Activity activity, boolean accepted)
	{
		applicantOverlay.clear();
		gameMessage((accepted ? "Accepted " : "Declined ") + applicant.getName()
			+ " for your " + activity.getDisplayName() + " party.");
	}

	/**
	 * Post a game message to the in-game chatbox on the client thread. No-op
	 * when not logged in (the chatbox only exists in-game).
	 */
	private void gameMessage(String message)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		String formatted = ColorUtil.wrapWithColorTag("[OSParty]", Color.ORANGE) + " " + message;
		clientThread.invoke(() -> client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", formatted, null));
	}

	@Provides
	OSPartyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OSPartyConfig.class);
	}
}
