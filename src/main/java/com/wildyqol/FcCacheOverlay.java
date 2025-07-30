package com.wildyqol;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.playerindicators.PlayerIndicatorsConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;
import net.runelite.api.Point;


import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

@Slf4j
@Singleton
public class FcCacheOverlay extends Overlay
{
	private final Client client;
	private final WildyQoLConfig config;
	private final FcCacheService cacheService;
	private final ConfigManager configManager;

	@Inject
	FcCacheOverlay(Client client, WildyQoLConfig config, FcCacheService cacheService, ConfigManager configManager)
	{
		this.client = client;
		this.config = config;
		this.cacheService = cacheService;
		this.configManager = configManager;
		
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enableFcCache())
		{
			return null;
		}

		// Get the friends chat color from Player Indicators config
		PlayerIndicatorsConfig piConfig = configManager.getConfig(PlayerIndicatorsConfig.class);
		Color fcColor = piConfig.getFriendsChatMemberColor();

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

			// Render the player name with friends chat color (same as Player Indicators)
			final String name = Text.sanitize(player.getName());
			Point textLocation = player.getCanvasTextLocation(graphics, name, player.getLogicalHeight() + 40);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, name, fcColor);
			}
		}

		return null;
	}


} 