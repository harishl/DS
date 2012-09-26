package Server;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 */

/**
 * @author harish
 *
 */
public class Player extends GameEntity implements Runnable {

	private GameServer gameServer;
	public List<Character> requestQueue; 
	public List<String> responseQueue;
	public boolean interestToWrite;
	public int numCollectedTreasures;
	public String msgToPlayerClient;
	
	public Player(String playerId, GridLocation l, GameServer s) {
		super(playerId, l);
		gameServer = s;
		numCollectedTreasures = 0;
		requestQueue = Collections.synchronizedList(new LinkedList<Character>());
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
		while (true) {
			synchronized (requestQueue) {
				while (requestQueue.isEmpty()) {
					try {
						requestQueue.wait();
					} catch (InterruptedException e) {
						System.out.println("Interrupted Exception in "
								+ this.id + "while waiting on requestQueue");
					}
				} // end of while (requestQueue.isEmpty())
				
				char moveDirChar = requestQueue.remove(0);
				if(Direction.getDirection(moveDirChar) != Direction.invalid) {
					interestToWrite = move(Direction.getDirection(moveDirChar));
				}
				if(interestToWrite) {
					msgToPlayerClient = "Treasures: " + numCollectedTreasures;
					synchronized (gameServer.writeReadyPlayers) {
						gameServer.writeReadyPlayers.add(this);
					}
				}
			}
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
	public synchronized boolean move(Direction nextCellDir) {
		boolean moved = false;
		GridLocation nextLocation = position.get(nextCellDir);
		if(nextLocation != null) {
			if (gameServer.vacant(nextLocation) 
					|| gameServer.grid[nextLocation.x][nextLocation.y] instanceof Treasures) {
				gameServer.grid[position.x][position.y] = null;
				position.moveTo(nextLocation);
				if (gameServer.grid[position.x][position.y] instanceof Treasures) {
					numCollectedTreasures += ((Treasures)gameServer.grid[position.x][position.y]).treasureCountInLocation;
					gameServer.numTreasures -= ((Treasures)gameServer.grid[position.x][position.y]).treasureCountInLocation;;
				}
				gameServer.grid[position.x][position.y] = this;
				moved = true;
			}
		}
		return moved;
	}
}
