/**
 * 
 */
package peer;

/**
 * @author harish
 * enumeration of directions in which Player can move
 */
public enum Direction {
	left,
	right,
	up,
	down,
	noMove,
	invalid;
	
	public static Direction getDirection(char c) {
		switch (c) {
		case 'w':
			return Direction.up;
		case 's':
			return Direction.down;
		case 'a':
			return Direction.left;
		case 'd':
			return Direction.right;
		case 'x':
			return Direction.noMove;	
		default: 
			return Direction.invalid;
		}
	}
}
