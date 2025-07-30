package com.wildyqol;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.playerindicators.PlayerIndicatorsConfig;
import net.runelite.client.plugins.playerindicators.PlayerNameLocation;
import net.runelite.client.plugins.playerindicators.PlayerIndicatorsPlugin;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;
import net.runelite.api.Point;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class FcCacheOverlay extends Overlay
{
	private final Client client;
	private final WildyQoLConfig config;
	private final FcCacheService cacheService;
	private final ConfigManager configManager;
	private final PartyService partyService;
	private final PluginManager pluginManager;
	private final AtomicReference<PiSnapshot> currentSnapshot = new AtomicReference<>();
	private final AtomicReference<Boolean> playerIndicatorsEnabled = new AtomicReference<>(null);

	private static final int ACTOR_OVERHEAD_TEXT_MARGIN = 40;
	private static final int ACTOR_HORIZONTAL_TEXT_MARGIN = 10;

	@Inject
	FcCacheOverlay(Client client, WildyQoLConfig config, FcCacheService cacheService, ConfigManager configManager, PartyService partyService, PluginManager pluginManager, EventBus eventBus)
	{
		this.client = client;
		this.config = config;
		this.cacheService = cacheService;
		this.configManager = configManager;
		this.partyService = partyService;
		this.pluginManager = pluginManager;
		
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_HIGH);
		
		// Register with event bus to receive config changes
		eventBus.register(this);
		
		// Initialize the snapshot
		updateSnapshot();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(PlayerIndicatorsConfig.GROUP))
		{
			updateSnapshot();
		}
		// Reset the cached plugin enabled state when any config changes
		playerIndicatorsEnabled.set(null);
	}

	private void updateSnapshot()
	{
		PlayerIndicatorsConfig piConfig = configManager.getConfig(PlayerIndicatorsConfig.class);
		PiSnapshot snapshot = new PiSnapshot(
			piConfig.drawTiles(),
			piConfig.playerNamePosition(),
			piConfig.highlightPartyMembers(),
			piConfig.highlightFriends(),
			piConfig.highlightFriendsChat(),
			piConfig.getFriendsChatMemberColor()
		);
		currentSnapshot.set(snapshot);
		log.debug("Updated Player Indicators snapshot");
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableFcCache())
		{
			return null;
		}

		// Check if Player Indicators plugin is enabled (with caching)
		Boolean cached = this.playerIndicatorsEnabled.get();
		if (cached == null)
		{
			cached = pluginManager.getPlugins().stream()
				.anyMatch(plugin -> plugin instanceof PlayerIndicatorsPlugin && pluginManager.isPluginEnabled(plugin));
			this.playerIndicatorsEnabled.set(cached);
		}
		if (!cached)
		{
			return null;
		}

		PiSnapshot snapshot = currentSnapshot.get();
		if (snapshot == null)
		{
			return null;
		}

		Player localPlayer = client.getLocalPlayer();
		for (Player player : client.getTopLevelWorldView().players())
		{
			if (player == null || player.getName() == null)
			{
				continue;
			}

			// Skip local player
			if (player == localPlayer)
			{
				continue;
			}

			// Skip if already handled by vanilla Player Indicators
			if (player.isFriendsChatMember())
			{
				continue;
			}

			// Check if this player is in our cache
			if (!cacheService.isCached(player.getName()))
			{
				continue;
			}

			// Check if we should render this player as friends chat
			if (!shouldRenderAsFc(player, snapshot))
			{
				continue;
			}

			// Render the player name
			renderPlayerName(graphics, player, snapshot);
			
			// Render tile if enabled
			if (snapshot.isDrawTiles())
			{
				renderPlayerTile(graphics, player, snapshot);
			}
		}

		return null;
	}

	private boolean shouldRenderAsFc(Player player, PiSnapshot snapshot)
	{
		// Check if Player Indicators will draw friends chat
		if (snapshot.getFc() == PlayerIndicatorsConfig.HighlightSetting.DISABLED)
		{
			return false;
		}

		// Check if we're in PvP area (for PVP setting)
		boolean inPvpArea = isInPvpArea();
		boolean fcEnabled = snapshot.getFc() == PlayerIndicatorsConfig.HighlightSetting.ENABLED ||
			(snapshot.getFc() == PlayerIndicatorsConfig.HighlightSetting.PVP && inPvpArea);

		if (!fcEnabled)
		{
			return false;
		}

		// Check if higher priority indicators would override
		if (snapshot.getParty() != PlayerIndicatorsConfig.HighlightSetting.DISABLED)
		{
			boolean partyEnabled = snapshot.getParty() == PlayerIndicatorsConfig.HighlightSetting.ENABLED ||
				(snapshot.getParty() == PlayerIndicatorsConfig.HighlightSetting.PVP && inPvpArea);
			if (partyEnabled && isPartyMember(player))
			{
				return false;
			}
		}

		if (snapshot.getFriends() != PlayerIndicatorsConfig.HighlightSetting.DISABLED)
		{
			boolean friendsEnabled = snapshot.getFriends() == PlayerIndicatorsConfig.HighlightSetting.ENABLED ||
				(snapshot.getFriends() == PlayerIndicatorsConfig.HighlightSetting.PVP && inPvpArea);
			if (friendsEnabled && player.isFriend())
			{
				return false;
			}
		}

		return true;
	}

	private boolean isInPvpArea()
	{
		// Check wilderness
		int wildernessVarbit = client.getVarbitValue(5963); // IN_WILDERNESS varbit ID
		if (wildernessVarbit == 1)
		{
			return true;
		}

		// Check PvP world
		int pvpVarbit = client.getVarbitValue(6116); // PVP_AREA_CLIENT varbit ID
		return pvpVarbit == 1;
	}

	private boolean isPartyMember(Player player)
	{
		return partyService.isInParty() && partyService.getMemberByDisplayName(player.getName()) != null;
	}

	private void renderPlayerName(Graphics2D graphics, Player player, PiSnapshot snapshot)
	{
		final PlayerNameLocation drawPlayerNamesConfig = snapshot.getNamePos();
		if (drawPlayerNamesConfig == PlayerNameLocation.DISABLED)
		{
			return;
		}

		final int zOffset;
		switch (drawPlayerNamesConfig)
		{
			case MODEL_CENTER:
			case MODEL_RIGHT:
				zOffset = player.getLogicalHeight() / 2;
				break;
			default:
				zOffset = player.getLogicalHeight() + ACTOR_OVERHEAD_TEXT_MARGIN;
		}

		final String name = Text.sanitize(player.getName());
		Point textLocation = player.getCanvasTextLocation(graphics, name, zOffset);

		if (drawPlayerNamesConfig == PlayerNameLocation.MODEL_RIGHT)
		{
			textLocation = player.getCanvasTextLocation(graphics, "", zOffset);

			if (textLocation == null)
			{
				return;
			}

			textLocation = new Point(textLocation.getX() + ACTOR_HORIZONTAL_TEXT_MARGIN, textLocation.getY());
		}

		if (textLocation == null)
		{
			return;
		}

		OverlayUtil.renderTextLocation(graphics, textLocation, name, snapshot.getFcColor());
	}

	private void renderPlayerTile(Graphics2D graphics, Player player, PiSnapshot snapshot)
	{
		final Polygon poly = player.getCanvasTilePoly();

		if (poly != null)
		{
			OverlayUtil.renderPolygon(graphics, poly, snapshot.getFcColor());
		}
	}

	// Immutable snapshot of Player Indicators config
	private static class PiSnapshot
	{
		private final boolean drawTiles;
		private final PlayerNameLocation namePos;
		private final PlayerIndicatorsConfig.HighlightSetting party;
		private final PlayerIndicatorsConfig.HighlightSetting friends;
		private final PlayerIndicatorsConfig.HighlightSetting fc;
		private final Color fcColor;

		public PiSnapshot(boolean drawTiles, PlayerNameLocation namePos,
						 PlayerIndicatorsConfig.HighlightSetting party,
						 PlayerIndicatorsConfig.HighlightSetting friends,
						 PlayerIndicatorsConfig.HighlightSetting fc,
						 Color fcColor)
		{
			this.drawTiles = drawTiles;
			this.namePos = namePos;
			this.party = party;
			this.friends = friends;
			this.fc = fc;
			this.fcColor = fcColor;
		}

		public boolean isDrawTiles() { return drawTiles; }
		public PlayerNameLocation getNamePos() { return namePos; }
		public PlayerIndicatorsConfig.HighlightSetting getParty() { return party; }
		public PlayerIndicatorsConfig.HighlightSetting getFriends() { return friends; }
		public PlayerIndicatorsConfig.HighlightSetting getFc() { return fc; }
		public Color getFcColor() { return fcColor; }
	}
} 