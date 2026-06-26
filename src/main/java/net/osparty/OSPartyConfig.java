package net.osparty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(OSPartyConfig.GROUP)
public interface OSPartyConfig extends Config
{
	String GROUP = "osparty";

	@ConfigSection(
		name = "Meme mode",
		description = "Optional sound effects for party events. All off by default.",
		position = 10,
		closedByDefault = true
	)
	String MEME_MODE = "memeMode";

	@ConfigItem(
		keyName = "apiBaseUrl",
		name = "API base URL",
		description = "Base URL of the party advertising API (no trailing slash).",
		position = 1
	)
	default String apiBaseUrl()
	{
		return "https://api.osparty.net";
	}

	@ConfigItem(
		keyName = "defaultCapacity",
		name = "Default party size",
		description = "Capacity pre-filled in the create-party form.",
		position = 2
	)
	default int defaultCapacity()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "runeWatch",
		name = "RuneWatch warnings",
		description = "Warn when a party member or applicant is on the RuneWatch / We Do Raids scammer watchlist.",
		position = 3
	)
	default boolean runeWatch()
	{
		return true;
	}

	@ConfigItem(
		keyName = "inGamePrompts",
		name = "In-game join prompts",
		description = "As a host, show Accept/Decline for new applicants in the in-game chatbox (not just the side panel).",
		position = 4
	)
	default boolean inGamePrompts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "readyCheckSound",
		name = "Ready-check sounds",
		description = "Play sounds for ready checks (when one starts, and when everyone is ready).",
		position = 5,
		section = MEME_MODE
	)
	default boolean readyCheckSound()
	{
		return false;
	}

	@ConfigItem(
		keyName = "kickSound",
		name = "Kick sound",
		description = "Play a sound when you are kicked from a party.",
		position = 7,
		section = MEME_MODE
	)
	default boolean kickSound()
	{
		return false;
	}

	@ConfigItem(
		keyName = "friendsChatRequestSound",
		name = "Friends-chat request sound",
		description = "Play a sound when a host asks you to join their friends chat.",
		position = 8,
		section = MEME_MODE
	)
	default boolean friendsChatRequestSound()
	{
		return false;
	}
}
