/**
 * 
 */
package Server;

/**
 * @author harish
 *
 */
public class Treasure extends GameEntity {
	
	public Treasure(String treasureId, GridLocation l){
		id = treasureId; 
		position = l;
	}
	
	/**
	 * This method handles how a treasure is removed from the 
	 * game board when it is collected by a player. 
	 * Setting removed boolean to true for now.
	 */
	@Override
	public void remove() {	
		removed = true;
	}

}
