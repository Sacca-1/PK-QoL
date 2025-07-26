package com.pkqol;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("pkqol")
public interface PKQoLConfig extends Config {
    @ConfigItem(
        keyName = "petSpellBlocker",
        name = "Pet Spell Blocker",
        description = "Prevents casting spells on your pets to avoid misclicks",
        position = 1
    )
    default boolean petSpellBlocker() {
        return true;
    }
} 