
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * Every Peer will have a ServerSocket and a Socket to maintain a peertopeer
 * network. Each peer has an opportunity to become a Primary server or an
 * backupserver.
 */
public class PeerSocket {
	 public static void main(String args[])
	  {
	  try{
	  // Create file 
	File file=new File("out.txt");
	 FileWriter fstream = new FileWriter(file,true);
	  BufferedWriter out = new BufferedWriter(fstream);

	  out.append("Hello Java");

	  //Close the output stream
	  out.close();
	 fstream = new FileWriter(file,true);
	out = new BufferedWriter(fstream);
	  out.append("Hello Java");

	  //Close the output stream
	  out.close();
	  }catch (Exception e){//Catch exception if any
	  System.err.println("Error: " + e.getMessage());
	  }
	  }

}
