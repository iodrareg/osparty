package net.osparty.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Payload sent to the queue API when a player creates a new party. The host is
 * the currently logged in player, resolved by the plugin at request time.
 */
@Data
@AllArgsConstructor
public class PartyRequest
{
	private String activity;
	private String host;
	private String description;
	private int capacity;
	private String world;

	/** Minimum kills required to apply. 0 means no requirement. */
	private int minKillCount;

	/** Minimum CM/HM/Expert kills required to apply (raids only). 0 means none. */
	private int minHardModeKillCount;

	/**
	 * RuneLite party passphrase for the live room the host has opened. Advertised
	 * so applicants can join the actual party; the API itself tracks no membership.
	 */
	private String passphrase;
}
