package Server;
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
	public abstract void remove();
}
