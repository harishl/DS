package peer;
import java.util.Random;

/**
 * @author harish
 *
 */
public class GridLocation {
	public int x;
	public int y;
	private int gridRowColSize;
	private Random randomNoGenerator;
	
	public GridLocation(int x, int y){
		this.x = x;
		this.y = y;
	}
	public GridLocation(int gridSize){
		randomNoGenerator = new Random();
		gridRowColSize = gridSize;
		x = randomNoGenerator.nextInt(gridRowColSize);
		y = randomNoGenerator.nextInt(gridRowColSize);
	}
	
	public void pickAnotherLocation(){
		x = randomNoGenerator.nextInt(gridRowColSize);
		y = randomNoGenerator.nextInt(gridRowColSize);
	}
	
	public void moveTo(GridLocation l) {
		x = l.x;
		y = l.y;
	}
	
	public GridLocation get(Direction cell){
		switch(cell){
		case up:
			if(this.x - 1 >= 0){
				return new GridLocation(this.x - 1 , this.y);
			}
			break;
			
		case down:
			if(this.x + 1 < gridRowColSize){
				return new GridLocation(this.x + 1 , this.y);
			}
			break;
			
		case right:
			if(this.y + 1 < gridRowColSize){
				return new GridLocation(this.x, this.y + 1);
			}
			break;
			
		case left:
			if(this.y - 1 >= 0){
				return new GridLocation(this.x, this.y - 1);
			}
			break;
			
		case noMove:
			return new GridLocation(this.x, this.y);
			
		case invalid:
			return new GridLocation(this.x, this.y);
		}
		
		return null;
	}
}
