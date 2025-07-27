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

    @ConfigItem(
        keyName = "emptyVialBlocker",
        name = "Empty Vial Blocker",
        description = "Consumes the Use option on empty vials while in dangerous areas to avoid misclicks",
        position = 2
    )
    default boolean emptyVialBlocker() {
        return false;
    }
} 