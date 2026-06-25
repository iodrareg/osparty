package net.osparty.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One admitted member in the host-authoritative roster carried by
 * {@link PartyStateMessage}. Identified by the relay-assigned {@code memberId}
 * plus a display {@code name}. Kept Gson-friendly (no-arg constructor) so it
 * serialises cleanly inside a party message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RosterEntry
{
	private long memberId;
	private String name;
}
