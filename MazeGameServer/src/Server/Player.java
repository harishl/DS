package Server;
/**
 * 
 */

/**
 * @author harish
 *
 */
public class Player extends GameEntity implements Runnable {


	public Player(String playerId, GridLocation l) {
		super(playerId, l);
		
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
		// TODO Auto-generated method stub
		
	}
}
