import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class PeertoPeer implements Cloneable, Runnable {
	Thread thread;
	ServerSocket peerServerSocket;
	Socket clientSocket;
	boolean serverFlag;
	ObjectInputStream bfRead;
	ObjectOutputStream bfWrite;
	public PeertoPeer(){
		boolean flag=true;
		while(flag){
		try {
			flag=false;
			peerServerSocket=new ServerSocket(12341);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			flag=true;
		}}
		serverFlag=true;
		thread=new Thread(this);
		thread.start();
	
	}
	public PeertoPeer(int port) throws IOException {
		serverFlag=false;
		clientSocket=new Socket("localhost",port);
		
	}
	public static void main(String args[])throws IOException {

		if (args.length == 2) {
			PeertoPeer peer=new PeertoPeer();
			peer.startServerSocket(args);
		} else if(args.length==1){
			PeertoPeer peer=new PeertoPeer(Integer.parseInt(args[0]));
			peer.startClientSocket();
		}
		else
		{
			System.out.println("Enter proper arguments for server and client");
			System.exit(0);
		}
	}
	private void initReader()
	{
		try {
			this.bfRead=new ObjectInputStream(new DataInputStream(clientSocket.getInputStream()));
		} catch (IOException e) {
			System.out.println("Socket "+clientSocket.getInetAddress()+"error getting an input stream");
		}
	}
	private void initWrite()
	{
		try {
			this.bfWrite=new ObjectOutputStream(new DataOutputStream(clientSocket.getOutputStream()));
			bfWrite.flush();
		} catch (IOException e) {
			System.out.println("Socket "+clientSocket.getInetAddress()+"error getting an output stream");
		}
	}
	private Object streamRead()
	{Object temp=null;
		try {
				temp=bfRead.readObject();
		} catch (IOException e) {
			System.out.println("Socket error getting an input stream");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return temp;
	}
	private void streamWrite(Object s)
	{
		try {
			bfWrite.writeObject(s);
		} catch (IOException e) {
			System.out.println("Socket error getting an output stream");
		}
	}
	private void startClientSocket() {
		initReader();
		initWrite();
		DataInputStream in=new DataInputStream(System.in);
		try {
			System.out.println(clientSocket.getPort());
			System.out.println(clientSocket.getKeepAlive());
			System.out.println(clientSocket.getInetAddress());
			System.out.println(clientSocket.getRemoteSocketAddress());
			System.out.println(clientSocket.isConnected());
			System.out.println(clientSocket.isInputShutdown());
			System.out.println(clientSocket.isOutputShutdown());
			System.out.println(clientSocket.isBound());
			System.out.println(clientSocket.isClosed());
					streamWrite(in.readLine());
			System.out.println((String)streamRead());
		String tempString=in.readLine();
		streamWrite(tempString);
	
		
		System.out.println(clientSocket.getPort());
		System.out.println(clientSocket.getKeepAlive());
		System.out.println(clientSocket.getInetAddress());
		System.out.println(clientSocket.getRemoteSocketAddress());
		System.out.println(clientSocket.isConnected());
		System.out.println(clientSocket.isInputShutdown());
		System.out.println(clientSocket.isOutputShutdown());
		System.out.println(clientSocket.isBound());
		System.out.println(clientSocket.isClosed());
	
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	System.out.println(streamRead());
	System.out.println(streamRead());
	streamWrite("Hi");
	streamWrite("Exit");
		

	}

	private  void startServerSocket(String args[]) {
//need to call harish game init method
	}

	@Override
	public void run() {
		while(true)
		{
			try {
				clientSocket=peerServerSocket.accept();
				clientSocket.setKeepAlive(true);
				System.out.println(clientSocket.getPort());
				System.out.println(clientSocket.getKeepAlive());
				System.out.println(clientSocket.getInetAddress());
				System.out.println(clientSocket.getRemoteSocketAddress());
				System.out.println(clientSocket.getSoLinger());
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			initWrite();
			System.out.println("HI");
			initReader();
		
//			System.out.println("HOW");
		
		streamWrite("HI");
		//initReader();
	System.out.println((String)streamRead());
			if(streamRead().equals("Exit"));
			break;
		}

	}

	
	

}