package net.osparty.ui;

import net.osparty.OSPartyConfig;
import net.osparty.combat.DefenceTracker;
import net.osparty.combat.DefenceTracker.DefenceState;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Draws the live defence level of the monster the party is draining, next to its
 * overhead health bar. The value comes from {@link DefenceTracker}; the overlay
 * just positions and colours it.
 */
public class NpcDefenceOverlay extends Overlay
{
	private final Client client;
	private final DefenceTracker tracker;
	private final OSPartyConfig config;

	public NpcDefenceOverlay(Client client, DefenceTracker tracker, OSPartyConfig config)
	{
		this.client = client;
		this.tracker = tracker;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.defenceHpBar())
		{
			return null;
		}
		DefenceState state = tracker.state();
		if (state == null)
		{
			return null;
		}
		NPC npc = npcByIndex(state.getNpcIndex());
		if (npc == null)
		{
			return null;
		}

		long shown = config.defenceShowFullLevel() ? state.getCurrent() : state.getCurrent() - state.getMin();
		String text = "Def: " + Math.max(0, shown);
		Color color = colorFor(state);

		// Sit just above the health bar (which renders near the model's logical
		// height) and nudge to the right so it doesn't overlap the bar.
		Point base = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + 40);
		if (base == null)
		{
			return null;
		}
		Point at = new Point(base.getX() + 30, base.getY());
		OverlayUtil.renderTextLocation(graphics, at, text, color);
		return null;
	}

	private Color colorFor(DefenceState state)
	{
		long relative = Math.max(state.getCurrent() - state.getMin(), 0);
		if (relative == 0)
		{
			return config.defenceCappedColor();
		}
		if (relative <= config.defenceLowThreshold())
		{
			return config.defenceLowColor();
		}
		return config.defenceHighColor();
	}

	private NPC npcByIndex(int index)
	{
		for (NPC npc : client.getNpcs())
		{
			if (npc != null && npc.getIndex() == index)
			{
				return npc;
			}
		}
		return null;
	}
}
