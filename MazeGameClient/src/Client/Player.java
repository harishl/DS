package Client;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 
 */

/**
 * @author harish
 *
 */
public class Player {
	public final String serverAddr;
	public final int serverPort;
	private Selector selector = null;
	SocketChannel socketChannel;
	public boolean gameOn;
	public boolean canMove;
	
	public List<Character> userInputs;
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	Thread inputRcvrThread;
	
	public Player() {
		serverAddr = "localhost";
		serverPort = 1234;
		
		try {
			selector = Selector.open();
		} catch (ClosedChannelException e) {
			System.out.println("An Closed Channel Exception occurred. " + e.getMessage());
		} catch (IOException e) {
			System.out.println("An IO Exception occurred. " + e.getMessage());
		}
	}
	
	public void connectToServer() throws IOException {
		socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false); //non-blocking socket channel
		socketChannel.connect(new InetSocketAddress(serverAddr, serverPort));
		SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
		socketChannel.finishConnect();
		if (key.isReadable()) {
			readDataFromServer(key);
		}
		key.interestOps(SelectionKey.OP_READ);
	}

	public void go() {
		userInputs = Collections.synchronizedList(new ArrayList<Character>());
		gameOn = true;
		canMove = false;
		inputRcvrThread = new Thread(new UserInputReceiver(this));
		inputRcvrThread.start();
		
		readBuffer = ByteBuffer.allocate(8192);
		writeBuffer = ByteBuffer.allocate(8192);
		try {
			while (gameOn) {
				if(userInputs.size() > 0) { 
					writeBuffer = ByteBuffer.allocate(8192);
					writeBuffer.clear();
					writeBuffer = ByteBuffer.wrap(userInputs.remove(0).toString().getBytes());
					SelectionKey key = socketChannel.keyFor(selector);
					key.interestOps(SelectionKey.OP_WRITE);
				}
				if (selector.selectNow() == 0)
					continue;
				
				Iterator<SelectionKey> selKeyIterator = selector.selectedKeys().iterator();
				
				while (selKeyIterator.hasNext()) {
					SelectionKey key = (SelectionKey) selKeyIterator.next();
					selKeyIterator.remove();
					if (!key.isValid())
						continue;

					if (key.isReadable()) {
						readDataFromServer(key);
					}
					else if (key.isWritable()) {
						writeDataToServer(key);
					}
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

	private void finishPlaying() throws IOException{
		socketChannel.keyFor(selector).cancel();
		socketChannel.close();
		System.exit(0);
	}

	private void writeDataToServer(SelectionKey key) throws IOException {
		socketChannel.write(writeBuffer);
		key.interestOps(SelectionKey.OP_READ);
	}

	private void readDataFromServer(SelectionKey key) throws IOException {
		readBuffer = ByteBuffer.allocate(8192);
		readBuffer.clear();
		int readLen;
		try{
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
		System.out.println(dataFromServer);
		if(dataFromServer.contains("START MOVING")) {
			userInputs.clear();
			canMove = true;
			System.out.println("Up = w | Down = s | Left = a | Right = d | NoMove = x");
		}
		if(dataFromServer.contains("ENDED")) {
			gameOn = false;
			canMove = false;
		}
		if(dataFromServer.contains("TREASURE")) {
			System.out.println("Up = w | Down = s | Left = a | Right = d | NoMove = x");
		}
		
	}

}
