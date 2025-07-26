package com.pkqol;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;

@Slf4j
@PluginDescriptor(
        name = "PK QoL",
        description = "Quality of life improvements for PKing"
)
public class PKQoLPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private PKQoLConfig config;

    @Override
    protected void startUp()
    {
        log.debug("PK QoL started");
    }

    @Override
    protected void shutDown()
    {
        log.debug("PK QoL stopped");
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
        // Check if pet spell blocker is enabled
        if (!config.petSpellBlocker())
        {
            return;
        }

        // Check if this is a spell cast action
        if (!"Cast".equals(event.getOption()))
        {
            return;
        }

        // Get the target NPC
        int npcIndex = event.getIdentifier();
        if (npcIndex < 0)
        {
            return;
        }

        // Get NPC from the scene using the new API
        NPC npc = client.getTopLevelWorldView().npcs().byIndex(npcIndex);
        
        if (npc == null)
        {
            return;
        }

        // Check if the NPC is a follower (pet)
        NPCComposition comp = npc.getComposition();
        if (comp != null && comp.isFollower())
        {
            // Remove the menu entry using the new API
            // Use the new Menu API to remove the entry
            client.getMenu().removeMenuEntry(event.getMenuEntry());
            log.debug("Removed spell cast menu entry for pet: {}", npc.getName());
        }
    }

    @Provides
    PKQoLConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PKQoLConfig.class);
    }
} 