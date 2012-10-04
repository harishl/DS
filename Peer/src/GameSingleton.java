import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GameSingleton implements Serializable {
	private static GameSingleton gameInstance = null;
	public GameEntity[][] grid;
	public String primaryPlayerId = null;
	public String backupPlayerId = null;
	int numTreasures;
	int gridSize;
	int playercounter;
	String filename;
	private static final long serialVersionUID = -403250971215465053L;
	public List<Player> playerlist;
	public List<Player> writeReadyPlayers;
	public List<String> crashedPlayersandBackupserver;

	public int getNumTreasures() {
		return numTreasures;
	}

	public void setNumTreasures(int numTreasures) {
		this.numTreasures = numTreasures;
	}

	public int getGridSize() {
		return gridSize;
	}

	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;

	}

	private GameSingleton() {
		playercounter = 0;
		crashedPlayersandBackupserver=new ArrayList<String>();
		playerlist = Collections.synchronizedList(new ArrayList<Player>());

	}

	public static GameSingleton getInstance() {
		if (null == gameInstance) {
			gameInstance = new GameSingleton();

		}
		return gameInstance;
	}

	public void populateTreasures() {
		this.grid = new GameEntity[gridSize][gridSize];
		for (int i = 0; i < numTreasures; i++) {
			GridLocation l = new GridLocation(gridSize);
			if (vacant(l)) {
				grid[l.x][l.y] = new Treasures(l);
			} else {
				((Treasures) grid[l.x][l.y]).addTreasure();
			}
		}
	}

	public synchronized boolean vacant(GridLocation l) {
		return (grid[l.x][l.y] == null);
	}

	public String prepareResponseMsg(String s) {
		String response = "";
		for (int i = 0; i < gridSize; i++) {
			response += "\n";
			for (int j = 0; j < gridSize; j++) {
				if (grid[i][j] == null) {
					response += "\tX";
				} else {
					response += "\t" + (grid[i][j]).toString();
				}
			}
		}
		response += "\n\n\n" + s.toUpperCase() + "\n";
		if ((null != primaryPlayerId) && (null == backupPlayerId)) {
			response += " backup";
			backupPlayerId = "P" + playercounter;
		}
		return response;
	}

	public void putPlayerOnGame(SocketChannel aPlayer, Peer peer)
			throws IOException {
		GridLocation l = new GridLocation(gridSize);
		while (!vacant(l)) {
			l.pickAnotherLocation();
		}
		Player p = new Player("P" + playercounter, l, aPlayer);
		grid[l.x][l.y] = p;
		peer.players.put(aPlayer, p);
		playerlist.add(p);
	}

	public String getTime() {
		String time;
		Date date = new Date();

		time = "Time: " + Integer.toString(date.getHours()) + ":"
				+ Integer.toString(date.getMinutes()) + ":"
				+ Integer.toString(date.getSeconds());
		return time;
	}

}
