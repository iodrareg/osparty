package net.osparty.ui;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;
import net.runelite.http.api.worlds.WorldRegion;

/**
 * Small country-flag icons for a world's {@link WorldRegion}, used to show where
 * a party's host is playing. Icons are bundled with the plugin and loaded lazily.
 */
final class WorldFlags
{
	private static final int SIZE = 16;
	private static final Map<WorldRegion, ImageIcon> CACHE = new EnumMap<>(WorldRegion.class);

	private WorldFlags()
	{
	}

	/** @return the flag for {@code region}, or null if unknown / not loadable. */
	static ImageIcon forRegion(WorldRegion region)
	{
		if (region == null)
		{
			return null;
		}
		return CACHE.computeIfAbsent(region, r -> load(resourceFor(r)));
	}

	private static String resourceFor(WorldRegion region)
	{
		switch (region)
		{
			case UNITED_STATES_OF_AMERICA:
				return "flag_us.png";
			case UNITED_KINGDOM:
				return "flag_uk.png";
			case AUSTRALIA:
				return "flag_aus.png";
			case GERMANY:
				return "flag_ger.png";
			case BRAZIL:
				return "flag_br.png";
			case JAPAN:
				return "flag_jp.png";
			case SINGAPORE:
				return "flag_sg.png";
			case SOUTH_AFRICA:
				return "flag_za.png";
			default:
				return null;
		}
	}

	private static ImageIcon load(String file)
	{
		if (file == null)
		{
			return null;
		}
		BufferedImage img = ImageUtil.loadImageResource(WorldFlags.class, "/net/osparty/icons/flags/" + file);
		if (img == null)
		{
			return null;
		}
		return new ImageIcon(ImageUtil.resizeImage(img, SIZE, SIZE));
	}
}
