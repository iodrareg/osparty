package net.osparty;

import net.osparty.model.Activity;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreEndpoint;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.hiscore.Skill;

/**
 * Looks up a player's killcount for an activity from the OSRS hiscores, since we
 * can't read another account's boss KC locally. Results are cached per
 * (player, activity); the normal hiscores endpoint covers every account type.
 */
@Slf4j
@Singleton
public class KillcountService
{
	/** A looked-up killcount pair; {@code -1} means unknown / unranked. */
	public static final class Killcount
	{
		public final int killCount;
		public final int hardModeKillCount;

		Killcount(int killCount, int hardModeKillCount)
		{
			this.killCount = killCount;
			this.hardModeKillCount = hardModeKillCount;
		}
	}

	private final HiscoreClient hiscoreClient;
	private final Map<String, Killcount> cache = new ConcurrentHashMap<>();
	private final Map<String, Boolean> inFlight = new ConcurrentHashMap<>();

	@Inject
	private KillcountService(HiscoreClient hiscoreClient)
	{
		this.hiscoreClient = hiscoreClient;
	}

	/** @return the cached killcount for this player+activity, or {@code null} if not looked up yet. */
	public Killcount cached(String rsn, Activity activity)
	{
		if (rsn == null || activity == null)
		{
			return null;
		}
		return cache.get(key(rsn, activity));
	}

	/**
	 * Look up the activity killcount for {@code rsn} once (subsequent calls are
	 * no-ops while cached/in flight). {@code onComplete} fires on the EDT.
	 */
	public void lookup(String rsn, Activity activity, Runnable onComplete)
	{
		if (rsn == null || activity == null)
		{
			return;
		}
		String key = key(rsn, activity);
		if (cache.containsKey(key) || inFlight.putIfAbsent(key, Boolean.TRUE) != null)
		{
			return;
		}

		HiscoreSkill skill = skillFor(activity);
		if (skill == null)
		{
			// Activity has no hiscore killcount (minigames etc.).
			cache.put(key, new Killcount(-1, -1));
			inFlight.remove(key);
			return;
		}

		hiscoreClient.lookupAsync(rsn, HiscoreEndpoint.NORMAL).whenComplete((result, ex) -> {
			int kc = -1;
			int hard = -1;
			if (ex == null && result != null)
			{
				kc = count(result, skill);
				HiscoreSkill hardSkill = hardSkillFor(activity);
				if (hardSkill != null)
				{
					hard = count(result, hardSkill);
				}
			}
			else
			{
				log.debug("Hiscore lookup failed for {}", rsn, ex);
			}
			cache.put(key, new Killcount(kc, hard));
			inFlight.remove(key);
			if (onComplete != null)
			{
				SwingUtilities.invokeLater(onComplete);
			}
		});
	}

	private static int count(HiscoreResult result, HiscoreSkill skill)
	{
		Skill s = result.getSkill(skill);
		return s == null ? -1 : s.getLevel();
	}

	private static String key(String rsn, Activity activity)
	{
		return rsn.replace('\u00A0', ' ').trim().toLowerCase() + "|" + activity.getId();
	}

	private static HiscoreSkill skillFor(Activity activity)
	{
		switch (activity)
		{
			case CHAMBERS_OF_XERIC:
				return HiscoreSkill.CHAMBERS_OF_XERIC;
			case THEATRE_OF_BLOOD:
				return HiscoreSkill.THEATRE_OF_BLOOD;
			case TOMBS_OF_AMASCUT:
				return HiscoreSkill.TOMBS_OF_AMASCUT;
			case NEX:
				return HiscoreSkill.NEX;
			case NIGHTMARE:
				return HiscoreSkill.NIGHTMARE;
			case CORPOREAL_BEAST:
				return HiscoreSkill.CORPOREAL_BEAST;
			case WINTERTODT:
				return HiscoreSkill.WINTERTODT;
			case TEMPOROSS:
				return HiscoreSkill.TEMPOROSS;
			case ZALCANO:
				return HiscoreSkill.ZALCANO;
			case INFERNO:
				return HiscoreSkill.TZKAL_ZUK;
			default:
				return null; // BA, GotR, Soul Wars, Pest Control, Castle Wars
		}
	}

	private static HiscoreSkill hardSkillFor(Activity activity)
	{
		switch (activity)
		{
			case CHAMBERS_OF_XERIC:
				return HiscoreSkill.CHAMBERS_OF_XERIC_CHALLENGE_MODE;
			case THEATRE_OF_BLOOD:
				return HiscoreSkill.THEATRE_OF_BLOOD_HARD_MODE;
			case TOMBS_OF_AMASCUT:
				return HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT;
			default:
				return null;
		}
	}
}
