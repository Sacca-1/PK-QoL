package com.wildyqol;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
		keyName = "updateMessageShown110",
		name = "Update Message Shown v1.1.0",
		description = "Internal flag to track if the v1.1.0 update message has been shown",
		hidden = true,
        position = 3
	)
	default boolean updateMessageShown110()
	{
		return false;
	}
} 