import java.util.List;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GameSingleton {
private static GameSingleton gameInstance=null;
public GameEntity[][] grid;
int numTreasures;
int gridSize;
int playercounter;
private Map<String,Player> players;
public List<Player> playerRequestQueue;
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
private GameSingleton()
{
	playercounter=1;
	players=new HashMap<String, Player>();
	playerRequestQueue=Collections.synchronizedList(new ArrayList<Player>());
}
public static GameSingleton getInstance()
{
	if(null==gameInstance)
	{
		gameInstance=new GameSingleton();
		
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

public void putPlayerOnGame(Socket playerSocket) throws IOException {
	GridLocation l = new GridLocation(gridSize);
	while (!vacant(l)) {
		l.pickAnotherLocation();
	}
	Player p = new Player("P" + ++playercounter, l,playerSocket);
	p.io.streamWrite("Game joined");
	grid[l.x][l.y] = p;
	players.put("P"+playercounter, p);
}
public void putHostPlayerOnGame() throws IOException {
	GridLocation l = new GridLocation(gridSize);
	while (!vacant(l)) {
		l.pickAnotherLocation();
	}
	Player p = new Player("P" + playercounter, l);
	grid[l.x][l.y] = p;
	players.put("P"+playercounter, p);
}
public void initiateGame() {
	Iterator<String> itr=players.keySet().iterator();
	System.out.println("HI");
	while(itr.hasNext())
	{
		String check=itr.next();
		if(!check.equals("P1"))
		{
			Player pl=players.get(check);
			Thread th=new Thread(pl);
			th.start();
		}
	}
	new ProcessThread().start();
	
}
}
