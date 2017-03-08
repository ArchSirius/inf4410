import java.io.File;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class LoadBalancer implements LoadBalancerAPI {

	final static String CONFIG_SERVERS_FILE = "../config/servers.txt";

	public static void main(String[] args) {
		final LoadBalancer loadBalancer = new LoadBalancer();
		loadBalancer.run();
	}

	public LoadBalancer() {
		
	}

	public void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {
			final LoadBalancerAPI stub = (LoadBalancerAPI) UnicastRemoteObject.exportObject(this, 0);
			final Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Load balancer ready.");
		}
		catch (final ConnectException e) {
			System.err.println("Could not connect to RMI registry. Is rmiregistry running?");
			System.err.println();
			System.err.println("Error: " + e.getMessage());
		}
		catch (final Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	@Override
	public int execute(String path) throws RemoteException {
		return 0;
		// Load operations
		// For each instruction
			// Loop server instances
				// Execute operation
				// If timeout, retry operation
				// Store result
			// Determine result
			// If invalid, retry instruction
			// Update result
		// Send result
	}

	private ArrayList<String> loadInstructions(final String path) {
		return null;
	}

	private int determineResult(ArrayList<Integer> values) throws Exception {
		return 0;
	}
}
