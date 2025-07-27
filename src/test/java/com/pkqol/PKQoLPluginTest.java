package com.pkqol;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PKQoLPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(PKQoLPlugin.class);
        RuneLite.main(args);
    }
} 