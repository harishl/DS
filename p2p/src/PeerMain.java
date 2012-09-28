
public class PeerMain {
public static void main(String args[])
{
	if(args.length==2)
	{
		try {
			int gridSize = Integer.parseInt(args[0]);
			int numTreasures = Integer.parseInt(args[1]);
		
			if (gridSize <= 1 || numTreasures <= 0)
				throw new IllegalArgumentException();
			
			Peer peer=new Peer(gridSize,numTreasures);
			peer.start();
		}
		catch (IllegalArgumentException e) {
			System.out.println("Invalid command-line arguments");
			System.out.println("- Size N for N*N grid must be an integer > 1");
			System.out.println("- Number of treasures must be an integer > 0");
			System.exit(0); // exit
		}
	
		
	
	}
	else if(args.length==0)
	{
		new Peer().startPlayer();
	}
	else
	{
		System.out.println("Invalid command-line arguments");
		System.out.println("To host a game give gridsize and numberoftressures as input");
		System.out.println("To joing a game no arguments should be given");
		System.exit(0); // exit
	}
}
}
