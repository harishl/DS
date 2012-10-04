import java.io.Serializable;



/**
 * @author harish
 */
public abstract class GameEntity implements Serializable {
	//TODO: Re-visit to check if we can have a stricter access modifier.
	public String id;
	public GridLocation position;
	public boolean removed;
	private static final long serialVersionUID = -403250971215465052L;
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
