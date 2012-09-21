package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author harish
 * @author sadesh
 */
public class GameServer {

	// Socket Params
	private Selector dispatcher = null;
	private ServerSocketChannel svrScktChnl;
	public final int timeBeforeStart;
	public final int port;
	private boolean timerStarted;
	private Timer timer;
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
		this.timeBeforeStart = 3000; // 20 * 1000ms;
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
		GridLocation nextLocation = p.position.get(nextCell);
		if (vacant(nextLocation)
				|| grid[nextLocation.x][nextLocation.y] instanceof Treasures) {
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
	 * @throws IOException
	 */
	public void initServer() throws IOException {
		dispatcher = Selector.open();
		svrScktChnl = ServerSocketChannel.open();
		svrScktChnl.socket().bind(new InetSocketAddress(port));
		svrScktChnl.configureBlocking(false); // makes server to accept without blocking
		SelectionKey key = svrScktChnl.register(dispatcher, SelectionKey.OP_ACCEPT);
		System.out.println("SelectionKey: " + key.channel().toString());
		SocketChannel aPlayerScktChnl = null;

		while (!gameStarted) {
			System.out.println("Waiting for players to join");

			if(dispatcher.selectNow() == 0) continue;
			Iterator<SelectionKey> i = dispatcher.selectedKeys().iterator();
			while(i.hasNext()){
				SelectionKey selKey = (SelectionKey) i.next();
				i.remove();
				if(!selKey.isValid()) continue;
				
				//has a player attempted to join?
				if(selKey.isAcceptable()) {
					aPlayerScktChnl = svrScktChnl.accept();
					aPlayerScktChnl.configureBlocking(false);
					aPlayerScktChnl.register(dispatcher, SelectionKey.OP_READ|SelectionKey.OP_WRITE);
				}
			}
			

			if (aPlayerScktChnl != null) {
				playerCounter++;
				System.out.println("Players joined: " + playerCounter);
				putPlayerOnGame(aPlayerScktChnl);
			}

			if (!timerStarted && playerCounter == 1 /*first player has joined*/) {
				// Executes only once
				timer.schedule(new LoopBreakerTask(), timeBeforeStart);
				timerStarted = true;
			}
		}
	}
	
	class LoopBreakerTask extends TimerTask {
		@Override
		public void run() {
			gameStarted = true;
			System.out.println("No more players can join!");
		}
	}

	private void putPlayerOnGame(SocketChannel aPlayer) throws IOException {
		GridLocation l = new GridLocation(gridSize);
		while (!vacant(l)) {
			l.pickAnotherLocation();
		}
		Player p = new Player("P" + playerCounter, l);
		grid[l.x][l.y] = p;
	}

	private String gridToString() {
		String gridString = "";
		for (int i = 0; i < gridSize; i++) {
			gridString += "\n";
			for (int j = 0; j < gridSize; j++) {
				if (grid[i][j] == null) {
					gridString += "\tX\t";
				} else {
					gridString += "\t" + (grid[i][j]).toString() + "\t";
				}
			}
		}
		return gridString;
	}

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
		aGameServer.initServer();
	}

}
