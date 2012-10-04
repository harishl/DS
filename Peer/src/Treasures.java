/**
 * 
 */


/**
 * @author harish
 *
 */
public class Treasures extends GameEntity{
	
	public int treasureCountInLocation;
	public Treasures(GridLocation l){
		super("treasure", l);
		treasureCountInLocation = 1;
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
	
	public void addTreasure(){
		treasureCountInLocation++;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ((Integer)treasureCountInLocation).toString();
	}

}
