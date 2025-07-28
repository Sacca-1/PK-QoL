package com.wildyqol;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.WorldType;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.GameState;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.api.ChatMessageType;

@Slf4j
@PluginDescriptor(
        name = "Wildy QoL",
        description = "Quality of life improvements for wilderness activities"
)
public class WildyQoLPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ConfigManager configManager;

    @Inject
    private WildyQoLConfig config;

    @Inject
    private ChatMessageManager chatMessageManager;

    private boolean shouldShowUpdateMessage = false;

    @Override
    protected void startUp()
    {
        log.debug("Pet Spell Blocker enabled: {}", config.petSpellBlocker());
        log.debug("Empty Vial Blocker enabled: {}", config.emptyVialBlocker());
        log.debug("NPC Spell Blocker enabled: {}", config.npcSpellBlocker());
        
        // Check if we should show update message (but don't show it yet)
        if (!config.updateMessageShown110())
        {
            shouldShowUpdateMessage = true;
        }
    }

    @Override
    protected void shutDown()
    {
        log.debug("Wildy QoL stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        // Show update message if needed
        if (shouldShowUpdateMessage)
        {
            showUpdateMessage();
            configManager.setConfiguration("wildyqol", "updateMessageShown110", true);
            shouldShowUpdateMessage = false;
        }
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event)
    {

        if (config.petSpellBlocker())
        {
            handlePetSpellBlock(event);
        }

        if (config.npcSpellBlocker())
        {
            handleNpcSpellBlock(event);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        // Intercept left-click "Use" on vial and cancel it (do nothing)
        if (!config.emptyVialBlocker())
        {
            return;
        }

        // Only on dangerous areas
        if (!inDangerousArea())
        {
            return;
        }

        // Check if the clicked option is "Use" on a vial item
        if ("Use".equals(event.getMenuEntry().getOption()))
        {
            boolean targetContainsVial = event.getMenuEntry().getTarget() != null && event.getMenuEntry().getTarget().contains("Vial");
            if (targetContainsVial)
            {
                event.consume(); // Cancels the action, effectively doing nothing
            }
        }
    }

    private void handlePetSpellBlock(MenuEntryAdded event)
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

    private void handleNpcSpellBlock(MenuEntryAdded event)
    {
        // Ensure this is a spell cast action
        if (!"Cast".equals(event.getOption()))
        {
            return;
        }

        // Only remove in dangerous areas
        if (!inDangerousArea())
        {
            return;
        }

        // Resolve NPC by index
        int npcIndex = event.getIdentifier();
        if (npcIndex < 0)
        {
            return;
        }

        NPC npc = client.getTopLevelWorldView().npcs().byIndex(npcIndex);

        if (npc == null)
        {
            return;
        }

        // Skip followers (pets) – handled by petSpellBlocker
        NPCComposition comp = npc.getComposition();
        if (comp != null && comp.isFollower())
        {
            return;
        }

        // Remove the menu entry using the new API
        client.getMenu().removeMenuEntry(event.getMenuEntry());
        log.debug("Removed spell cast menu entry for NPC: {}", npc.getName());
    }

    private boolean inDangerousArea()
    {
        // Check if in wilderness
        int wildernessVarbit = client.getVarbitValue(5963); // IN_WILDERNESS varbit ID
        boolean inWilderness = wildernessVarbit == 1;
        
        if (inWilderness)
        {
            return true;
        }

        // Check world types
        for (WorldType worldType : client.getWorldType())
        {
            if (worldType == WorldType.PVP || 
                worldType == WorldType.DEADMAN ||
                worldType == WorldType.HIGH_RISK)
            {
                return true;
            }
        }

        return false;
    }

    private void showUpdateMessage()
    {
        chatMessageManager.queue(QueuedMessage.builder()
            .type(ChatMessageType.GAMEMESSAGE)
            .runeLiteFormattedMessage("<col=00ff00>Wildy QoL v1.1.0:</col> Pet Spell Blocker plugin name changed to \"Wildy QoL\" with two added features: NPC Spell Blocker and Empty Vial Blocker.")
            .build());
    }

    @Provides
    WildyQoLConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WildyQoLConfig.class);
    }
} 