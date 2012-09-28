

import java.net.Socket;
import java.util.List;

/**
 * 
 */

/**
 * @author harish
 *
 */
public class Player extends GameEntity implements Runnable {

	//private GameServer gameServer;
	public List<Character> requestQueue; 
	public List<String> responseQueue;
	public boolean interestToWrite;
	public int numCollectedTreasures;
	public String msgToPlayerClient;
	public Socket ps;
	public IOOperations io;
	GameSingleton gs;
	public Player(String playerId, GridLocation l,Socket s) {
		super(playerId, l);
		gs=GameSingleton.getInstance();
		ps=s;
		io=new IOOperations(ps);
		io.initWrite();
		io.initRead();
		//gameServer = s;
		numCollectedTreasures = 0;
		//requestQueue = Collections.synchronizedList(new LinkedList<Character>());
	}
	public Player(String playerId, GridLocation l) {
		super(playerId, l);
		gs=GameSingleton.getInstance();
		
		io=new IOOperations();
		//gameServer = s;
		numCollectedTreasures = 0;
		//requestQueue = Collections.synchronizedList(new LinkedList<Character>());
	}
	
	/**
	 * This method handles how a player is removed from the 
	 * game board when the corresponding client crashes. 
	 * Setting removed boolean to true for now.
	 */
	@Override
	public void remove() {	
		removed = true;
	}

	@Override
	public void run() {
		io.streamWrite("Game Started");
		while (true) {
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Client "+this.id+" "+io.socket.getPort());
			System.out.println("Client "+this.id+" "+ps.getPort());
			System.out.println("Player "+this.id+(String)io.streamRead());
		}
	}
	
	/**
	 * Move to the next cell in given direction. This is synchronized to avoid inconsistent game states due
	 * to parallel updates by two or more Player threads.
	 * 
	 * @param nextCellDir
	 *            : direction in which the Player wants to move
	 * @return boolean indicating whether the move was successful or not.
	 */
/*	public synchronized boolean move(Direction nextCellDir) {
		boolean moved = false;
		GridLocation nextLocation = position.get(nextCellDir);
		if(nextLocation != null) {
			if (gameServer.vacant(nextLocation) 
					|| gameServer.grid[nextLocation.x][nextLocation.y] instanceof Treasures) {
				gameServer.grid[position.x][position.y] = null;
				position = nextLocation;
				if (gameServer.grid[position.x][position.y] instanceof Treasures) {
					numCollectedTreasures += ((Treasures)gameServer.grid[position.x][position.y]).treasureCountInLocation;
				}
				gameServer.grid[position.x][position.y] = this;
				moved = true;
			}
		}
		return moved;
	}*/
}
