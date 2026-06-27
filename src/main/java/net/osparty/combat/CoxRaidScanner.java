package net.osparty.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.InstanceTemplates;
import net.runelite.api.NullObjectID;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.Varbits;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.raids.Raid;
import net.runelite.client.plugins.raids.RaidRoom;
import net.runelite.client.plugins.raids.RoomType;
import net.runelite.client.plugins.raids.solver.Layout;
import net.runelite.client.plugins.raids.solver.LayoutSolver;
import net.runelite.client.plugins.raids.solver.Room;

/**
 * Resolves the full Chambers of Xeric room layout while in a raid.
 *
 * <p>A single scene scan only sees the rooms loaded around the player, so we
 * can't read the whole raid directly. Instead — exactly like RuneLite's core
 * Raids plugin — we scan what's visible into a {@link Raid}, build a partial
 * room-type code, and match it against a database of known layouts
 * ({@link LayoutSolver}) to recover every room's position. The combat-room
 * rotation is then solved from the known rooms via the four fixed CoX rotations.
 * Rooms not yet identified read as "Unknown (combat/puzzle)".
 *
 * <p>The scan/solve logic is adapted from RuneLite's Raids plugin (BSD-2). The
 * package-private solver steps are reimplemented here against the public API
 * because an external plugin can't call them directly (different classloader).
 */
@Singleton
public class CoxRaidScanner
{
	private static final int LOBBY_PLANE = 3;
	private static final int SECOND_FLOOR_PLANE = 2;
	private static final int ROOMS_PER_PLANE = 8;
	private static final int ROOMS_PER_X = 4;
	private static final int ROOM_MAX_SIZE = 32;
	private static final int SCENE_SIZE = Constants.SCENE_SIZE;

	/** The four fixed CoX combat-room rotations, used to fill unscouted combat rooms. */
	private static final List<List<RaidRoom>> ROTATIONS = Arrays.asList(
		Arrays.asList(RaidRoom.TEKTON, RaidRoom.VASA, RaidRoom.GUARDIANS, RaidRoom.MYSTICS,
			RaidRoom.SHAMANS, RaidRoom.MUTTADILES, RaidRoom.VANGUARDS, RaidRoom.VESPULA),
		Arrays.asList(RaidRoom.TEKTON, RaidRoom.MUTTADILES, RaidRoom.GUARDIANS, RaidRoom.VESPULA,
			RaidRoom.SHAMANS, RaidRoom.VASA, RaidRoom.VANGUARDS, RaidRoom.MYSTICS),
		Arrays.asList(RaidRoom.VESPULA, RaidRoom.VANGUARDS, RaidRoom.MUTTADILES, RaidRoom.SHAMANS,
			RaidRoom.MYSTICS, RaidRoom.GUARDIANS, RaidRoom.VASA, RaidRoom.TEKTON),
		Arrays.asList(RaidRoom.MYSTICS, RaidRoom.VANGUARDS, RaidRoom.VASA, RaidRoom.SHAMANS,
			RaidRoom.VESPULA, RaidRoom.GUARDIANS, RaidRoom.MUTTADILES, RaidRoom.TEKTON));

	private final Client client;
	private final LayoutSolver layoutSolver = new LayoutSolver();

	private Raid raid;
	private Layout solvedLayout;
	private String cachedLayout;

	@Inject
	private CoxRaidScanner(Client client)
	{
		this.client = client;
	}

	/** Scan the scene and (re)solve the layout; resets when not in a raid. Client thread. */
	public void update()
	{
		if (client.getVarbitValue(Varbits.IN_RAID) != 1 || client.getGameState() != GameState.LOGGED_IN)
		{
			reset();
			return;
		}
		if (client.getScene() == null)
		{
			return;
		}

		raid = buildRaid(raid);
		if (raid == null)
		{
			return; // lobby not located yet
		}

		if (solvedLayout == null)
		{
			Layout layout = layoutSolver.findLayout(raid.toCode());
			if (layout == null)
			{
				return; // not enough scanned to match a layout yet - keep accumulating
			}
			solvedLayout = layout;
			fillUnsolvedRooms(raid, layout);
		}

		// Solve the combat rotation from the rooms we know each pass (more get known
		// as the player explores), then render the ordered rotation.
		RaidRoom[] combat = combatRooms(raid, solvedLayout);
		solveRotation(combat);
		setCombatRooms(raid, solvedLayout, combat);
		cachedLayout = orderedRooms(raid, solvedLayout);
	}

