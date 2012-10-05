

import java.nio.channels.SocketChannel;
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

	private GameSingleton gs;
	public List<Character> requestQueue; 
	public List<String> responseQueue;
	public boolean interestToWrite;
	public int numCollectedTreasures;
	public String msgToPlayerClient;	

	public Player(String playerId, GridLocation l,SocketChannel sct) {
		super(playerId, l);
		gs=GameSingleton.getInstance();
		numCollectedTreasures = 0;
		requestQueue = Collections.synchronizedList(new LinkedList<Character>());
	}
	public Player(String playerId, GridLocation l,SocketChannel sct,int nooftressures) {
		super(playerId, l);
		gs=GameSingleton.getInstance();
		numCollectedTreasures = nooftressures;
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
					synchronized (gs.writeReadyPlayers) {
						gs.writeReadyPlayers.add(this);
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
			if (gs.vacant(nextLocation) 
					|| gs.grid[nextLocation.x][nextLocation.y] instanceof Treasures) {
				gs.grid[position.x][position.y] = null;
				position.moveTo(nextLocation);
				if (gs.grid[position.x][position.y] instanceof Treasures) {
					numCollectedTreasures += ((Treasures)gs.grid[position.x][position.y]).treasureCountInLocation;
					gs.numTreasures -= ((Treasures)gs.grid[position.x][position.y]).treasureCountInLocation;;
				}
				gs.grid[position.x][position.y] = this;
				moved = true;
			}
			else if (nextCellDir == Direction.noMove) {
				// we must give an update to the client if noMove was requested
				moved = true;
			}
		}
		return moved;
	}
}
