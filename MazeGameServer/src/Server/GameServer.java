package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author harish
 * @author sadesh
 */
public class GameServer implements Runnable{

	// Socket Params
	private Selector selector = null;
	private ServerSocketChannel svrScktChnl;
	public final int timeBeforeStart;
	public final int port;
	private boolean timerStarted;
	private boolean gameStarted;
	private Timer timer;
	
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	private Map<SocketChannel, Player> players;
	public List<Player> writeReadyPlayers;

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
		timer = new Timer();
		this.timerStarted = false;
		this.gameStarted = false;
		this.timeBeforeStart = 5000; // 20 * 1000ms;
		this.port = 1234;

		this.playerCounter = 0;
		this.gridSize = gridSize;
		this.numTreasures = numTreasures;

		this.grid = new GameEntity[gridSize][gridSize];
		populateTreasures();
	}

	/**
	 * Populate treasures in random locations on the grid
	 */
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
	public synchronized boolean vacant(GridLocation l) {
		return (grid[l.x][l.y] == null);
	}

	/**
	 * Initializes the server and waits for Players to join the game
	 * 
	 * @throws IOException
	 */
	@Override
	public void run() {
		try {
			selector = Selector.open(); // or SelectorProvider.provider.open() ??
			svrScktChnl = ServerSocketChannel.open();
			svrScktChnl.socket().bind(new InetSocketAddress(port));
			svrScktChnl.configureBlocking(false); // makes server to accept without blocking
			SelectionKey key = svrScktChnl.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("SelectionKey: " + key.channel().toString());
			
			players = new HashMap<SocketChannel, Player>();
			writeReadyPlayers = Collections.synchronizedList(new ArrayList<Player>());
			readBuffer = ByteBuffer.allocate(8192);
			writeBuffer = ByteBuffer.allocate(16384);
			System.out.println("Game ready. Players can join");
			while (true) {
				synchronized (writeReadyPlayers) {
					Iterator<Player> playerIt = this.writeReadyPlayers.iterator();
					while (playerIt.hasNext()) {
						Player aWriteReadyPlayer = (Player) playerIt.next();
						SocketChannel scktChnl = getscktChannel(aWriteReadyPlayer);
						SelectionKey selKey = scktChnl.keyFor(selector);
						selKey.interestOps(SelectionKey.OP_WRITE);
					}
					writeReadyPlayers.clear();
				}
	
				if (selector.selectNow() == 0)
					continue;
				
				Iterator<SelectionKey> selKeyIterator = selector.selectedKeys().iterator();
				
				while (selKeyIterator.hasNext()) {
					SelectionKey selKey = (SelectionKey) selKeyIterator.next();
					selKeyIterator.remove();
					if (!selKey.isValid())
						continue;

					// has a player attempted to join?
					if (selKey.isAcceptable() && !gameStarted) {
						acceptPlayer();
					}
					else if (selKey.isReadable()) {
						readDataFromPlayer(selKey);
					}
					else if (selKey.isWritable()) {
						writeDataToPlayer(selKey);
					}
				}

				if (!timerStarted && playerCounter == 1) {
					// Executes only once
					// First player has joined
					timer.schedule(new LoopBreakerTask(), timeBeforeStart);
					timerStarted = true;
				}
			}
		}
		catch (NullPointerException e) {
			System.out.println("NullPointerException occurred in GameServer run()\n" + e.getMessage());
		}
		catch (ClosedChannelException e) {
			System.out.println("ClosedChannelException occurred in GameServer run()\n" + e.getMessage());
		}
		catch (IOException e) {
			System.out.println("IOException occurred in GameServer run()\n" + e.getMessage());
		}
	}

	private void acceptPlayer() throws IOException {
		SocketChannel aPlayerScktChnl = svrScktChnl.accept();
		aPlayerScktChnl.configureBlocking(false);
		playerCounter++;
		System.out.println("Players joined: " + playerCounter);
		putPlayerOnGame(aPlayerScktChnl);
		writeWelcomeMsgToPlayer(aPlayerScktChnl);
	}
	
	private void readDataFromPlayer(SelectionKey key) throws IOException {
		SocketChannel scktChannel = (SocketChannel)key.channel();
		int dataSize;
		try{
		dataSize = scktChannel.read(readBuffer);
		} catch (IOException e) {
			key.cancel();
			scktChannel.close();
			return;
		}
		System.out.println(new String(readBuffer.array()));
		if(dataSize == -1) {
			// The player client has shut it's socket down. 
			key.channel().close();
			key.cancel();
			return;
		}
		Player p = players.get(scktChannel);
		
		synchronized (p.requestQueue) {
			p.requestQueue.add(readBuffer.getChar());
			p.requestQueue.notify();
		}
		
		readBuffer.clear();
	}
	
	private void writeDataToPlayer(SelectionKey key) throws IOException {
		System.out.println("in server writeDataToPlayer");
		SocketChannel scktChannel = (SocketChannel) key.channel(); 
		Player playerToWriteTo = players.get(scktChannel);
		String responseMsg = prepareResponseMsg(playerToWriteTo.msgToPlayerClient);
		writeBuffer = ByteBuffer.wrap(responseMsg.getBytes());
		scktChannel.write(writeBuffer);
		key.interestOps(SelectionKey.OP_READ);
		writeBuffer.clear();
	}
	
	private void writeWelcomeMsgToPlayer(SocketChannel scktChannel) throws IOException {
		String welcomeMsg = "Join game successful";
		welcomeMsg = prepareResponseMsg(welcomeMsg);
		writeBuffer = ByteBuffer.wrap(welcomeMsg.getBytes());
		scktChannel.register(selector, SelectionKey.OP_WRITE);
		scktChannel.write(writeBuffer);
		writeBuffer.clear();
		SelectionKey selKey = scktChannel.keyFor(selector);
		selKey.interestOps(SelectionKey.OP_READ);
	}

	private void putPlayerOnGame(SocketChannel aPlayer) throws IOException {
		GridLocation l = new GridLocation(gridSize);
		while (!vacant(l)) {
			l.pickAnotherLocation();
		}
		Player p = new Player("P" + playerCounter, l, this);
		grid[l.x][l.y] = p;
		players.put(aPlayer, p);
	}

	private String prepareResponseMsg(String s) {
		String response = "";
		for (int i = 0; i < gridSize; i++) {
			response += "\n";
			for (int j = 0; j < gridSize; j++) {
				if (grid[i][j] == null) {
					response += "\tX\t";
				} else {
					response += "\t" + (grid[i][j]).toString() + "\t";
				}
			}
		}
		response += "\n\n\n" + s.toUpperCase() + "\n";
		return response;
	}
	
	private SocketChannel getscktChannel(Player aWriteReadyPlayer) {
		for(SocketChannel s : players.keySet()) {
			if(players.get(s) == aWriteReadyPlayer) return s;
		}
		return null;
	}
	
	class LoopBreakerTask extends TimerTask {
		@Override
		public void run() {
			gameStarted = true;
			// register all joined players for read
			for (SocketChannel s : players.keySet()) {
				// set interest as read
				s.keyFor(selector).interestOps(SelectionKey.OP_READ);
				// start the player thread
				new Thread(players.get(s)).start(); 
			}
			System.out.println("No more players can join!");
		}
	}

}
