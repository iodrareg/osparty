package net.osparty.party;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * Host -> member prompt telling a specific peer how to actually join the raid:
 * join the host's friends chat (CoX), apply on the Theatre of Blood notice board,
 * or apply on the Grouping Obelisk (ToA). The target client shows it as a brief,
 * self-dismissing in-game popup. Purely cooperative: it cannot move anyone.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FcRequestMessage extends PartyMemberMessage
{
	/** Ignored by everyone but this member. */
	private long targetMemberId;

	private String hostName;

	/** "FC" (default), "NOTICE_BOARD" or "OBELISK". */
	private String kind;

	/** The friends-chat name (only for kind "FC"). */
	private String friendsChat;
}
