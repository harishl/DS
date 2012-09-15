package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author harish
 * @author sadesh
 */
public class GameServer {

	// Socket Params
	private ServerSocket gameServerSocket = null;
	Dispatcher dispatcher = null;
	public final int timeBeforeStart;
	public final int port;
	private boolean timerStarted;
	private boolean gameStarted;
	
	// Game params
	public final int gridSize;
	public int numTreasures;
	public int playerCounter;

	/**
	 * grid is basically the game state. Game state is maintained in a grid of
	 * GameEntity objects. GameEntity can be a Player or a Treasure by virtue of
	 * inheritance.
	 */
	public GameEntity[][] grid;

	public GameServer(int gridSize, int numTreasures) {

		this.timerStarted = false;
		this.gameStarted = false;
		this.timeBeforeStart = 20000; // 20 * 1000ms;
		this.port = 1234;
		
		this.playerCounter = 0;
		this.gridSize = gridSize;
		this.numTreasures = numTreasures;
		
		this.grid = new GameEntity[gridSize][gridSize];		
		populateTreasures();
	}

	private void populateTreasures() {
		for (int i = 0; i < numTreasures; i++) {
			GridLocation l = new GridLocation(gridSize);
			if (vacant(l)) {
				grid[l.x][l.y] = new Treasures(l);
			} else {
				((Treasures) grid[l.x][l.y]).addTreasure();
			}
		}
	}

	/**
	 * Checks for vacancy of a cell in the grid (game state)
	 * 
	 * @param l
	 *            : location which is to be checked
	 * @return boolean indicating whether l is vacant in the grid or not
	 */
	private boolean vacant(GridLocation l) {
		return (grid[l.x][l.y] == null);
	}

	/**
	 * This method is called by the Player threads to move to the next cell in
	 * any direction. This is synchronized to avoid inconsistent game states due
	 * to parallel updates by two or more Player threads.
	 * 
	 * @param p
	 *            : Player who wants to move
	 * @param nextCell
	 *            : direction in which the Player wants to move
	 * @return boolean indicating whether the move was successful or not.
	 */
	public synchronized boolean move(Player p, Direction nextCell) {
		boolean moved = false;

		if (vacant(p.position.get(nextCell))) {
			grid[p.position.x][p.position.y] = null;

			switch (nextCell) {
			case left:
				p.position.x = p.position.x - 1;
				break;

			case right:
				p.position.x = p.position.x + 1;
				break;

			case up:
				p.position.y = p.position.y + 1;
				break;

			case down:
				p.position.y = p.position.y - 1;
				break;
			}

			grid[p.position.x][p.position.y] = p;
			moved = true;
		}
		return moved;
	}

	/**
	 * Initializes the server and waits for Players to join the game
	 * 
	 * @throws IOException
	 */
	public void initServer() throws IOException {

		gameServerSocket = new ServerSocket(port);
		dispatcher = new Dispatcher();
		Socket aPlayerClient = null;
		
		while (!gameStarted) {
			System.out.println("Waiting for clients to join");

			try	{
				aPlayerClient = gameServerSocket.accept();
			}
			catch (SocketTimeoutException sctTimeOutEx) {
				System.out.println("No more players can join!");
				break;
			}
			System.out.println("Client from " + aPlayerClient.getInetAddress()
					+ " connected.");
			putPlayerOnGame(aPlayerClient);


			if (!timerStarted) {
				// Executes only once
				gameServerSocket.setSoTimeout(timeBeforeStart);
				timerStarted = true;
			}
		}
	}

	private void putPlayerOnGame(Socket aPlayerClient) throws IOException {
		
		GridLocation l = new GridLocation(gridSize);
		while (!vacant(l)) {
			l.pickAnotherLocation();
		} 
		Player p = new Player("P"+ ++playerCounter , l);
		grid[l.x][l.y] = p;
		dispatcher.addPlayer(aPlayerClient, p);
	}

	/**
	 * @param args
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
		aGameServer.initServer();
	}

}
