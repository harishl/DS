package peer;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GameSingleton {
public Selector selector=null;
public Selector playerSelector=null;
private static GameSingleton gameInstance=null;
public ByteBuffer readBuffer;
public ByteBuffer writeBuffer;
public GameEntity[][] grid;
int numTreasures;
int gridSize;
int playercounter;

public List<Player> playerRequestQueue;
public  Map<SocketChannel, Player> players;
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
	players=new HashMap<SocketChannel, Player>();
	playerRequestQueue=Collections.synchronizedList(new ArrayList<Player>());
	readBuffer = ByteBuffer.allocate(8192);
	writeBuffer = ByteBuffer.allocate(8192);
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

public void finishGame() throws IOException{
	Player winner = null;
	int maxTreasures = 0;
	for (SocketChannel s : players.keySet()) {
		if (players.get(s).numCollectedTreasures > maxTreasures) {
			winner = players.get(s);
			maxTreasures = winner.numCollectedTreasures;
		}
	}
	
	for (SocketChannel s : players.keySet()) {
		writeBuffer.clear();
		SelectionKey key = s.keyFor(selector);
		key.interestOps(SelectionKey.OP_WRITE);
		String goodByeMsg = "Player " + winner.id + " has won the game. The game has ended.";
		goodByeMsg = prepareResponseMsg(goodByeMsg);
		writeBuffer = ByteBuffer.wrap(goodByeMsg.getBytes());
		s.write(writeBuffer);
		key.cancel();
		s.close();
	}
	selector.close();
	
	//Exit the program
	System.exit(0);
}

public void readDataFromPlayer(SelectionKey key) throws IOException {
	readBuffer.clear();
	SocketChannel scktChannel = (SocketChannel)key.channel();
	int dataSize;
	try{
	dataSize = scktChannel.read(readBuffer);
	} catch (IOException e) {
		key.cancel();
		scktChannel.close();
		return;
	}
	System.out.println(new String(readBuffer.array()));
	if(dataSize == -1) {
		// The player client has shut it's socket down. 
		key.channel().close();
		key.cancel();
		return;
	}
	Player p = players.get(scktChannel);
	
	synchronized (p.requestQueue) {
		p.requestQueue.add((char)readBuffer.array()[0]);
		p.requestQueue.notify();
	}
}

public void writeDataToPlayer(SelectionKey key) throws IOException {
	writeBuffer.clear();
	SocketChannel scktChannel = (SocketChannel) key.channel(); 
	Player playerToWriteTo = players.get(scktChannel);
	String responseMsg = prepareResponseMsg(playerToWriteTo.msgToPlayerClient);
	writeBuffer = ByteBuffer.wrap(responseMsg.getBytes());
	scktChannel.write(writeBuffer);
	key.interestOps(SelectionKey.OP_READ);
}

public void writeWelcomeMsgToPlayer(SocketChannel scktChannel) throws IOException {
	writeBuffer.clear();
	String msg = "You've joined the game. \nYou are Player " 
			+ players.get(scktChannel).id 
			+ "\nPlease wait for start signal.";
	msg = prepareResponseMsg(msg);
	writeBuffer = ByteBuffer.wrap(msg.getBytes());
	scktChannel.register(selector, SelectionKey.OP_WRITE);
	scktChannel.write(writeBuffer);
	SelectionKey selKey = scktChannel.keyFor(selector);
	selKey.interestOps(SelectionKey.OP_READ);
}

public void putPlayerOnGame(SocketChannel aPlayer) throws IOException {
	GridLocation l = new GridLocation(gridSize);
	while (!vacant(l)) {
		l.pickAnotherLocation();
	}
	Player p = new Player("P" + playercounter, l,aPlayer);
	grid[l.x][l.y] = p;
	players.put(aPlayer, p);
	
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
	return response;
}

public SocketChannel getscktChannel(Player aWriteReadyPlayer) {
	for(SocketChannel s : players.keySet()) {
		if(players.get(s) == aWriteReadyPlayer) return s;
	}
	return null;
}


}
