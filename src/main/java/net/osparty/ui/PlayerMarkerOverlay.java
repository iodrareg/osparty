package net.osparty.ui;

import net.osparty.OSPartyConfig;
import net.osparty.party.LiveParty;
import net.osparty.party.LiveParty.Marker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Marks party members tagged as a learner or teacher in the game scene: an icon
 * above their head and a coloured highlight on their tile. Untagged members get
 * nothing. Driven by {@link LiveParty#learnerMarkers()} and gated by config.
 */
public class PlayerMarkerOverlay extends Overlay
{
	private static final Color TEACHER_COLOR = new Color(255, 175, 45);
	private static final Color LEARNER_COLOR = new Color(80, 200, 255);

	private final Client client;
	private final LiveParty liveParty;
	private final OSPartyConfig config;
	private final BufferedImage learnerIcon;
	private final BufferedImage teacherIcon;

	public PlayerMarkerOverlay(Client client, LiveParty liveParty, OSPartyConfig config,
		BufferedImage learnerIcon, BufferedImage teacherIcon)
	{
		this.client = client;
		this.liveParty = liveParty;
		this.config = config;
		this.learnerIcon = learnerIcon;
		this.teacherIcon = teacherIcon;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.learnerTeacherMarkers())
		{
			return null;
		}
		Map<String, Marker> markers = liveParty.learnerMarkers();
		if (markers.isEmpty())
		{
			return null;
		}

		for (Player player : client.getPlayers())
		{
			if (player == null || player.getName() == null)
			{
				continue;
			}
			Marker marker = markers.get(LiveParty.normalizeName(player.getName()));
			if (marker == null || marker == Marker.NONE)
			{
				continue;
			}
			Color color = marker == Marker.TEACHER ? TEACHER_COLOR : LEARNER_COLOR;
			BufferedImage icon = marker == Marker.TEACHER ? teacherIcon : learnerIcon;
			drawMarker(graphics, player, color, icon);
		}
		return null;
	}

	private void drawMarker(Graphics2D graphics, Player player, Color color, BufferedImage icon)
	{
		LocalPoint lp = player.getLocalLocation();
		if (lp == null)
		{
			return;
		}

		Polygon tile = Perspective.getCanvasTilePoly(client, lp);
		if (tile != null)
		{
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
			graphics.fill(tile);
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
			graphics.draw(tile);
		}

		if (icon != null)
		{
			Point loc = player.getCanvasImageLocation(icon, player.getLogicalHeight() + 20);
			if (loc != null)
			{
				graphics.drawImage(icon, loc.getX(), loc.getY(), null);
			}
		}
	}
}
