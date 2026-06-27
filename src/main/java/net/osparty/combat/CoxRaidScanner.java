package net.osparty.combat;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.InstanceTemplates;
import net.runelite.api.NullObjectID;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.Varbits;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.raids.RaidRoom;

/**
 * Tracks the Chambers of Xeric room layout while the player is in a raid.
 *
 * <p>A CoX raid is larger than the 104×104 scene, so a single scan only sees the
 * rooms currently loaded around the player. This scanner keeps a stable
 * world-coordinate grid (anchored on the lobby) and <em>accumulates</em> rooms
 * across scans as the player explores, so the layout fills in over time instead
 * of flickering to whatever is on screen right now. Rooms that are present but
 * not yet identified are reported as "Unknown". Call {@link #update()} each game
 * tick on the client thread, then read {@link #layout()}.
 *
 * <p>Scanning logic adapted from RuneLite's core Raids plugin (BSD-2).
 */
@Singleton
public class CoxRaidScanner
{
	private static final int ROOM_MAX_SIZE = 32;
	private static final int LOBBY_PLANE = 3;
	private static final int SECOND_FLOOR_PLANE = 2;
	private static final int ROOMS_PER_PLANE = 8;
	private static final int ROOMS_PER_X = 4;
	private static final int ROOM_COUNT = 16;

	/** Rooms not worth listing in the advertised rotation. */
	private static final Set<RaidRoom> SKIP = EnumSet.of(
		RaidRoom.START, RaidRoom.END, RaidRoom.SCAVENGERS, RaidRoom.FARMING);

	private final Client client;

	/** Stable lobby anchor in world coordinates; null until found. */
	private WorldPoint gridBase;
	private int lobbyIndex;
	/** Accumulated rooms by grid index; null = not yet scanned, EMPTY = present but unknown. */
	private RaidRoom[] rooms = new RaidRoom[ROOM_COUNT];

	@Inject
	private CoxRaidScanner(Client client)
	{
		this.client = client;
	}

	/** Scan the loaded scene, accumulating rooms; resets when not in a raid. Client thread. */
	public void update()
	{
		if (client.getVarbitValue(Varbits.IN_RAID) != 1)
		{
			reset();
			return;
		}
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || wv.getScene() == null)
		{
			return;
		}
		Tile[][][] tiles = wv.getScene().getTiles();

		if (gridBase == null)
		{
			Point base = findLobbyBase(wv);
			if (base == null)
			{
				return;
			}
			Integer index = findLobbyIndex(wv, base);
			if (index == null)
			{
				return;
			}
			gridBase = new WorldPoint(wv.getBaseX() + base.getX(), wv.getBaseY() + base.getY(), LOBBY_PLANE);
			lobbyIndex = index;
		}

		int baseX = lobbyIndex % ROOMS_PER_X;
		int baseY = lobbyIndex % ROOMS_PER_PLANE > (ROOMS_PER_X - 1) ? 1 : 0;

