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
	
	public List<Character> userInputs;
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffer;
	
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
		key.interestOps(SelectionKey.OP_READ);
	}

	public void go() {
		userInputs = Collections.synchronizedList(new ArrayList<Character>());
		Thread inputRcvrThread = new Thread(new UserInputReceiver(this));
		inputRcvrThread.start(); 
		readBuffer = ByteBuffer.allocate(8192);
		writeBuffer = ByteBuffer.allocate(16384);
		try {
			while (true) {
				if(userInputs.size() > 0) { 
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
		}
		catch (NullPointerException e) {
			System.out.println("NullPointerException accurred in GameServer run()\n" + e.getMessage());
		}
		catch (ClosedChannelException e) {
			System.out.println("ClosedChannelException occurred in GameServer run()\n" + e.getMessage());
		}
		catch (IOException e) {
			System.out.println("IOException occurred in GameServer run()\n" + e.getMessage());
		}
	}

	private void writeDataToServer(SelectionKey key) throws IOException {
		System.out.println("in writeDataToServer");
		socketChannel.write(writeBuffer);
		key.interestOps(SelectionKey.OP_READ);
		writeBuffer.clear();
	}

	private void readDataFromServer(SelectionKey key) throws IOException {
		System.out.println("in readDataFromServer");
		try{
			socketChannel.read(readBuffer);
		} catch (IOException e) {
			key.cancel();
			socketChannel.close();
			return;
		}
		
		System.out.println(new String(readBuffer.array()));
		readBuffer.clear();
	}

}
