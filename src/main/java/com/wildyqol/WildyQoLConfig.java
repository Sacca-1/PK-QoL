package com.wildyqol;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("wildyqol")
public interface WildyQoLConfig extends Config
{
	@ConfigItem(
		keyName = "petSpellBlocker",
		name = "Pet Spell Blocker",
		description = "Removes 'Cast' menu entries on pets",
		position = 1
	)
	default boolean petSpellBlocker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "emptyVialBlocker",
		name = "Empty Vial Blocker",
		description = "Prevents left-clicking 'Use' on empty vials in dangerous areas",
		position = 2
	)
	default boolean emptyVialBlocker()
	{
		return false;
	}

	@ConfigItem(
		keyName = "updateMessageShown120",
		name = "Update Message Shown v1.2.0",
		description = "Internal flag to track if the v1.2.0 update message has been shown",
		hidden = true,
        position = 3
	)
	default boolean updateMessageShown120()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableFcCache",
		name = "Cache friends-chat players",
		description = "Treat recently-seen FC members as FC for player indicators on world hop",
		position = 4
	)
	default boolean enableFcCache()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fcCacheMinutes",
		name = "Cache duration (minutes)",
		description = "How long to remember FC members after world hop",
		position = 5
	)
	@Range(min = 1)
	default int fcCacheMinutes()
	{
		return 10;
	}


} 