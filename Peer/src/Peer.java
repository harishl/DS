import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Peer extends Thread {

	GameSingleton gs;
	File file;
	FileWriter fstream;
	BufferedWriter out;
	public ByteBuffer readBuffer;
	public ByteBuffer writeBuffer;
	public Map<SocketChannel, Player> players;
	public Selector selector = null;
	public Selector playerSelector = null;
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
	boolean isbackupPlayer = false;
	public boolean gameOn;
	public boolean canMove;
	public boolean backupServerAcceptedAllPlayers = false;
	BackupRmiServer server;

	Peer() {

		gs = GameSingleton.getInstance();
		readBuffer = ByteBuffer.allocate(8192);
		writeBuffer = ByteBuffer.allocate(8192);
		ishostPlayer = false;
		try {
			playerSelector = Selector.open();
		} catch (ClosedChannelException e) {
			System.out.println("An Closed Channel Exception occurred. "
					+ e.getMessage());
		} catch (IOException e) {
			System.out.println("An IO Exception occurred. " + e.getMessage());
		}
	}

	Peer(boolean isserverPlayer) {
		readBuffer = ByteBuffer.allocate(8192);
		writeBuffer = ByteBuffer.allocate(8192);
		gs = GameSingleton.getInstance();
		try {
			playerSelector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ishostPlayer = isserverPlayer;

	}

	Peer(int gridSize, int noOfTressures) {

		ishostPlayer = false;
		gs = GameSingleton.getInstance();
		gs.setGridSize(gridSize);
		gs.setNumTreasures(noOfTressures);
		gs.populateTreasures();
		// players=new HashMap<SocketChannel, Player>();
		this.timerStarted = false;
		this.gameStarted = false;
		this.timeBeforeStart = PrimaryServerConstants.timedelay;
		timer = new Timer();

	}

	Peer(String id) {
		this.isbackupPlayer = true;
		this.playerId = id;
		gs = GameSingleton.getInstance();
		gs.primaryPlayerId = this.playerId;
		
	}

	public void run() {
		if (isbackupPlayer) {
			/*boolean flag=true;
			while(flag){*/
			try {
				selector = Selector.open();
				svrScktChnl = ServerSocketChannel.open();
				svrScktChnl.socket().bind(
						new InetSocketAddress(gs.backpServerPort));
				svrScktChnl.configureBlocking(false);
				SelectionKey key = svrScktChnl.register(selector,
						SelectionKey.OP_ACCEPT);
				System.out.println("SelectionKey: " + key.channel().toString());

				players = new HashMap<SocketChannel, Player>();
				gs.writeReadyPlayers = Collections
						.synchronizedList(new ArrayList<Player>());
				readBuffer = ByteBuffer.allocate(8192);
				writeBuffer = ByteBuffer.allocate(8192);
				file = new File(gs.primaryPlayerId + "primary.txt");
				System.out.println(file.getPath());
				if (file.isFile())
					file.delete();
				file.createNewFile();
			//	flag=false;

			} catch (NullPointerException e) {
				System.out
						.println("NullPointerException occurred in Peer run()");
				e.printStackTrace();
			} catch (ClosedChannelException e) {
				System.out
						.println("ClosedChannelException occurred in Peer run()");
				e.printStackTrace();
			} catch (BindException e) {
				//flag=true;
				System.out.println("port already in use. Exiting");
				
			} catch (IOException e) {
				System.out.println("IOException occurred in Peer run()");
				e.printStackTrace();
			}//}
			while (gs.numTreasures >= 0) {
				try {
					synchronized (gs.writeReadyPlayers) {
						Iterator<Player> playerIt = gs.writeReadyPlayers
								.iterator();
						while (playerIt.hasNext()) {
							Player aWriteReadyPlayer = (Player) playerIt.next();
							SocketChannel scktChnl = getscktChannel(aWriteReadyPlayer);
							SelectionKey selKey = scktChnl.keyFor(selector);
							selKey.interestOps(SelectionKey.OP_WRITE);
						}
						gs.writeReadyPlayers.clear();
					}

					if (selector.selectNow() == 0)
						continue;

					Iterator<SelectionKey> selKeyIterator = selector
							.selectedKeys().iterator();

					while (selKeyIterator.hasNext()) {
						SelectionKey selKey = (SelectionKey) selKeyIterator
								.next();
						selKeyIterator.remove();
						if (!selKey.isValid())
							continue;

						// has a player attempted to join?
						if (selKey.isAcceptable()) {
							if (!backupServerAcceptedAllPlayers) {// need to
																	// change
																	// the flag
								acceptPlayerforbackup();// need to change
														// methods
							} else {
								kickPlayerOut();
							}
						} else if (selKey.isReadable()) {
							readDataFromPlayer(selKey);
						} else if (selKey.isWritable()) {
							writeDataToPlayer(selKey);

							new BackupRMIClient().saveBackupData();
							fstream = new FileWriter(file, true);
							out = new BufferedWriter(fstream);
							out.append(gs.getTime()
									+ gs.prepareResponseMsg("-----------------"));
							out.close();

						}
					}
					if (gs.numTreasures == 0)
						break;
				} catch (java.rmi.ConnectException e) {
					gs.crashedPlayersandBackupserver.add(gs.backupPlayerId);
					Iterator<Player> playerIterator = gs.playerlist.iterator();
					while (playerIterator.hasNext()) {
						Player p = playerIterator.next();
						if (!(p.id.equals(gs.primaryPlayerId) || gs.crashedPlayersandBackupserver
								.contains(p.id))) {
							gs.backupPlayerId = p.id;
							SocketChannel scktChnl = getscktChannel(p);
							SelectionKey backupkey = scktChnl.keyFor(selector);
							backupkey.interestOps(SelectionKey.OP_WRITE);
							writeBuffer.clear();
							SocketChannel scktChannel = (SocketChannel) backupkey
									.channel();
							String responseMsg = "backup";
							writeBuffer = ByteBuffer.wrap(responseMsg
									.getBytes());

							try {
								scktChannel.write(writeBuffer);
								backupkey.interestOps(SelectionKey.OP_READ);
								Thread.sleep(500);
								new BackupRMIClient().saveBackupData();
								fstream = new FileWriter(file, true);
								out = new BufferedWriter(fstream);
								out.append(gs.getTime()
										+ "Back up server changed to"
										+ p.id
										+ " "
										+ gs.prepareResponseMsg("-----------------"));
								out.close();
								for (SocketChannel s : players.keySet()) {
									if (gs.crashedPlayersandBackupserver
											.contains(players.get(s).id))
										continue;

									backupkey = s.keyFor(selector);
									backupkey
											.interestOps(SelectionKey.OP_WRITE);
									writeBuffer.clear();
									SocketChannel sChannel = (SocketChannel) backupkey
											.channel();
									String Msg = "PORT"
											+ Integer
													.toString(PeerConstants.port
															+ gs.crashedPlayersandBackupserver
																	.size() + 1)+" ";
									writeBuffer = ByteBuffer.wrap(Msg
											.getBytes());
									sChannel.write(writeBuffer);
									backupkey.interestOps(SelectionKey.OP_READ);

								}
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (NotBoundException e1) {
								e1.printStackTrace();
							} catch (java.rmi.ConnectException e2) {
								e2.printStackTrace();
							} catch (RemoteException e3) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e4) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							break;
						}
					}

				} catch (NotBoundException e) {
					e.printStackTrace();
				} catch (ConnectException e2) {
					e2.printStackTrace();
				} catch (RemoteException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				} catch (IOException e4) {
					// TODO Auto-generated catch block
					e4.printStackTrace();
				}
			}
			try {
				finishServerGame();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				finishServerGame();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (ishostPlayer) {
			boolean flag = true;
			while (flag) {

				flag = false;
				this.connectToServer();

			}
			this.startPlayer();
		} else {
			try {
				Peer hostPlayer = new Peer(true);
				hostPlayer.start();
				selector = Selector.open(); 
				svrScktChnl = ServerSocketChannel.open();
				svrScktChnl.socket().bind(
						new InetSocketAddress(PrimaryServerConstants.port));
				svrScktChnl.configureBlocking(false); // makes server to accept
														// without blocking
				SelectionKey key = svrScktChnl.register(selector,
						SelectionKey.OP_ACCEPT);
				System.out.println("SelectionKey: " + key.channel().toString());

				players = new HashMap<SocketChannel, Player>();
				gs.writeReadyPlayers = Collections
						.synchronizedList(new ArrayList<Player>());
				readBuffer = ByteBuffer.allocate(8192);
				writeBuffer = ByteBuffer.allocate(8192);
				System.out.println("Game ready. Players can join");
			} catch (NullPointerException e) {
				System.out
						.println("NullPointerException occurred in Peer run()");
				e.printStackTrace();
			} catch (ClosedChannelException e) {
				System.out
						.println("ClosedChannelException occurred in Peer run()");
				e.printStackTrace();
			} catch (BindException e) {
				System.out.println("port already in use. Exiting");
				System.exit(0);
			} catch (IOException e) {
				System.out.println("IOException occurred in Peer run()");
				e.printStackTrace();
			}
			boolean flag = true;

			while (gs.numTreasures >= 0) {
				try {
					if (null != gs.primaryPlayerId && flag) {
						flag = false;
						file = new File(gs.primaryPlayerId + "primary.txt");
						System.out.println(file.getPath());
						if (file.isFile())
							file.delete();

						file.createNewFile();

					}
					synchronized (gs.writeReadyPlayers) {
						Iterator<Player> playerIt = gs.writeReadyPlayers
								.iterator();
						while (playerIt.hasNext()) {
							Player aWriteReadyPlayer = (Player) playerIt.next();
							SocketChannel scktChnl = getscktChannel(aWriteReadyPlayer);
							SelectionKey selKey = scktChnl.keyFor(selector);
							selKey.interestOps(SelectionKey.OP_WRITE);
						}
						gs.writeReadyPlayers.clear();
					}

					if (selector.selectNow() == 0)
						continue;

					Iterator<SelectionKey> selKeyIterator = selector
							.selectedKeys().iterator();

					while (selKeyIterator.hasNext()) {
						SelectionKey selKey = (SelectionKey) selKeyIterator
								.next();
						selKeyIterator.remove();
						if (!selKey.isValid())
							continue;

						// has a player attempted to join?
						if (selKey.isAcceptable()) {
							if (!gameStarted) {
								acceptPlayer();
							} else {
								kickPlayerOut();
							}
						} else if (selKey.isReadable()) {
							readDataFromPlayer(selKey);
						} else if (selKey.isWritable()) {
							writeDataToPlayer(selKey);

							new BackupRMIClient().saveBackupData();
							fstream = new FileWriter(file, true);
							out = new BufferedWriter(fstream);
							out.append(gs.getTime()
									+ gs.prepareResponseMsg("-----------------"));
							out.close();

						}
					}

					if (!timerStarted && gs.playercounter == 1) {
						// Executes only once
						// First player has joined
						timer.schedule(new LoopBreakerTask(), timeBeforeStart);
						timerStarted = true;
					}
					if (gs.numTreasures == 0)
						break;
				} catch (java.rmi.ConnectException e) {
					gs.crashedPlayersandBackupserver.add(gs.backupPlayerId);
					Iterator<Player> playerIterator = gs.playerlist.iterator();
					while (playerIterator.hasNext()) {
						Player p = playerIterator.next();
						if (!(p.id.equals(gs.primaryPlayerId) || gs.crashedPlayersandBackupserver
								.contains(p.id))) {
							gs.backupPlayerId = p.id;
							SocketChannel scktChnl = getscktChannel(p);
							SelectionKey backupkey = scktChnl.keyFor(selector);
							backupkey.interestOps(SelectionKey.OP_WRITE);
							writeBuffer.clear();
							SocketChannel scktChannel = (SocketChannel) backupkey
									.channel();
							String responseMsg = "backup";
							writeBuffer = ByteBuffer.wrap(responseMsg
									.getBytes());

							try {
								scktChannel.write(writeBuffer);
								backupkey.interestOps(SelectionKey.OP_READ);
								Thread.sleep(500);
								new BackupRMIClient().saveBackupData();
								fstream = new FileWriter(file, true);
								out = new BufferedWriter(fstream);
								out.append(gs.getTime()
										+ "Back up server changed to"
										+ p.id
										+ " "
										+ gs.prepareResponseMsg("-----------------"));
								out.close();
								for (SocketChannel s : players.keySet()) {
									if (gs.crashedPlayersandBackupserver
											.contains(players.get(s).id))
										continue;

									backupkey = s.keyFor(selector);
									backupkey
											.interestOps(SelectionKey.OP_WRITE);
									writeBuffer.clear();
									SocketChannel sChannel = (SocketChannel) backupkey
											.channel();
									String Msg = "PORT"
											+ Integer
													.toString(PeerConstants.port
															+ gs.crashedPlayersandBackupserver
																	.size() + 1);
									writeBuffer = ByteBuffer.wrap(Msg
											.getBytes());
									sChannel.write(writeBuffer);
									backupkey.interestOps(SelectionKey.OP_READ);

								}
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (NotBoundException e1) {
								e1.printStackTrace();
							} catch (java.rmi.ConnectException e2) {
								e2.printStackTrace();
							} catch (RemoteException e3) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e4) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							break;
						}
					}

				} catch (NotBoundException e) {
					e.printStackTrace();
				} catch (ConnectException e2) {
					e2.printStackTrace();
				} catch (RemoteException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				} catch (IOException e4) {
					// TODO Auto-generated catch block
					e4.printStackTrace();
				}
			}
			try {
				finishServerGame();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	private void acceptPlayerforbackup() throws IOException {
		SocketChannel aPlayerScktChnl = svrScktChnl.accept();
		aPlayerScktChnl.configureBlocking(false);
		writeBuffer.clear();
		writeBuffer = ByteBuffer.wrap("GETPLAYERID".getBytes());
		aPlayerScktChnl.register(selector, SelectionKey.OP_WRITE|SelectionKey.OP_READ);
		SelectionKey key = aPlayerScktChnl.keyFor(selector);
		//key.interestOps(SelectionKey.OP_WRITE);
		aPlayerScktChnl.write(writeBuffer);
		
		boolean playerAdditionFlag = true;
		while (playerAdditionFlag) {
			
			if (selector.selectNow() == 0)
				continue;

			Iterator<SelectionKey> selKeyIterator = selector
					.selectedKeys().iterator();

			while (selKeyIterator.hasNext()) {

				key = (SelectionKey) selKeyIterator.next();
			//	if (((SocketChannel) key.channel()).socket().getPort() == PeerConstants.port) {
					selKeyIterator.remove();
					if (!key.isValid())
						continue;

					if (key.isReadable()) {
						playerAdditionFlag = false;
						readBuffer = ByteBuffer.allocate(8192);
						readBuffer.clear();
						int readLen = aPlayerScktChnl.read(readBuffer);
						String dataFromPlayer = new String(readBuffer.array());
						System.out.println(dataFromPlayer);
						for (Player s : gs.playerlist) {
							System.out.println(s.id);
							System.out.println(dataFromPlayer.contains(s.id));
							if (dataFromPlayer.contains(s.id)) {
								Player p = new Player(dataFromPlayer, s.position,
										aPlayerScktChnl, s.numCollectedTreasures);
								players.put(aPlayerScktChnl, p);//player have to add server address
								gs.playerlist.remove(s);
								gs.playerlist.add(p);
								break;
							}
						}
						System.out.println("players"+players.size());
						System.out.println("playercounter"+gs.playercounter);
						System.out.println("crashedPlayersandBackupserver"+gs.crashedPlayersandBackupserver.size());
						if (players.size() == (gs.playercounter - gs.crashedPlayersandBackupserver
								.size())) {
							backupServerAcceptedAllPlayers = true;
							for (SocketChannel s : players.keySet()) {

								// set interest as read

								s.keyFor(selector).interestOps(SelectionKey.OP_READ);
								// start the player thread
								Player p = players.get(s);
								new Thread(p).start();
								System.out.println(" gs.backupPlayerId"+ gs.backupPlayerId);
								System.out.println("gs.primaryPlayerId"+gs.primaryPlayerId);
								System.out.println("p.id"+p.id);
								System.out.println(gs.backupPlayerId.trim().equalsIgnoreCase(p.id.trim()));
								if (gs.backupPlayerId.trim().equalsIgnoreCase(p.id.trim())) {
									p.msgToPlayerClient = "Game Resumed backup";
								} else {
									p.msgToPlayerClient = "Game Resumed";
								}
								gs.writeReadyPlayers.add(p);
							}
						}
					}
					aPlayerScktChnl.keyFor(selector).interestOps(SelectionKey.OP_READ);
				//}
			}
			
			
		}
		
	}

	/**
	 * Connects a player peer to the primary server
	 */
	public void connectToServer() {
		try {
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false); // non-blocking socket channel
			socketChannel.connect(getServerAddress());
			SelectionKey key = socketChannel.register(playerSelector,
					SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
			socketChannel.finishConnect();
			if (key.isReadable()) {
				readDataFromServer(key);
			}
			key.interestOps(SelectionKey.OP_READ);
		} catch (NullPointerException e) {
			System.out.println("e:");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("io:" + e.getMessage());
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
				if (userInputs.size() > 0) {
					writeBuffer = ByteBuffer.allocate(8192);
					writeBuffer.clear();
					writeBuffer = ByteBuffer.wrap(userInputs.remove(0)
							.toString().getBytes());
					SelectionKey key = socketChannel.keyFor(playerSelector);
					key.interestOps(SelectionKey.OP_WRITE); //set OP_WRITE only when there is data to write
				}
				if (!socketChannel.isOpen()) {
					if (this.isbackupPlayer) {
						server.stopServer();
						System.out.println("RMI server interrupted");
						gs.crashedPlayersandBackupserver.add(gs.primPlayerIdfromRMI);
					}
					socketChannel.close();
					socketChannel = SocketChannel.open();
					socketChannel.configureBlocking(false); // non-blocking
															// socket
															// channel
					socketChannel.connect(new InetSocketAddress(PeerConstants.serverAddress,gs.backpServerPort));
					socketChannel.register(playerSelector,
							SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
					socketChannel.finishConnect();
					SelectionKey key = socketChannel.keyFor(playerSelector);
					if (key.isReadable()) {
						readDataFromServer(key);
					}
					key.interestOps(SelectionKey.OP_READ);

				}
				if (playerSelector.selectNow() == 0)
					continue;

				Iterator<SelectionKey> selKeyIterator = playerSelector
						.selectedKeys().iterator();

				while (selKeyIterator.hasNext()) {

					SelectionKey key = (SelectionKey) selKeyIterator.next();
						selKeyIterator.remove();
						if (!key.isValid())
							continue;

						if (key.isReadable()) {
							readDataFromServer(key);
						} else if (key.isWritable()) {
							writeDataToServer(key);
						}
				}
			}

			finishPlaying(); // when gameOn flag becomes false
		} catch (NullPointerException e) {
			System.out
					.println("NullPointerException occurred in GameServer run()\n");
			e.printStackTrace();
			System.exit(0);
		} catch (ClosedChannelException e) {
			System.out
					.println("ClosedChannelException occurred in GameServer run()\n");
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			System.out.println("IOException occurred in GameServer run()\n");
			e.printStackTrace();
			System.exit(0);
		}

	}

	private InetSocketAddress getServerAddress() {
		InetSocketAddress iaddress = new InetSocketAddress(
				PrimaryServerConstants.serverAddress, PrimaryServerConstants.port);
		return iaddress;
	}

	public void acceptPlayer() throws IOException {
		SocketChannel aPlayerScktChnl = svrScktChnl.accept();
		aPlayerScktChnl.configureBlocking(false);
		gs.playercounter++;
		System.out.println("Players joined: " + gs.playercounter);
		gs.putPlayerOnGame(aPlayerScktChnl, this);
		writeWelcomeMsgToPlayer(aPlayerScktChnl);

	}

	private void finishPlaying() throws IOException {
		socketChannel.keyFor(playerSelector).cancel();
		socketChannel.close();
		System.exit(0);
	}

	private void writeDataToServer(SelectionKey key) throws IOException {
		socketChannel.write(writeBuffer);
		System.out.println(socketChannel.socket().getPort());
		key.interestOps(SelectionKey.OP_READ);
	}

	private void readDataFromServer(SelectionKey key) throws IOException {
		readBuffer = ByteBuffer.allocate(8192);
		readBuffer.clear();
		int readLen;
		try {
			readLen = socketChannel.read(readBuffer);
			if (readLen == -1) {
				throw new IOException();
			}
		} catch (IOException e) {
			socketChannel.close();
			key.cancel();
			return;
		}

		String dataFromServer = new String(readBuffer.array());
		if (dataFromServer.contains("GETPLAYERID")) {
			writeBuffer.clear();
			writeBuffer = ByteBuffer.wrap(this.playerId.getBytes());
			key = socketChannel.keyFor(playerSelector);
			System.out.println(socketChannel.socket().getPort());
			key.interestOps(SelectionKey.OP_WRITE);
			try{
			writeDataToServer(key);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		if (dataFromServer.contains("PORT")) {
			String temp ;
			if(dataFromServer.contains(" ")) {
				temp = dataFromServer.substring(4,
					dataFromServer.indexOf(" "));
			}
			else {
				temp=dataFromServer.substring(4);

			}
			System.out.println("temp:" + temp);
			gs.backpServerPort = Integer.parseInt(temp.trim()) + 1;

			System.out.println(gs.backpServerPort);
			dataFromServer = dataFromServer.substring(dataFromServer
					.indexOf(" ") + 1);

		}
		if (dataFromServer.contains("PLAYER")) {

			int indexstart = dataFromServer.indexOf("PLAYER");
			int indexend = dataFromServer.indexOf(",");
			if (indexend > 0) {
				this.playerId = dataFromServer.substring(indexstart + 7,
						indexend);
				gs.filename = this.playerId + "backup.txt";
				if (gs.gridSize > 0) {
					gs.primaryPlayerId = this.playerId;

				}
			}
		}
		if (dataFromServer.contains("backup")||dataFromServer.contains("BACKUP")) {
			this.isbackupPlayer = true;
			if(dataFromServer.contains("backup"))
			dataFromServer = dataFromServer.substring(0,
					dataFromServer.indexOf("backup"));
			else
				dataFromServer = dataFromServer.substring(0,
						dataFromServer.indexOf("BACKUP"));
			gs.filename = this.playerId + "backup.txt";
			server = new BackupRmiServer(this.playerId + "rmi");
			server.start();
			Peer p = new Peer(this.playerId);
		p.start();
			
System.out.println("End of Backup"+gs.backpServerPort);
		}
		System.out.println(dataFromServer);
		if (dataFromServer.contains("START MOVING")) {
			userInputs.clear();
			canMove = true;
			System.out
					.println("Up = w | Down = s | Left = a | Right = d | NoMove = x");
		}
		if (dataFromServer.contains("ENDED")) {
			gameOn = false;
			canMove = false;
		}
		if (dataFromServer.contains("TREASURE")) {
			System.out
					.println("Up = w | Down = s | Left = a | Right = d | NoMove = x");
		}
		if (dataFromServer.contains("CANNOT JOIN")) {
			System.exit(0);
		}
	//	System.out.println(this.isbackupPlayer);

	}

	private void kickPlayerOut() throws IOException {
		SocketChannel aPlayerScktChnl = svrScktChnl.accept();
		aPlayerScktChnl.configureBlocking(false);
		// putPlayerOnGame(aPlayerScktChnl);
		writeRejectMsgToPlayer(aPlayerScktChnl);
	}

	private void writeRejectMsgToPlayer(SocketChannel scktChannel)
			throws IOException {
		writeBuffer.clear();
		String msg = "Game has started. Cannot Join.";
		writeBuffer = ByteBuffer.wrap(msg.toUpperCase().getBytes());
		scktChannel.register(selector, SelectionKey.OP_WRITE);
		scktChannel.write(writeBuffer);
		SelectionKey selKey = scktChannel.keyFor(selector);
		selKey.cancel();
		scktChannel.close();
	}

	public void finishServerGame() throws IOException {
		Player winner = null;
		int maxTreasures = 0;
		for (SocketChannel s : players.keySet()) {
			if (players.get(s).numCollectedTreasures > maxTreasures) {
				winner = players.get(s);
				maxTreasures = winner.numCollectedTreasures;
			}
		}

		for (SocketChannel s : players.keySet()) {
			if (gs.crashedPlayersandBackupserver.contains(players.get(s).id))
				continue;
			writeBuffer.clear();
			SelectionKey key = s.keyFor(selector);
			if(key != null) {
				key.interestOps(SelectionKey.OP_WRITE);
				String goodByeMsg = "Player " + winner.id
						+ " has won the game. The game has ended.";
				goodByeMsg = gs.prepareResponseMsg(goodByeMsg);
				writeBuffer = ByteBuffer.wrap(goodByeMsg.getBytes());
				s.write(writeBuffer);
				System.out.println(goodByeMsg);
				key.cancel();
				s.close();
			}
		}
		selector.close();

		// Exit the program
		System.exit(0);
	}

	public void readDataFromPlayer(SelectionKey key) throws IOException {
		readBuffer.clear();
		SocketChannel scktChannel = (SocketChannel) key.channel();
		int dataSize;
		try {
			dataSize = scktChannel.read(readBuffer);
		} catch (IOException e) {
			key.cancel();
			scktChannel.close();
			return;
		}
		System.out.println(new String(readBuffer.array()));
		if (dataSize == -1) {
			// The player client has shut it's socket down.
			key.channel().close();
			key.cancel();
			return;
		}
		Player p = players.get(scktChannel);

		synchronized (p.requestQueue) {
			p.requestQueue.add((char) readBuffer.array()[0]);
			p.requestQueue.notify();
		}
	}

	public void writeDataToPlayer(SelectionKey key) throws IOException {
		writeBuffer.clear();
		SocketChannel scktChannel = (SocketChannel) key.channel();
		Player playerToWriteTo = players.get(scktChannel);
		String responseMsg = gs
				.prepareResponseMsg(playerToWriteTo.msgToPlayerClient);
		writeBuffer = ByteBuffer.wrap(responseMsg.getBytes());
		scktChannel.write(writeBuffer);
		key.interestOps(SelectionKey.OP_READ);

	}

	public void writeWelcomeMsgToPlayer(SocketChannel scktChannel)
			throws IOException {
		writeBuffer.clear();
		String msg = " You've joined the game. \nYou are Player "
				+ players.get(scktChannel).id + ","
				+ "\nPlease wait for start signal.";
		msg = "PORT" + Integer.toString(PeerConstants.port +gs.crashedPlayersandBackupserver.size() +1) + " "
				+ gs.prepareResponseMsg(msg);
		writeBuffer = ByteBuffer.wrap(msg.getBytes());
		scktChannel.register(selector, SelectionKey.OP_WRITE);
		scktChannel.write(writeBuffer);
		SelectionKey selKey = scktChannel.keyFor(selector);
		selKey.interestOps(SelectionKey.OP_READ);
	}

	public SocketChannel getscktChannel(Player aWriteReadyPlayer) {
		for (SocketChannel s : players.keySet()) {
			if (players.get(s) == aWriteReadyPlayer)
				return s;
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
				Player p = players.get(s);
				new Thread(p).start();
				p.msgToPlayerClient = "start moving";

				gs.writeReadyPlayers.add(p);
			}
			System.out.println("No more players can join!");
		}
	}

}
