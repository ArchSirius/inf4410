/**
 * @author Samuel Lavoie-Marchildon et Samuel Rondeau
 * @created March 12 2017
 * @Description Application LoadBalancer utilisée pour distribuer les charges aux serveurs
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;

public class LoadBalancer implements LoadBalancerAPI {

	final static String CONFIG_LB_FILE = "../config/loadBalancer.properties";
	final static String CONFIG_SHARED_FILE = "../config/shared.properties";
	final static String OPERATIONS_PATH = "../config/operations/";

	final static int TIMEOUT_MS = 10000; // 10 seconds
	final static int SUCCESS_BLOCK_INCREMENT = 5;

	final ArrayList<ServerAPI> servers;
	int portRmi;
	int portLoadBalancer;
	boolean isSecure = false;

	public static void main(String[] args) {
		final LoadBalancer loadBalancer = new LoadBalancer();
		loadBalancer.run();
	}

	public LoadBalancer() {
		servers = new ArrayList<>();
		initialise();
	}

	/**
	 * Load the servers specified inside the properties file.
	 * @param input Input stream used to gather properties
	 * @throws IOException
	 */
	private void loadServersStub(final InputStream input) throws IOException {
		final Properties properties = new Properties();
		properties.load(input);
		final String[] hostnames = properties.getProperty("hostnames").split(";");
		for (final String hostname : hostnames) {
			final ServerAPI stub = loadServerStub(hostname);
			if (stub != null) {
				servers.add(loadServerStub(hostname));
			}
		}
	}

	/**
	 * Gather the ports specified inside the properties file.
	 * @param input Input stream used to gather properties
	 * @throws IOException
	 */
	private void initPorts(final InputStream input) throws IOException {
		final Properties properties = new Properties();
		properties.load(input);
		String port = properties.getProperty("portRMI");
		portRmi = Integer.parseInt(port);
		port = properties.getProperty("portLoadBalancer");
		portLoadBalancer = Integer.parseInt(port);
	}

	/**
	 * Specify the number of instance to send the job specified inside the properties file.
	 * @param shared Input stream used to gather the shared properties
	 * @param lb Input stream used to gather the load balancer properties
	 * @throws IOException
	 */
	private void initNbInstance(final InputStream shared, final InputStream lb) throws IOException {
		Properties properties = new Properties();
		properties.load(shared);
		isSecure = Boolean.parseBoolean(properties.getProperty("securise"));

		properties = new Properties();
		properties.load(lb);
	}

	/**
	 * Initialise the load balancer using the params specified in the config files
	 */
	private void initialise() {
		InputStream input = null;
		InputStream lbInput = null;
		try {
			// load server properties file
			input = new FileInputStream(CONFIG_LB_FILE);
			initPorts(input);
			input = new FileInputStream(CONFIG_LB_FILE);
			loadServersStub(input);

			// load shared properties file
			input = new FileInputStream(CONFIG_SHARED_FILE);
			lbInput = new FileInputStream(CONFIG_LB_FILE);
			initNbInstance(input, lbInput);
		}
		catch (final IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		finally {
			if (input != null) {
				try {
					input.close();
					lbInput.close();
				}
				catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Load a server stub
	 * @param hostname The address of the server
	 * @return
	 */
	private ServerAPI loadServerStub(final String hostname) {
		System.out.println("Connecting to " + hostname);
		ServerAPI stub = null;
		try {
			Registry registry = LocateRegistry.getRegistry(hostname, portRmi);
			stub = (ServerAPI) registry.lookup("server");
			System.out.println(stub.toString());
			return stub;
		}
		catch (final RemoteException e) {
			System.err.println("Unknown remote exception: " + e.getMessage());
		}
		catch (final NotBoundException e) {
			System.err.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas defini dans le registre.");
		}
		catch (final Exception e) {
		  e.printStackTrace();
		}
		return stub;
	}

	public void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {
			final LoadBalancerAPI stub = (LoadBalancerAPI) UnicastRemoteObject.exportObject(this, portLoadBalancer);
			final Registry registry = LocateRegistry.getRegistry(portRmi);
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

	/**
	 * Execute the operations specified in the file
	 * @param operationsFile Name of the operation file.
	 * @return The result from the servers
	 * @throws RemoteException
	 */
	@Override
	public int execute(String operationsFile) throws RemoteException {
		// Initialize results lists
		ArrayList<Map.Entry<String, ArrayList<Integer>>> results = new ArrayList<>();
		for (final String instruction : loadInstructions(operationsFile)) {
			results.add(createEntry(instruction));
		}
		final ArrayList<Integer> validResults = new ArrayList<>();
		do {
			// Send instructions to servers
			runComputation(results);
			final ArrayList<Map.Entry<String, ArrayList<Integer>>> invalidEntries = new ArrayList<>();

			// Determine result
			for (final Map.Entry<String, ArrayList<Integer>> entry : results) {
				try {
					validResults.add(determineResult(entry.getValue()));
				}
				catch (final Exception e) {
					invalidEntries.add(createEntry(entry.getKey()));
				}
			}

			// Retry invalid entries
			results = invalidEntries;
		}
		while (!results.isEmpty());
		return computeResult(validResults);
	}

	private void runComputation(final ArrayList<Map.Entry<String, ArrayList<Integer>>> results) {
		// Create and start threads (one thread per server)
		final ArrayList<Thread> serverThreads = new ArrayList<>();
		for (final ServerAPI server : servers) {
			final Thread serverThread = new Thread() {
				// Server execution
				public void run() {
					int successProcessedBlocks = 0;
					int blockSize = 1;
					try {
						blockSize = server.getCapacity();
					}
					catch (final RemoteException e) {
						e.printStackTrace();
						unregisterServer(server);
						return;
					}
					try {
						int head = 0;
						// Send all instructions
						while (head < results.size()) {
							// Build task block
							final ArrayList<String> taskBlock = new ArrayList<>();
							int offset;
							for (offset = 0; offset < blockSize && head < results.size(); ++offset) {
								taskBlock.add(results.get(offset).getKey());
								++head;
							}
							// Send task block and save result
							final ArrayList<Integer> resultBlock = server.doOperations(taskBlock);
							for (final Integer result : resultBlock) {
								results.get(head + offset).getValue().add(result);
							}

							// Update head
							head += offset;
							++successProcessedBlocks;
							// Increment block size every SUCCESS_BLOCK_INCREMENT successful blocks
							if (successProcessedBlocks % SUCCESS_BLOCK_INCREMENT == 0) {
								++blockSize;
							}
						}
					}
					catch (final RemoteException e) {
						// Decrement block size
						successProcessedBlocks = 0;
						if (blockSize > 1) {
							--blockSize;
						}
					}
				}
			};
			serverThreads.add(serverThread);
			serverThread.start();
		}

		// Wait for all threads to finish
		for (final Thread serverThread : serverThreads) {
			try {
				serverThread.join(TIMEOUT_MS);
			}
			catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add all the results from the servers in order to produce the return value
	 * @param results
	 * @return the final result to return to the client
	 */
	private int computeResult(final ArrayList<Integer> results) {
	   int total = 0;
	   for (final Integer result : results) {
		   total = (total + result) % 4000;
	   }
	   return total;
	}

	/**
	 * Load the instructions from the instruction file.
	 * @param operationsFile Name of the operations file
	 * @return Return A list of operation
	 */
	private List<String> loadInstructions(final String operationsFile) {
		try {
			return Files.readAllLines(Paths.get(OPERATIONS_PATH + operationsFile));
		}
		catch (final IOException e) {
			System.err.println("Error: " + e.getMessage());
		}
		return null;        // TODO Handle exception
	}

	/**
	 * Determine what is the good result from the servers if they return differents values.
	 * @param values A list of results from N servers
	 * @return The good result
	 * @throws Exception
	 */
	private int determineResult(final ArrayList<Integer> values) throws Exception {
		if (isSecure) {
			return values.get(0);
		}
		Collections.sort(values);
		if (values.get(0).equals(values.get(values.size() / 2))) {
			return values.get(0);
		}
		else if (values.get(values.size() - 1).equals(values.get(values.size() / 2))) {
			return values.get(values.size() - 1);
		}
		throw new Exception("Could not determine result with values " + values);
	}

	private Map.Entry<String, ArrayList<Integer>> createEntry(final String instruction) {
		return new AbstractMap.SimpleEntry<>(instruction, new ArrayList<>());
	}

	private void unregisterServer(final ServerAPI server) {
		servers.remove(server);
	}
}
