package com.petspellblocker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PetSpellBlockerPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(PetSpellBlockerPlugin.class);
        RuneLite.main(args);
    }
} 