	/** @return the solved raid rotation (combat + puzzle rooms in order), or null. */
	public String layout()
	{
		return cachedLayout;
	}

	private void reset()
	{
		raid = null;
		solvedLayout = null;
		cachedLayout = null;
	}

	// ---- scan (adapted from RaidsPlugin#buildRaid) ---------------------------

	private Raid buildRaid(Raid from)
	{
		Raid result = from;
		if (result == null)
		{
			Point gridBase = findLobbyBase();
			if (gridBase == null)
			{
				return null;
			}
			Integer lobbyIndex = findLobbyIndex(gridBase);
			if (lobbyIndex == null)
			{
				return null;
			}
			result = new Raid(new WorldPoint(client.getBaseX() + gridBase.getX(),
				client.getBaseY() + gridBase.getY(), LOBBY_PLANE), lobbyIndex);
		}

		int baseX = result.getLobbyIndex() % ROOMS_PER_X;
		int baseY = result.getLobbyIndex() % ROOMS_PER_PLANE > (ROOMS_PER_X - 1) ? 1 : 0;
		Tile[][][] tiles = client.getScene().getTiles();

		for (int i = 0; i < result.getRooms().length; i++)
		{
			int x = i % ROOMS_PER_X;
			int y = i % ROOMS_PER_PLANE > (ROOMS_PER_X - 1) ? 1 : 0;
			int plane = i > (ROOMS_PER_PLANE - 1) ? SECOND_FLOOR_PLANE : LOBBY_PLANE;

			x = result.getGridBase().getX() + (x - baseX) * ROOM_MAX_SIZE - client.getBaseX();
			y = result.getGridBase().getY() - (y - baseY) * ROOM_MAX_SIZE - client.getBaseY();

			if (x < (1 - ROOM_MAX_SIZE) || x >= SCENE_SIZE)
			{
				continue;
			}
			x = Math.max(1, x);
			y = Math.max(1, y);
			if (y >= SCENE_SIZE)
			{
				continue;
			}

			Tile tile = tiles[plane][x][y];
			if (tile == null)
			{
				continue;
			}
			result.setRoom(determineRoom(tile), i);
		}
		return result;
	}

