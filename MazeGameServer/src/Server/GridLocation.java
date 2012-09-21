package Server;
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
	
	public GridLocation get(Direction cell){
		switch(cell){
		case left:
			if(this.x - 1 >= 0){
				return new GridLocation(this.x - 1 , this.y);
			}
			break;
			
		case right:
			if(this.x + 1 < gridRowColSize){
				return new GridLocation(this.x + 1 , this.y);
			}
			break;
			
		case up:
			if(this.y + 1 < gridRowColSize){
				return new GridLocation(this.x, this.y + 1);
			}
			
		case down:
			if(this.y - 1 >= 0){
				return new GridLocation(this.x, this.y - 1);
			}
		}
		
		return null;
	}
}
