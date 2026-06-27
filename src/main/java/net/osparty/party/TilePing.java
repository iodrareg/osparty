package net.osparty.party;

import java.awt.Color;
import lombok.Value;
import net.runelite.api.coords.WorldPoint;

/** An active map ping the overlay is animating, with when it was received. */
@Value
public class TilePing
{
	WorldPoint point;
	String name;
	Color color;
	long createdAt;
}