		for (int i = 0; i < ROOM_COUNT; i++)
		{
			int gx = i % ROOMS_PER_X;
			int gy = i % ROOMS_PER_PLANE > (ROOMS_PER_X - 1) ? 1 : 0;
			int plane = i > (ROOMS_PER_PLANE - 1) ? SECOND_FLOOR_PLANE : LOBBY_PLANE;

			int x = gridBase.getX() + (gx - baseX) * ROOM_MAX_SIZE - wv.getBaseX();
			int y = gridBase.getY() - (gy - baseY) * ROOM_MAX_SIZE - wv.getBaseY();

			if (x < (1 - ROOM_MAX_SIZE) || x >= Constants.SCENE_SIZE || y >= Constants.SCENE_SIZE)
			{
				continue; // room not in the currently-loaded scene
			}
			x = Math.max(1, x);
			y = Math.max(1, y);

			if (tiles[plane][x][y] == null)
			{
				continue; // nothing loaded here this scan
			}
			RaidRoom room = determineRoom(wv, plane, x, y);
			// Keep the best knowledge: don't downgrade an identified room back to EMPTY.
			if (rooms[i] == null || room != RaidRoom.EMPTY)
			{
				rooms[i] = room;
			}
		}
	}

	/**
	 * @return the accumulated rotation (combat/puzzle rooms in grid order, "Unknown"
	 * for present-but-unidentified rooms), or null when not in a raid / nothing yet.
	 */
	public String layout()
	{
		if (gridBase == null)
		{
			return null;
		}
		List<String> names = new ArrayList<>();
		for (RaidRoom room : rooms)
		{
			if (room == null || SKIP.contains(room))
			{
				continue; // not scanned, or a non-rotation room
			}
			names.add(room == RaidRoom.EMPTY ? "Unknown" : room.getName());
		}
		return names.isEmpty() ? null : String.join(", ", names);
	}

	private void reset()
	{
		gridBase = null;
		lobbyIndex = 0;
		rooms = new RaidRoom[ROOM_COUNT];
	}

	private static Point findLobbyBase(WorldView wv)
	{
		Tile[][] tiles = wv.getScene().getTiles()[LOBBY_PLANE];
		for (int x = 0; x < Constants.SCENE_SIZE; x++)
		{
			for (int y = 0; y < Constants.SCENE_SIZE; y++)
			{
				if (tiles[x][y] == null || tiles[x][y].getWallObject() == null)
				{
					continue;
				}
				if (tiles[x][y].getWallObject().getId() == NullObjectID.NULL_12231)
				{
					return tiles[x][y].getSceneLocation();
				}
			}
		}
		return null;
	}

	private static Integer findLobbyIndex(WorldView wv, Point base)
	{
		if (Constants.SCENE_SIZE <= base.getX() + ROOM_MAX_SIZE
			|| Constants.SCENE_SIZE <= base.getY() + ROOM_MAX_SIZE)
		{
			return null;
		}
		Tile[][] tiles = wv.getScene().getTiles()[LOBBY_PLANE];
		int y = tiles[base.getX()][base.getY() + ROOM_MAX_SIZE] == null ? 0 : 1;
		int x;
		if (tiles[base.getX() + ROOM_MAX_SIZE][base.getY()] == null)
		{
			x = 3;
		}
		else
		{
			for (x = 0; x < 3; x++)
			{
				int sceneX = base.getX() - 1 - ROOM_MAX_SIZE * x;
				if (sceneX < 0 || tiles[sceneX][base.getY()] == null)
				{
					break;
				}
			}
		}
		return x + y * ROOMS_PER_X;
	}

	private RaidRoom determineRoom(WorldView wv, int plane, int x, int y)
	{
		int[][][] chunks = wv.getInstanceTemplateChunks();
		if (chunks == null)
		{
			return RaidRoom.EMPTY;
		}
		int chunk = chunks[plane][x / 8][y / 8];
		InstanceTemplates template = InstanceTemplates.findMatch(chunk);
		if (template == null)
		{
			return RaidRoom.EMPTY;
		}
		switch (template)
		{
			case RAIDS_LOBBY:
			case RAIDS_START:
				return RaidRoom.START;
			case RAIDS_END:
				return RaidRoom.END;
			case RAIDS_SCAVENGERS:
			case RAIDS_SCAVENGERS2:
				return RaidRoom.SCAVENGERS;
			case RAIDS_SHAMANS:
				return RaidRoom.SHAMANS;
			case RAIDS_VASA:
				return RaidRoom.VASA;
			case RAIDS_VANGUARDS:
				return RaidRoom.VANGUARDS;
			case RAIDS_ICE_DEMON:
				return RaidRoom.ICE_DEMON;
			case RAIDS_THIEVING:
				return RaidRoom.THIEVING;
			case RAIDS_FARMING:
			case RAIDS_FARMING2:
				return RaidRoom.FARMING;
			case RAIDS_MUTTADILES:
				return RaidRoom.MUTTADILES;
			case RAIDS_MYSTICS:
				return RaidRoom.MYSTICS;
			case RAIDS_TEKTON:
				return RaidRoom.TEKTON;
			case RAIDS_TIGHTROPE:
				return RaidRoom.TIGHTROPE;
			case RAIDS_GUARDIANS:
				return RaidRoom.GUARDIANS;
			case RAIDS_CRABS:
				return RaidRoom.CRABS;
			case RAIDS_VESPULA:
				return RaidRoom.VESPULA;
			default:
				return RaidRoom.EMPTY;
		}
	}
}
