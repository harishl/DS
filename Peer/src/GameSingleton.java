
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class GameSingleton implements Serializable {
/*public Selector selector=null;
public Selector playerSelector=null;*/
private static GameSingleton gameInstance=null;
/*public ByteBuffer readBuffer;
public ByteBuffer writeBuffer;*/
public GameEntity[][] grid;
public String primaryPlayerId=null;
public String backupPlayerId=null;
int numTreasures;
int gridSize;
int playercounter;
FileWriter fstream;
BufferedWriter out;
private static final long serialVersionUID = -403250971215465053L;
public List<Player> playerlist;
//public  Map<SocketChannel, Player> players;
public List<Player> writeReadyPlayers;

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
	playercounter=0;

	playerlist=Collections.synchronizedList(new ArrayList<Player>());

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
	if((null!=primaryPlayerId)&&(null==backupPlayerId))
	{
		response+=" backup";
		backupPlayerId="P"+playercounter;
	}
	return response;
}
public void putPlayerOnGame(SocketChannel aPlayer,Peer peer) throws IOException {
	GridLocation l = new GridLocation(gridSize);
	while (!vacant(l)) {
		l.pickAnotherLocation();
	}
	Player p = new Player("P" + playercounter, l,aPlayer);
	grid[l.x][l.y] = p;
	peer.players.put(aPlayer, p);
	playerlist.add(p);
	
	
}


}
