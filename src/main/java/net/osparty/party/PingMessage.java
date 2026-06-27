package net.osparty.party;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.runelite.client.party.messages.PartyMemberMessage;

/**
 * A map ping: a party member highlights a world tile for everyone to see (e.g.
 * "meet here" / "attack this"). The sender's display name is drawn in the centre
 * of the tile and the ping is rendered in the sender's chosen colour, both of
 * which travel with the message so peers don't need to resolve them locally.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PingMessage extends PartyMemberMessage
{
	/** World coordinates of the pinged tile. */
	private int worldX;
	private int worldY;
	private int plane;

	/** Display name of the player who pinged (drawn in the tile centre). */
	private String name;

	/** Sender's configured ping colour, as a packed ARGB/RGB int. */
	private int color;
}
