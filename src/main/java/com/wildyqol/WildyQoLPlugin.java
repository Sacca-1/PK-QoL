package com.wildyqol;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.events.FriendsChatMemberJoined;
import net.runelite.api.GameState;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.api.ChatMessageType;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @Inject
    private FcCacheService fcCacheService;

    @Inject
    private FcCacheOverlay fcCacheOverlay;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ScheduledExecutorService executor;

    private boolean shouldShowUpdateMessage = false;

    @Override
    protected void startUp()
    {
        log.debug("Pet Spell Blocker enabled: {}", config.petSpellBlocker());
        log.debug("Empty Vial Blocker enabled: {}", config.emptyVialBlocker());
        log.debug("Friends Chat Cache enabled: {}", config.enableFcCache());
        
        // Check if we should show update message (but don't show it yet)
        if (!config.updateMessageShown110())
        {
            shouldShowUpdateMessage = true;
        }

        // Add the FC cache overlay
        overlayManager.add(fcCacheOverlay);

        // Schedule cache cleanup every minute
        executor.scheduleAtFixedRate(() -> {
            if (config.enableFcCache())
            {
                fcCacheService.purgeExpired();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    protected void shutDown()
    {
        log.debug("Wildy QoL stopped");
        
        // Remove the FC cache overlay
        overlayManager.remove(fcCacheOverlay);
        
        // Clear the cache
        fcCacheService.clear();
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

    @Subscribe
    public void onFriendsChatMemberJoined(FriendsChatMemberJoined event)
    {
        if (!config.enableFcCache())
        {
            return;
        }

        // Cache the new friends chat member
        fcCacheService.cache(event.getMember().getName());
        log.debug("Cached new FC member from FriendsChatMemberJoined: {}", event.getMember().getName());
    }



    @Subscribe
    public void onPlayerSpawned(PlayerSpawned event)
    {
        if (!config.enableFcCache())
        {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || player.getName() == null)
        {
            return;
        }

        		// If player is already a friends chat member, cache them for future world hops
		if (player.isFriendsChatMember())
        {
            fcCacheService.cache(player.getName());
            			log.debug("Cached existing FC member: {}", player.getName());
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
            .runeLiteFormattedMessage("<col=00ff00>Wildy QoL v1.1.0:</col> Pet Spell Blocker plugin name changed to \"Wildy QoL\" with added feature: Empty Vial Blocker.")
            .build());
    }

    @Provides
    WildyQoLConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WildyQoLConfig.class);
    }

    @Provides
    FcCacheOverlay provideFcCacheOverlay(Client client, WildyQoLConfig config, FcCacheService cacheService, ConfigManager configManager)
    {
        return new FcCacheOverlay(client, config, cacheService, configManager);
    }
} 