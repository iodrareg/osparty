package net.osparty.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The set of group activities a player can queue for. Each value carries a
 * stable {@link #id} used when talking to the queue API, a human readable
 * {@link #displayName} shown in the UI, and the party size bounds the activity
 * supports.
 *
 * <p>{@link #hardModeLabel} names the activity's harder kill variant (e.g.
 * Challenge Mode for Chambers of Xeric) where one exists, so parties can set a
 * separate minimum requirement for it; it is {@code null} for activities with
 * no such variant.
 */
@Getter
@RequiredArgsConstructor
public enum Activity
{
	CHAMBERS_OF_XERIC("cox", "Chambers of Xeric", 1, 100, "CM"),
	THEATRE_OF_BLOOD("tob", "Theatre of Blood", 1, 5, "HM"),
	TOMBS_OF_AMASCUT("toa", "Tombs of Amascut", 1, 8, "Expert"),
	NEX("nex", "Nex", 1, 40, null),
	NIGHTMARE("nightmare", "The Nightmare", 1, 80, null),
	CORPOREAL_BEAST("corp", "Corporeal Beast", 1, 30, null),
	BARBARIAN_ASSAULT("ba", "Barbarian Assault", 4, 5, null),
	WINTERTODT("wintertodt", "Wintertodt", 1, 30, null),
	TEMPOROSS("tempoross", "Tempoross", 1, 30, null),
	ZALCANO("zalcano", "Zalcano", 1, 30, null),
	GUARDIANS_OF_THE_RIFT("gotr", "Guardians of the Rift", 1, 30, null),
	SOUL_WARS("soulwars", "Soul Wars", 1, 40, null),
	PEST_CONTROL("pestcontrol", "Pest Control", 1, 25, null),
	CASTLE_WARS("castlewars", "Castle Wars", 1, 100, null),
	INFERNO("inferno", "The Inferno", 1, 1, null),
	;

	private final String id;
	private final String displayName;
	private final int minPartySize;
	private final int maxPartySize;
	private final String hardModeLabel;

	public boolean hasHardMode()
	{
		return hardModeLabel != null;
	}

	@Override
	public String toString()
	{
		return displayName;
	}

	public static Activity fromId(String id)
	{
		for (Activity activity : values())
		{
			if (activity.id.equals(id))
			{
				return activity;
			}
		}
		return null;
	}
}
