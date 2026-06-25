package net.osparty.api;

import net.osparty.model.Activity;
import net.osparty.model.Applicant;
import net.osparty.model.Applicant.EquipmentSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Generates fake {@link Applicant}s for the host-side application mock. Stats
 * and killcounts are randomised; the combat level is derived from the rolled
 * stats with the live OSRS formula so the numbers stay self-consistent. Each
 * applicant also gets a plausible worn-gear set and a raid inventory so the
 * Current-tab inspection screen has something to render.
 */
public final class MockApplicants
{
	private static final String[] NAMES = {
		"Zarphan", "Iron Gulp", "Lumby Lad", "Maxed Mia", "No Life Bob",
		"Cabbage King", "Gim Goblin", "Pure Pete", "Tick Manipoo", "Splashy Sam",
	};

	private MockApplicants()
	{
	}

	public static Applicant randomFor(Activity activity)
	{
		ThreadLocalRandom rng = ThreadLocalRandom.current();

		int attack = rng.nextInt(75, 100);
		int strength = rng.nextInt(75, 100);
		int defence = rng.nextInt(70, 100);
		int hitpoints = rng.nextInt(80, 100);
		int ranged = rng.nextInt(80, 100);
		int magic = rng.nextInt(80, 100);
		int prayer = rng.nextInt(70, 100);

		Map<String, Integer> stats = new LinkedHashMap<>();
		stats.put("Attack", attack);
		stats.put("Strength", strength);
		stats.put("Defence", defence);
		stats.put("Hitpoints", hitpoints);
		stats.put("Ranged", ranged);
		stats.put("Magic", magic);
		stats.put("Prayer", prayer);

		Applicant applicant = new Applicant();
		applicant.setName(NAMES[rng.nextInt(NAMES.length)]);
		applicant.setStats(stats);
		applicant.setCombatLevel(combatLevel(attack, strength, defence, hitpoints, ranged, magic, prayer));
		applicant.setKillCount(rng.nextInt(5, 1200));
		applicant.setHardModeKillCount(activity.hasHardMode() ? rng.nextInt(0, 400) : -1);

		Loadout loadout = Loadout.values()[rng.nextInt(Loadout.values().length)];
		applicant.setEquipment(loadout.equipment());
		applicant.setInventory(loadout.inventory());
		return applicant;
	}

	/**
	 * A handful of canned end-game loadouts (melee / ranged / mage). Item ids are
	 * real OSRS ids so {@code ItemManager} can resolve real icons; any id it
	 * can't resolve simply renders as an empty slot. {@code -1} marks an empty
	 * equipment/inventory slot.
	 */
	private enum Loadout
	{
		MELEE(
			new int[]{
				10828,  // HEAD   Helm of neitiznot
				21295,  // CAPE   Infernal cape
				19553,  // AMULET Amulet of torture
				-1,     // AMMO
				22325,  // WEAPON Scythe of vitur
				11832,  // BODY   Bandos chestplate
				-1,     // SHIELD (scythe is two-handed)
				11834,  // LEGS   Bandos tassets
				22983,  // GLOVES Ferocious gloves
				13239,  // BOOTS  Primordial boots
				25485,  // RING   Ultor ring
			},
			new int[]{
				12695, 22325, 6685, 6685, 6685, 6685, 3024, 3024,
				2434, 12625, 13441, 13441, 12791, 19553, -1, -1,
			}),
		RANGED(
			new int[]{
				11826,  // HEAD   Armadyl helmet
				22109,  // CAPE   Ava's assembler
				19547,  // AMULET Necklace of anguish
				11212,  // AMMO   Dragon arrow
				20997,  // WEAPON Twisted bow
				11828,  // BODY   Armadyl chestplate
				-1,     // SHIELD (tbow is two-handed)
				11830,  // LEGS   Armadyl chainskirt
				7462,   // GLOVES Barrows gloves
				13237,  // BOOTS  Pegasian boots
				11771,  // RING   Archers ring (i)
			},
			new int[]{
				2444, 6685, 6685, 6685, 6685, 3024, 3024, 3024,
				2434, 12625, 13441, 13441, 13441, 12791, 11212, -1,
			}),
		MAGE(
			new int[]{
				21018,  // HEAD   Ancestral hat
				21791,  // CAPE   Imbued saradomin cape
				12002,  // AMULET Occult necklace
				-1,     // AMMO
				22323,  // WEAPON Sanguinesti staff
				21021,  // BODY   Ancestral robe top
				12825,  // SHIELD Arcane spirit shield
				21024,  // LEGS   Ancestral robe bottom
				19544,  // GLOVES Tormented bracelet
				13235,  // BOOTS  Eternal boots
				11770,  // RING   Seers ring (i)
			},
			new int[]{
				6685, 6685, 6685, 6685, 3024, 3024, 3024, 2434,
				2434, 12625, 13441, 13441, 12791, 12002, -1, -1,
			}),
		;

		private final int[] equipment;
		private final int[] inventory;

		Loadout(int[] equipment, int[] inventory)
		{
			this.equipment = Arrays.copyOf(equipment, EquipmentSlot.COUNT);
			this.inventory = padInventory(inventory);
		}

		int[] equipment()
		{
			return equipment.clone();
		}

		int[] inventory()
		{
			return inventory.clone();
		}

		/** Pad (or trim) a supply list to the fixed 28-slot inventory size. */
		private static int[] padInventory(int[] items)
		{
			List<Integer> slots = new ArrayList<>();
			for (int item : items)
			{
				slots.add(item);
			}
			while (slots.size() < 28)
			{
				slots.add(-1);
			}
			int[] out = new int[28];
			for (int i = 0; i < 28; i++)
			{
				out[i] = slots.get(i);
			}
			return out;
		}
	}

	/** OSRS combat level formula. */
	private static int combatLevel(int attack, int strength, int defence, int hitpoints,
		int ranged, int magic, int prayer)
	{
		double base = 0.25 * (defence + hitpoints + Math.floor(prayer / 2.0));
		double melee = 0.325 * (attack + strength);
		double range = 0.325 * Math.floor(ranged * 1.5);
		double mage = 0.325 * Math.floor(magic * 1.5);
		return (int) Math.floor(base + Math.max(melee, Math.max(range, mage)));
	}
}