	private Point findLobbyBase()
	{
		Tile[][] tiles = client.getScene().getTiles()[LOBBY_PLANE];
		for (int x = 0; x < SCENE_SIZE; x++)
		{
			for (int y = 0; y < SCENE_SIZE; y++)
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

	private Integer findLobbyIndex(Point gridBase)
	{
		if (SCENE_SIZE <= gridBase.getX() + ROOM_MAX_SIZE || SCENE_SIZE <= gridBase.getY() + ROOM_MAX_SIZE)
		{
			return null;
		}
		Tile[][] tiles = client.getScene().getTiles()[LOBBY_PLANE];
		int y = tiles[gridBase.getX()][gridBase.getY() + ROOM_MAX_SIZE] == null ? 0 : 1;
		int x;
		if (tiles[gridBase.getX() + ROOM_MAX_SIZE][gridBase.getY()] == null)
		{
			x = 3;
		}
		else
		{
			for (x = 0; x < 3; x++)
			{
				int sceneX = gridBase.getX() - 1 - ROOM_MAX_SIZE * x;
				if (sceneX < 0 || tiles[sceneX][gridBase.getY()] == null)
				{
					break;
				}
			}
		}
		return x + y * ROOMS_PER_X;
	}

	private RaidRoom determineRoom(Tile base)
	{
		int chunk = client.getInstanceTemplateChunks()[base.getPlane()]
			[base.getSceneLocation().getX() / 8][base.getSceneLocation().getY() / 8];
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

	// ---- solve (reimplemented from Raid / RotationSolver) --------------------

	/** Fill every layout position we haven't scanned with its unsolved placeholder. */
	private void fillUnsolvedRooms(Raid raid, Layout layout)
	{
		for (int i = 0; i < raid.getRooms().length; i++)
		{
			Room room = layout.getRoomAt(i);
			if (room != null && raid.getRoom(i) == null)
			{
				raid.setRoom(unsolvedRoom(room.getSymbol()), i);
			}
		}
	}

	private RaidRoom[] combatRooms(Raid raid, Layout layout)
	{
		List<RaidRoom> combat = new ArrayList<>();
		for (Room room : layout.getRooms())
		{
			RaidRoom rr = raid.getRoom(room.getPosition());
			if (rr != null && rr.getType() == RoomType.COMBAT)
			{
				combat.add(rr);
			}
		}
		return combat.toArray(new RaidRoom[0]);
	}

	private void setCombatRooms(Raid raid, Layout layout, RaidRoom[] combat)
	{
		int index = 0;
		for (Room room : layout.getRooms())
		{
			RaidRoom rr = raid.getRoom(room.getPosition());
			if (rr != null && rr.getType() == RoomType.COMBAT && index < combat.length)
			{
				raid.setRoom(combat[index++], room.getPosition());
			}
		}
	}

	private String orderedRooms(Raid raid, Layout layout)
	{
		StringBuilder sb = new StringBuilder();
		for (Room room : layout.getRooms())
		{
			RaidRoom rr = raid.getRoom(room.getPosition());
			if (rr == null)
			{
				continue;
			}
			if (rr.getType() == RoomType.COMBAT || rr.getType() == RoomType.PUZZLE)
			{
				sb.append(rr.getName()).append(", ");
			}
		}
		return sb.length() < 2 ? null : sb.substring(0, sb.length() - 2);
	}

	private static RaidRoom unsolvedRoom(char symbol)
	{
		switch (symbol)
		{
			case '#':
				return RaidRoom.START;
			case '¤':
				return RaidRoom.END;
			case 'S':
				return RaidRoom.SCAVENGERS;
			case 'F':
				return RaidRoom.FARMING;
			case 'C':
				return RaidRoom.UNKNOWN_COMBAT;
			case 'P':
				return RaidRoom.UNKNOWN_PUZZLE;
			default:
				return RaidRoom.EMPTY;
		}
	}

	/** Fill unknown combat rooms by matching the known ones against the four rotations. */
	private static void solveRotation(RaidRoom[] rooms)
	{
		if (rooms == null)
		{
			return;
		}
		Integer start = null;
		int known = 0;
		for (int i = 0; i < rooms.length; i++)
		{
			if (rooms[i] == null || rooms[i].getType() != RoomType.COMBAT || rooms[i] == RaidRoom.UNKNOWN_COMBAT)
			{
				continue;
			}
			if (start == null)
			{
				start = i;
			}
			known++;
		}
		if (known < 2 || known == rooms.length)
		{
			return;
		}

		List<RaidRoom> match = null;
		Integer index = null;
		for (List<RaidRoom> rotation : ROTATIONS)
		{
			compare:
			for (int i = 0; i < rotation.size(); i++)
			{
				if (rooms[start] == rotation.get(i))
				{
					for (int j = start + 1; j < rooms.length; j++)
					{
						if (rooms[j].getType() != RoomType.COMBAT || rooms[j] == RaidRoom.UNKNOWN_COMBAT)
						{
							continue;
						}
						if (rooms[j] != rotation.get(Math.floorMod(i + j - start, rotation.size())))
						{
							break compare;
						}
					}
					if (match != null && match != rotation)
					{
						return; // ambiguous
					}
					index = i - start;
					match = rotation;
				}
			}
		}
		if (match == null)
		{
			return;
		}
		for (int i = 0; i < rooms.length; i++)
		{
			if (rooms[i] == null)
			{
				continue;
			}
			if (rooms[i].getType() != RoomType.COMBAT || rooms[i] == RaidRoom.UNKNOWN_COMBAT)
			{
				rooms[i] = match.get(Math.floorMod(index + i, match.size()));
			}
		}
	}
}
