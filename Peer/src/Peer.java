
import java.io.IOException;
import java.net.BindException;
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
import java.util.Timer;
import java.util.TimerTask;

public class Peer extends Thread {
	PeerSocket ps;
	GameSingleton gs;
	private ServerSocketChannel svrScktChnl;
	private SocketChannel socketChannel;
	public List<Character> userInputs;
	private boolean timerStarted;
	private boolean gameStarted;
	private Timer timer;
	public int timeBeforeStart;
	Thread inputRcvrThread;
	public String playerId;
	boolean ishostPlayer;
	boolean isbackupPlayer=false;
	public boolean gameOn;
	public boolean canMove;
	Peer() {
		ps = new PeerSocket();
		gs=GameSingleton.getInstance();
		ishostPlayer = false;
		try {
			gs.playerSelector = Selector.open();
		} catch (ClosedChannelException e) {
			System.out.println("An Closed Channel Exception occurred. " + e.getMessage());
		} catch (IOException e) {
			System.out.println("An IO Exception occurred. " + e.getMessage());
		}
	}
	Peer(boolean isserverPlayer)
	{
		gs=GameSingleton.getInstance();
		try {
			gs.playerSelector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ishostPlayer=isserverPlayer;

	
	}
	Peer(int gridSize, int noOfTressures) {
		ps = new PeerSocket();
		ishostPlayer = false;
		gs = GameSingleton.getInstance();
		gs.setGridSize(gridSize);
		gs.setNumTreasures(noOfTressures);
		gs.populateTreasures();
		this.timerStarted = false;
		this.gameStarted = false;
		this.timeBeforeStart = PeerConstants.timedelay; 
		timer=new Timer();
		
	}

	public void run() {
		if(ishostPlayer)
		{
			boolean flag=true;
		while(flag)
		{
	
				flag=false;
				this.connectToServer();
	
		}
		this.startPlayer();
		}
		else
		{
		try {
		Peer hostPlayer=new Peer(true);
		hostPlayer.start();
		gs.selector = Selector.open(); // or SelectorProvider.provider.open() ??
			svrScktChnl = ServerSocketChannel.open();
			svrScktChnl.socket().bind(new InetSocketAddress(PeerConstants.port));
			svrScktChnl.configureBlocking(false); // makes server to accept without blocking
			SelectionKey key = svrScktChnl.register(gs.selector, SelectionKey.OP_ACCEPT);
			System.out.println("SelectionKey: " + key.channel().toString());
			
			gs.players = new HashMap<SocketChannel, Player>();
			gs.writeReadyPlayers = Collections.synchronizedList(new ArrayList<Player>());
			
			System.out.println("Game ready. Players can join");
			while (gs.numTreasures >= 0) {
				synchronized (gs.writeReadyPlayers) {
					Iterator<Player> playerIt = gs.writeReadyPlayers.iterator();
					while (playerIt.hasNext()) {
						Player aWriteReadyPlayer = (Player) playerIt.next();
						SocketChannel scktChnl = gs.getscktChannel(aWriteReadyPlayer);
						SelectionKey selKey = scktChnl.keyFor(gs.selector);
						selKey.interestOps(SelectionKey.OP_WRITE);
					}
					gs.writeReadyPlayers.clear();
				}
	
				if (gs.selector.selectNow() == 0)
					continue;
				
				Iterator<SelectionKey> selKeyIterator = gs.selector.selectedKeys().iterator();
				
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
						gs.readDataFromPlayer(selKey);
					}
					else if (selKey.isWritable()) {
						gs.writeDataToPlayer(selKey);
					}
				}

				if (!timerStarted && gs.playercounter == 2) {
					// Executes only once
					// First player has joined
					timer.schedule(new LoopBreakerTask(), timeBeforeStart);
					timerStarted = true;
				}
				if (gs.numTreasures == 0) break;
			}
			
			gs.finishGame();

		}
		catch (NullPointerException e) {
			System.out.println("NullPointerException occurred in Peer run()");
			e.printStackTrace();
		}
		catch (ClosedChannelException e) {
			System.out.println("ClosedChannelException occurred in Peer run()");
			e.printStackTrace();
		}
		catch(BindException e)
		{
			System.out.println("port already in use. Exiting");
			System.exit(0);
		}
		catch (IOException e) {
			System.out.println("IOException occurred in Peer run()");
			e.printStackTrace();
		}}
	}
	
	public void connectToServer()  {
		try{
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false); //non-blocking socket channel
		socketChannel.connect(getServerAddress());
		SelectionKey key = socketChannel.register(gs.playerSelector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
		socketChannel.finishConnect();
		if (key.isReadable()) {
			readDataFromServer(key);
		}
		key.interestOps(SelectionKey.OP_READ);}
		catch (NullPointerException e)
		{
			System.out.println("e:");
			e.printStackTrace();
		}
		catch (IOException e)
		{
			System.out.println("io:"+e.getMessage());
		}
	}
	public void startPlayer() {
		userInputs = Collections.synchronizedList(new ArrayList<Character>());
		gameOn = true;
		canMove = false;
		inputRcvrThread = new Thread(new UserInputReceiver(this));
		inputRcvrThread.start();
		try {
			while (gameOn) {
				if(userInputs.size() > 0) { 
					gs.writeBuffer = ByteBuffer.allocate(8192);
					gs.writeBuffer.clear();
					gs.writeBuffer = ByteBuffer.wrap(userInputs.remove(0).toString().getBytes());
					SelectionKey key = socketChannel.keyFor(gs.playerSelector);
					key.interestOps(SelectionKey.OP_WRITE);
				}
				if (gs.playerSelector.selectNow() == 0)
					continue;
				
				Iterator<SelectionKey> selKeyIterator = gs.playerSelector.selectedKeys().iterator();
				
				while (selKeyIterator.hasNext()) {
					
					SelectionKey key = (SelectionKey) selKeyIterator.next();
					if(((SocketChannel)key.channel()).socket().getPort()==PeerConstants.port){
					selKeyIterator.remove();
					if (!key.isValid())
						continue;

					if (key.isReadable()) {
						readDataFromServer(key);
					}
					else if (key.isWritable()) {
						writeDataToServer(key);
					}}
				}
			}
			
			finishPlaying();
		}
		catch (NullPointerException e) {
			System.out.println("NullPointerException occurred in GameServer run()\n");
			e.printStackTrace();
			System.exit(0);
		}
		catch (ClosedChannelException e) {
			System.out.println("ClosedChannelException occurred in GameServer run()\n");
			e.printStackTrace();
			System.exit(0);
		}
		catch (IOException e) {
			System.out.println("IOException occurred in GameServer run()\n");
			e.printStackTrace();
			System.exit(0);
		}
	


	}

	private InetSocketAddress getServerAddress() {
		InetSocketAddress iaddress = new InetSocketAddress(
				PeerConstants.serverAddress, PeerConstants.port);
		return iaddress;
	}
	public void acceptPlayer() throws IOException {
		SocketChannel aPlayerScktChnl = svrScktChnl.accept();
		aPlayerScktChnl.configureBlocking(false);
		gs.playercounter++;
		System.out.println("Players joined: " + gs.playercounter);
		gs.putPlayerOnGame(aPlayerScktChnl);
		gs.writeWelcomeMsgToPlayer(aPlayerScktChnl);
		
	}
	private void finishPlaying() throws IOException{
		socketChannel.keyFor(	gs.playerSelector).cancel();
		socketChannel.close();
		System.exit(0);
	}

	private void writeDataToServer(SelectionKey key) throws IOException {
		socketChannel.write(gs.writeBuffer);
		key.interestOps(SelectionKey.OP_READ);
	}

	private void readDataFromServer(SelectionKey key) throws IOException {
		gs.readBuffer = ByteBuffer.allocate(8192);
		gs.readBuffer.clear();
		int readLen;
		try{
			readLen = socketChannel.read(gs.readBuffer);
		    if (readLen == -1) {
		    	throw new IOException();
		    }
		} catch (IOException e) {
			socketChannel.close();
			key.cancel();
			return;
		}
		
		String dataFromServer = new String(gs.readBuffer.array());
		
		if(dataFromServer.contains("backup"))
		{
			this.isbackupPlayer=true;
			dataFromServer=dataFromServer.substring(0,dataFromServer.indexOf("backup"));
			
		}
		System.out.println(dataFromServer);
		if(dataFromServer.contains("PLAYER"))
		{
			int indexstart=dataFromServer.indexOf("PLAYER");
			int indexend=dataFromServer.indexOf(",");
			//System.out.println("Index Start:"+indexstart+"\n IndexEnd:"+indexend+"\n player id:"+dataFromServer.substring(indexstart+7, indexend));
			this.playerId=dataFromServer.substring(indexstart+7, indexend);
			if(gs.gridSize>0)
			{
				gs.primaryPlayerId=this.playerId;
			}
		}
		else if(dataFromServer.contains("START MOVING")) {
			userInputs.clear();
			canMove = true;
			System.out.println("Up = w | Down = s | Left = a | Right = d | NoMove = x");
		}
		else if(dataFromServer.contains("ENDED")) {
			gameOn = false;
			canMove = false;
		}
		else if(dataFromServer.contains("TREASURE")) {
			System.out.println("Up = w | Down = s | Left = a | Right = d | NoMove = x");
		}
		System.out.println(this.isbackupPlayer);
		
	}

	
	class LoopBreakerTask extends TimerTask {
		@Override
		public void run() {
			
			gameStarted = true;
			// register all joined players for read
			for (SocketChannel s : gs.players.keySet()) {

				// set interest as read
				
				s.keyFor(gs.selector).interestOps(SelectionKey.OP_READ);
				// start the player thread
				Player p = gs.players.get(s);
				new Thread(p).start(); 
				p.msgToPlayerClient = "start moving";
			
				gs.writeReadyPlayers.add(p);
			}
			System.out.println("No more players can join!");
		}
	}
	
}
