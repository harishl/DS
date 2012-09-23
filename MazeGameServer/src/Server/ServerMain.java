/**
 * 
 */
package Server;

import java.io.IOException;

/**
 * @author harish
 *
 */
public class ServerMain {

	/**
	 * @param args
	 *            : accepts two args, viz. - Number of cells in a row/column of
	 *            the grid and number of treasures on the grid
	 */
	public static void main(String[] args) throws IOException {
		int gridSize = 0, numTreasures = 0;

		// input validations
		if (args.length <= 0) {
			System.out.println("Missing required command-line arguments");
			System.out.println("- Size N for N*N grid");
			System.out.println("- Number of treasures");
			System.exit(0); // exit
		}
		try {
			gridSize = Integer.parseInt(args[0]);
			numTreasures = Integer.parseInt(args[1]);
			if (gridSize <= 1 || numTreasures <= 0)
				throw new IllegalArgumentException();
		} catch (IllegalArgumentException e) {
			System.out.println("Invalid command-line arguments");
			System.out.println("- Size N for N*N grid must be an integer > 1");
			System.out.println("- Number of treasures must be an integer > 0");
			System.exit(0); // exit
		}

		// Here we go!
		GameServer aGameServer = new GameServer(gridSize, numTreasures);
		Thread mazeGame = new Thread(aGameServer);
		mazeGame.start();
	}

}
