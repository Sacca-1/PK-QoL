package com.wildyqol;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

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

	@ConfigSection(
		name = "Kill Value Hider",
		description = "Settings for hiding kill and death values in clan broadcasts",
		position = 4
	)
	String killValueHiderSection = "killValueHider";

	@ConfigItem(
		keyName = "hideOwnKillValue",
		name = "Hide Own Kill Values",
		description = "Replaces the coin value with asterisks for your own kills in clan broadcasts",
		position = 1,
		section = killValueHiderSection
	)
	default boolean hideOwnKillValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideOwnDeathValue",
		name = "Hide Own Death Values",
		description = "Replaces the coin value with asterisks for your own deaths in clan broadcasts",
		position = 2,
		section = killValueHiderSection
	)
	default boolean hideOwnDeathValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideClanmateKillValue",
		name = "Hide Clanmate Kill Values",
		description = "Replaces the coin value with asterisks for clanmate kills in clan broadcasts",
		position = 3,
		section = killValueHiderSection
	)
	default boolean hideClanmateKillValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideClanmateDeathValue",
		name = "Hide Clanmate Death Values",
		description = "Replaces the coin value with asterisks for clanmate deaths in clan broadcasts",
		position = 4,
		section = killValueHiderSection
	)
	default boolean hideClanmateDeathValue()
	{
		return false;
	}
} 