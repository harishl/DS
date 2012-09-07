/**
 * 
 */

/**
 * @author harish
 *
 */
public class Location {
	private int x,y;

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Location(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public Location(){
		x=0;
		y=0;
	}
	
	
	
}
