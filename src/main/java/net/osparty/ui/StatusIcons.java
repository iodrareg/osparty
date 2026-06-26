package net.osparty.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/**
 * Small status glyphs drawn at load time: a green check and a red cross used to
 * show whether a party member is in the host's friends chat. Drawn rather than
 * shipped as assets so they stay crisp at this size and need no network fetch;
 * swap in wiki PNGs here if preferred.
 */
final class StatusIcons
{
	static final ImageIcon CHECK = new ImageIcon(check());
	static final ImageIcon CROSS = new ImageIcon(cross());

	private static final int SIZE = 14;

	private StatusIcons()
	{
	}

	private static BufferedImage check()
	{
		BufferedImage img = base();
		Graphics2D g = img.createGraphics();
		hints(g);
		g.setColor(new Color(0x4C, 0xD1, 0x37));
		g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(3, 8, 6, 11);
		g.drawLine(6, 11, 11, 3);
		g.dispose();
		return img;
	}

	private static BufferedImage cross()
	{
		BufferedImage img = base();
		Graphics2D g = img.createGraphics();
		hints(g);
		g.setColor(new Color(0xD1, 0x3A, 0x3A));
		g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.drawLine(3, 3, 11, 11);
		g.drawLine(11, 3, 3, 11);
		g.dispose();
		return img;
	}

	private static BufferedImage base()
	{
		return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
	}

	private static void hints(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
	}
}
