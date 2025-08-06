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
import net.runelite.api.events.ChatMessage;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.api.ChatMessageType;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
@PluginDescriptor(
        name = "Wildy QoL",
        description = "Quality of life improvements for wilderness activities"
)
public class WildyQoLPlugin extends Plugin
{
	// Regex patterns for parsing clan broadcasts
	private static final Pattern KILL_PATTERN = Pattern.compile("(.+?) has defeated (.+?) and received \\(([\\d,]+ coins)\\) worth of loot!");
	private static final Pattern DEATH_PATTERN = Pattern.compile("(.+?) has been defeated by (.+?) in The Wilderness and lost \\(([\\d,]+ coins)\\) worth of loot");

	// Data class for parsed broadcast information
	static class ParsedBroadcast
	{
		private final boolean isKill;
		private final String killer;
		private final String victim;
		private final String valueToken;

		public ParsedBroadcast(boolean isKill, String killer, String victim, String valueToken)
		{
			this.isKill = isKill;
			this.killer = killer;
			this.victim = victim;
			this.valueToken = valueToken;
		}

		public boolean isKill()
		{
			return isKill;
		}

		public String killer()
		{
			return killer;
		}

		public String victim()
		{
			return victim;
		}

		public String valueToken()
		{
			return valueToken;
		}
	}

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
    public void onChatMessage(ChatMessage event)
    {
        // Only process clan messages
        if (event.getType() != ChatMessageType.CLAN_MESSAGE && 
            event.getType() != ChatMessageType.CLAN_GIM_MESSAGE &&
            event.getType() != ChatMessageType.CLAN_GUEST_MESSAGE)
        {
            return;
        }

        String message = event.getMessage();
        ParsedBroadcast broadcast = parseBroadcast(message);
        
        if (broadcast == null)
        {
            return; // Not a kill/death broadcast
        }

        // Check if we should hide the value
        boolean shouldHide = shouldHideValue(broadcast);
        
        if (shouldHide)
        {
            String redactedValue = redactValue(broadcast.valueToken());
            String modifiedMessage = message.replace(broadcast.valueToken(), redactedValue);
            
            MessageNode messageNode = event.getMessageNode();
            messageNode.setRuneLiteFormatMessage(modifiedMessage);
            client.refreshChat();
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

    	/**
	 * Parses a clan broadcast message to extract kill/death information
	 * @param message The raw message text
	 * @return ParsedBroadcast object if it's a kill/death message, null otherwise
	 */
	private ParsedBroadcast parseBroadcast(String message)
    {
        // Try to match kill pattern first
        Matcher killMatcher = KILL_PATTERN.matcher(message);
        if (killMatcher.find())
        {
            return new ParsedBroadcast(
                true, // isKill
                killMatcher.group(1), // killer
                killMatcher.group(2), // victim
                killMatcher.group(3)  // valueToken
            );
        }

        // Try to match death pattern
        Matcher deathMatcher = DEATH_PATTERN.matcher(message);
        if (deathMatcher.find())
        {
            return new ParsedBroadcast(
                false, // isKill
                deathMatcher.group(2), // killer (group 2 is the opponent)
                deathMatcher.group(1), // victim (group 1 is the defeated player)
                deathMatcher.group(3)  // valueToken
            );
        }

        return null; // Not a kill/death broadcast
    }

    	/**
	 * Determines if the value should be hidden based on the broadcast type and player involvement
	 * @param broadcast The parsed broadcast information
	 * @return true if the value should be hidden, false otherwise
	 */
	private boolean shouldHideValue(ParsedBroadcast broadcast)
	{
		String playerName = client.getLocalPlayer().getName();
		
		if (broadcast.isKill())
		{
			// Check if player is the killer (the one who defeated someone)
			if (broadcast.killer().equals(playerName))
			{
				return config.hideOwnKillValue();
			}
			else
			{
				return config.hideClanmateKillValue();
			}
		}
		else
		{
			// Check if player is the victim (the one who was defeated)
			if (broadcast.victim().equals(playerName))
			{
				return config.hideOwnDeathValue();
			}
			else
			{
				return config.hideClanmateDeathValue();
			}
		}
	}

    	/**
	 * Redacts a value by replacing digits and commas with asterisks
	 * @param valueToken The original value token (e.g., "19,030 coins" or "13,782,115 gp")
	 * @return The redacted value token
	 */
	private String redactValue(String valueToken)
    {
        // Replace all digits and commas with asterisks, but preserve the suffix
        return valueToken.replaceAll("[\\d,]", "*");
    }

    @Provides
    WildyQoLConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(WildyQoLConfig.class);
    }
} 