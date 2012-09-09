package Server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * @author harish
 * @author sadesh
 */
public class GameServer extends TimerTask{

	//Main loop params
	public final long timeBeforeStart = 20000; //20 * 1000ms
	public final int port = 1234;
	public int playerCounter;
	private boolean timerStarted ;
	private boolean gameStarted; 
	
	/**
	 * grid is basically the game state.
	 * Game state is maintained in a grid of GameEntity objects. 
	 * GameEntity can be a Player or a Treasure by virtue of inheritance. 
	 */
	public GameEntity[][] grid;
	

	
	public GameServer(int gridSize, int numTreasures){
		playerCounter = 0;
		timerStarted = false;
		gameStarted = false; 
		
		grid =  new GameEntity[gridSize][gridSize];
		populateTreasures(gridSize, numTreasures);
	}
	
	private void populateTreasures(int gridSize, int numTreasures){
		for(int i = 0; i < numTreasures; i++){
			GridLocation l = new GridLocation(gridSize);
			while(!vacant(l)){
				l.pickAnotherLocation();
			}
			
			grid[l.x][l.y] = new Treasure("T"+i, l);
		}
	}
	
	/**
	 * Checks for vacancy of a cell in the grid (game state)
	 * @param l : location which is to be checked
	 * @return boolean indicating whether l is vacant in the grid or not
	 */
	private boolean vacant(GridLocation l){
		return (grid[l.x][l.y] == null);
	}
	
	/**
	 * This method is called by the Player threads to move to the next cell 
	 * in any direction. This is synchronized to avoid inconsistent game states 
	 * due to parallel updates by two or more Player threads.
	 * @param p : Player who wants to move
	 * @param nextCell : direction in which the Player wants to move
	 * @return boolean indicating whether the move was successful or not.
	 */
	public synchronized boolean move(Player p, Direction nextCell){
		boolean moved = false;

		if(vacant(p.position.get(nextCell))){
			grid[p.position.x][p.position.y] = null;
			
			switch(nextCell){
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
	public void initServer() throws IOException{
		int port = 1234; //some port number

		ServerSocket gameServerSocket = new ServerSocket(port);
		
		Dispatcher dispatcher = new Dispatcher();
		
		Timer gameStartTimer = new Timer();
		while(!gameStarted){
			System.out.println("Players can join now");
			Socket aPlayerClient = gameServerSocket.accept(); // blocks till a client requests for connection
			System.out.println("Client from " + aPlayerClient.getInetAddress()+ " connected.");
			dispatcher.players.put(aPlayerClient, new Player("P" + ++playerCounter));
			
			if(!timerStarted){
				//Executes only once
				gameStartTimer.schedule(this, timeBeforeStart);
				timerStarted = true;
			}
		}
	}
	
	/**
	 * run() method of TimerTask
	 */
	@Override
	public void run() {
		gameStarted = true;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		int gridSize = 0, numTreasures = 0;
		
		//input validations
		if(args.length <= 0){
			System.out.println("Missing required command-line arguments");
			System.out.println("- Size N for N*N grid");
			System.out.println("- Number of treasures");
			System.exit(0); //exit
		}
		try{
			gridSize = Integer.parseInt(args[0]);
			numTreasures = Integer.parseInt(args[1]);
			if(gridSize <= 1 || numTreasures <= 0) throw new IllegalArgumentException();
		}
		catch(IllegalArgumentException e){
			System.out.println("Invalid command-line arguments");
			System.out.println("- Size N for N*N grid must be an integer > 1");
			System.out.println("- Number of treasures must be an integer > 0");
			System.exit(0); //exit
		}
		
		//Here we go!
		GameServer aGameServer = new GameServer(gridSize, numTreasures);
		aGameServer.initServer();
	}

}
