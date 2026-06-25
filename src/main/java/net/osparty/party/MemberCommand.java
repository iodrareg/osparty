package net.osparty.party;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * A targeted host -> member command. The admitted roster lives in
 * {@link PartyStateMessage}; this message exists so a specific peer can be told
 * to act on itself — namely to leave the relay room when kicked or declined.
 * Honoured cooperatively by the target client.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MemberCommand extends PartyMemberMessage
{
	public enum Action
	{
		/** Remove an admitted member; they should leave the room. */
		KICK,
		/** Decline a pending applicant; they should leave the room. */
		REJECT,
	}

	private Action action;
	private long targetMemberId;
}
