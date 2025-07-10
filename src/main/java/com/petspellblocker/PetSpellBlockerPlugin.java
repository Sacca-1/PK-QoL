package com.petspellblocker;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Pet Spell Blocker",
        description = "Prevents casting spells on your follower pets to avoid misclicks"
)
public class PetSpellBlockerPlugin extends Plugin
{
    @Inject
    private Client client;

    @Override
    protected void startUp()
    {
        log.debug("Pet Spell Blocker started");
    }

    @Override
    protected void shutDown()
    {
        log.debug("Pet Spell Blocker stopped");
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {
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

        // Get NPCs from the scene
        java.util.List<NPC> npcs = client.getNpcs();
        
        NPC npc = null;
        for (NPC n : npcs)
        {
            if (n.getIndex() == npcIndex)
            {
                npc = n;
                break;
            }
        }
        
        if (npc == null)
        {
            return;
        }

        // Check if the NPC is a follower (pet)
        NPCComposition comp = npc.getComposition();
        if (comp != null && comp.isFollower())
        {
            // Remove the menu entry by replacing the menu entries array without this entry
            MenuEntry[] entries = client.getMenuEntries();
            if (entries.length > 0)
            {
                MenuEntry[] newEntries = new MenuEntry[entries.length - 1];
                System.arraycopy(entries, 0, newEntries, 0, entries.length - 1);
                client.setMenuEntries(newEntries);
                log.debug("Removed spell cast menu entry for pet: {}", npc.getName());
            }
        }
    }
} 