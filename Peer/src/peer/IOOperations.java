package peer;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
/**
 * @author Sadesh
 * All IO operations done between the peer has to be made using this class object only.
 * */

public class IOOperations {
	ObjectInputStream bfRead;
	ObjectOutputStream bfWrite;
	Socket socket;
	DataInputStream in;
	IOOperations(Socket s)
	{
		this.socket=s;
		initWrite();
		initRead();
	}
	IOOperations()
	{
		in=new DataInputStream(System.in);
		
	}
	public void initRead()
	{
		try {
			this.bfRead=new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			System.out.println("Socket "+socket.getInetAddress()+"error getting an input stream");
		}
	}
	public void initWrite()
	{
		try {
			this.bfWrite=new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			System.out.println("Socket "+socket.getInetAddress()+"error getting an output stream");
		}
	}
	public Object streamRead()
	{Object temp=null;

	try {
				temp=bfRead.readObject();
		} catch (IOException e) {
			System.out.println("Socket"+socket.getInetAddress()+" error getting an input stream");
		} catch (ClassNotFoundException e) {
			System.out.println("Class Not Found"+e.getMessage());
		}
	

		return temp;
	}
	public void streamWrite(Object s)
	{
	
		try {
			bfWrite.writeObject(s);
			bfWrite.flush();
		} catch (IOException e) {
			System.out.println("Socket "+socket.getInetAddress()+"error getting an output stream");
		}
		}
	
	}

