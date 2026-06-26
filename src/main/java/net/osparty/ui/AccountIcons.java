package net.osparty.ui;

import javax.swing.ImageIcon;
import net.runelite.api.vars.AccountType;
import net.runelite.client.util.ImageUtil;

/**
 * The in-game account-type badges (ironman variants), bundled as PNGs so we can
 * show the real icon next to a player's name instead of a text tag.
 */
final class AccountIcons
{
	private static final ImageIcon IRONMAN = load("ironman");
	private static final ImageIcon HARDCORE_IRONMAN = load("hardcore_ironman");
	private static final ImageIcon ULTIMATE_IRONMAN = load("ultimate_ironman");
	private static final ImageIcon GROUP_IRONMAN = load("group_ironman");
	private static final ImageIcon HARDCORE_GROUP_IRONMAN = load("hardcore_group_ironman");

	private AccountIcons()
	{
	}

	private static ImageIcon load(String name)
	{
		return new ImageIcon(ImageUtil.loadImageResource(AccountIcons.class, "/net/osparty/icons/" + name + ".png"));
	}

	/** @return the badge for {@code type}, or {@code null} for a normal account / unknown. */
	static ImageIcon forType(AccountType type)
	{
		if (type == null)
		{
			return null;
		}
		switch (type)
		{
			case IRONMAN:
				return IRONMAN;
			case HARDCORE_IRONMAN:
				return HARDCORE_IRONMAN;
			case ULTIMATE_IRONMAN:
				return ULTIMATE_IRONMAN;
			case GROUP_IRONMAN:
				return GROUP_IRONMAN;
			case HARDCORE_GROUP_IRONMAN:
				return HARDCORE_GROUP_IRONMAN;
			default:
				return null;
		}
	}
}
