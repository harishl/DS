package peer;
/**
 * Abstract base class which for objects (Treasures and Players) in the game grid. 
 */

/**
 * @author harish
 */
public abstract class GameEntity {
	//TODO: Re-visit to check if we can have a stricter access modifier.
	public String id;
	public GridLocation position;
	public boolean removed;
	
	public GameEntity(String id, GridLocation l){
		this.id = id;
		this.position = l;
		removed = false;
	}
	
	/* 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return id;
	}

	public abstract void remove();
}
