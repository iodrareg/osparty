package net.osparty.party;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * The host-authoritative state of a party, broadcast <b>only by the host</b>.
 * RuneLite's party network has no host or access control, so this message is
 * how one peer declares itself the authority and publishes the agreed roster
 * and rules. Re-sent whenever the state changes and whenever a new peer joins
 * (so newcomers learn the current state).
 *
 * <p>This is a cooperative convention, not an enforced one: a modified client
 * could ignore or spoof it. That is acceptable for an LFG tool and matches the
 * trust model of the rest of the party network.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PartyStateMessage extends PartyMemberMessage
{
	private long hostMemberId;
	private String hostName;

	/** Activity id (see {@link net.osparty.model.Activity#getId()}). */
	private String activityId;

	private int capacity;
	private int minKillCount;
	private int minHardModeKillCount;

	/** When locked, the host admits no further applicants. */
	private boolean locked;

	/** Set in the host's final broadcast to tell everyone the party is closing. */
	private boolean closed;

	/** The admitted roster (host first). Pending applicants are not listed here. */
	private List<RosterEntry> roster = new ArrayList<>();
}
