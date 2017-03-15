import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;

/**
 * The Load Balancer starts computations on multiple servers. 
 * It scales up and down requests to optimize servers capacity. 
 * In secure mode with N servers, each server computes 1/N of the computations. 
 * In non-secure mode, every servers compute all the computations. 
 * In that case, the Load Balancer ensures that the returned results are "good".
 *
 */
public class LoadBalancer implements LoadBalancerAPI, ServerThreadCallback {

	// Configuration files and operations directory
	final static String CONFIG_LB_FILE      = "../config/loadBalancer.properties";
	final static String CONFIG_SHARED_FILE  = "../config/shared.properties";
	final static String OPERATIONS_DIR_PATH = "../config/operations/";

	// Constants
	private final static int TIMEOUT_MS  = 10000; // 10 seconds
	private final static int MAX_NB_RUNS = 300;

	// Member variables
	private final List<ServerAPI> servers = new ArrayList<>();
	private final int portRmi;
	private final int portLoadBalancer;
	private final boolean isSecure;

	/**
	 * Program entry point.
	 * @param args Command-line arguments (unused).
	 */
	public static void main(String[] args) {
		final LoadBalancer loadBalancer = new LoadBalancer();
		loadBalancer.run();
	}

	/**
	 * Default constructor.
	 */
	public LoadBalancer() {
		int portRmi = 0;
		int portLoadBalancer = 0;
		boolean isSecure = false;
		try {
			portRmi = getRmiPortFromConfig();
			portLoadBalancer = getLbPortFromConfig();
			isSecure = getSecureModeFromConfig();
		}
		catch (final IOException | NumberFormatException e) {
			System.err.println("Could not read config file: " + e.getMessage());
			System.exit(1);
		}
		this.portRmi = portRmi;
		this.portLoadBalancer = portLoadBalancer;
		this.isSecure = isSecure;
		initServerStubs();
	}

	/**
	 * Connects LoadBalancer to RMI.
	 */
	public void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {
			final LoadBalancerAPI stub = (LoadBalancerAPI)
					UnicastRemoteObject.exportObject(this, portLoadBalancer);
			LocateRegistry.getRegistry(portRmi)
					.rebind("server", stub);
			System.out.println("Load balancer ready.");
		}
		catch (final ConnectException e) {
			System.err.println("Could not connect to RMI registry. Is rmiregistry running?");
			System.err.println(e.getMessage());
			System.exit(1);
		}
		catch (final RemoteException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Connects LoadBalancer to servers.
	 */
	private void initServerStubs() {
		String[] hostnames = null;
		try {
			hostnames = getHostnamesFromConfig();
		}
		catch (final IOException e) {
			System.err.println("Error while loading servers configuration: " + e.getMessage());
			System.exit(1);
		}
		if (hostnames == null || hostnames.length == 0) {
			System.err.println("Warning: No servers found. Please verify your loadBalancer.properties file.");
			return;
		}
		for (final String hostname : hostnames) {
			final ServerAPI stub = loadServerStub(hostname);
			if (stub != null) {
				servers.add(stub);
				System.out.println("Connected to " + hostname);
			}
		}
	}

	/**
	 * Loads a Server stub.
	 * @param hostname The Server address.
	 * @return Server stub.
	 */
	private ServerAPI loadServerStub(final String hostname) {
		try {
			return (ServerAPI) LocateRegistry
					.getRegistry(hostname, portRmi)
					.lookup("server");
		}
		catch (final RemoteException e) {
			System.err.println("[" + hostname + "] Remote exception: " + e.getMessage());
		}
		catch (final NotBoundException e) {
			System.err.println("No binding for server in registry: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Extracts RMI port from configuration file.
	 * @return RMI port.
	 * @throws IOException If an error occurred when reading from the input stream.
	 * @throws NumberFormatException If the string does not contain a parsable integer.
	 */
	private int getRmiPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_LB_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portRMI"));
	}

	/**
	 * Extracts LoadBalancer port from configuration file.
	 * @return LoadBalancer port.
	 * @throws IOException If an error occurred when reading from the input stream.
	 * @throws NumberFormatException If the string does not contain a parsable integer.
	 */
	private int getLbPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_LB_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portLoadBalancer"));
	}

	/**
	 * Extracts secure mode from configuration file.
	 * @return Secure mode.
	 * @throws IOException If an error occurred when reading from the input stream.
	 */
	private boolean getSecureModeFromConfig() throws IOException {
		final InputStream input = new FileInputStream(CONFIG_SHARED_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Boolean.parseBoolean(properties.getProperty("securise"));
	}

	/**
	 * Extracts hostnames from configuration file.
	 * @return Hostnames.
	 * @throws IOException If an error occurred when reading from the input stream.
	 */
	private String[] getHostnamesFromConfig() throws IOException {
		final InputStream input = new FileInputStream(CONFIG_LB_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return properties.getProperty("hostnames").split(";");
	}

	/**
	 * Executes instructions from specified file until all have been successfully processed.
	 * @param operationsFilePath The name of the file containing the instructions.
	 * @return The sum of all answers modulo 4000
	 * @throws RemoteException If an exception occurred.
	 */
	@Override
	public int execute(final String operationsFilePath) throws RemoteException {
		// Initialize results lists
		ResultsContainer results = null;
		try {
			results = new ResultsContainer(loadInstructions(operationsFilePath));
		}
		catch (final IOException e) {
			throw new RemoteException("Error loading instructions: " + e.getMessage());
		}
		final ArrayList<Integer> validResults = new ArrayList<>();

		int nbRuns = 0;
		do {
			// Send instructions to servers
			runComputation(results);
			final ResultsContainer invalidEntries = new ResultsContainer();

			// Determine result
			for (final ResultEntry entry : results) {
				try {
					validResults.add(determineResult(entry.getValue()));
				}
				catch (final Exception e) {
					invalidEntries.add(entry.getKey());
				}
			}

			// Retry invalid entries
			results = invalidEntries;
		}
		while (!results.isEmpty() && nbRuns++ < MAX_NB_RUNS);
		if (nbRuns >= MAX_NB_RUNS) {
			throw new RemoteException("Execution loop killed.");
		}
		return computeResult(validResults);
	}

	/**
	 * Runs a computation on servers and get results. 
	 * It starts a new thread for every active server and awaits completion.
	 * @param container The ResultsContainer containing instructions and results.
	 */
	private void runComputation(final ResultsContainer container) {
		// Create and start threads (one thread per server)
		final ArrayList<ServerThread> serverThreads = new ArrayList<>();
		final ArrayList<ResultsContainer> jobs = determineJobs(container);
		int i = 0;
		for (final ServerAPI server : servers) {
			final ServerThread serverThread = new ServerThread(server, jobs.get(i++), this);
			serverThreads.add(serverThread);
			serverThread.start();
		}

		// Wait for all threads to finish
		for (final ServerThread serverThread : serverThreads) {
			try {
				serverThread.join(TIMEOUT_MS);
			}
			catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

		// Save results
		syncContainer(container, jobs);
	}

	/**
	 * Callback on server failure.
	 * @param server The server that failed.
	 */
	@Override
	public void onFailure(ServerAPI server) {
		System.err.println("Connection lost - unregistering server " + server);
		unregisterServer(server);
	}

	/**
	 * Computes the sum module 4000 of multiple results.
	 * @param results All the individual results to sum modulo 4000.
	 * @return The resulting sum modulo 4000.
	 */
	private int computeResult(final ArrayList<Integer> results) {
		int total = 0;
		for (final Integer result : results) {
			total = (total + result) % 4000;
		}
		return total;
	}

	/**
	 * Extracts the instructions from instructions file.
	 * @param operationsFile Name of the instructions file.
	 * @return The ArrayList of operations.
	 * @throws IOException If an error occurred when reading from the input stream.
	 */
	private ArrayList<String> loadInstructions(final String operationsFile) throws IOException {
		ArrayList<String> instructions = new ArrayList<>();
		instructions = new ArrayList<>(Files.readAllLines(Paths.get(OPERATIONS_DIR_PATH + operationsFile)));
		return instructions;
	}

	/**
	 * Tries to determine the "good" result amongst multiple results.
	 * @param values An ArrayList of values to compare.
	 * @return The "good" result, which is the most present value in non-secure mode.
	 * @throws Exception If a "good" value cannot be determined.
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

	/**
	 * Removes a server from memory.
	 * @param server The server to remove.
	 * @return true if the list contained the specified server.
	 */
	private boolean unregisterServer(final ServerAPI server) {
		return servers.remove(server);
	}

	/**
	 * Determines individual job list for each active server. 
	 * In secure mode with N servers, each server computes 1/N of the tasks. 
	 * In non-secure mode, every servers compute the whole task list.
	 * @param container The main ResultsContainer to split, if necessary.
	 * @return An ArrayList of individual jobs for each server.
	 */
	private ArrayList<ResultsContainer> determineJobs(final ResultsContainer container) {
		final int nbServers = servers.size();
		final ArrayList<ResultsContainer> jobs = new ArrayList<>();
		if (isSecure) {
			final int subContainerSize = container.size() / nbServers;
			for (int i = 0; i < nbServers; ++i) {
				final int fromIndex = i * subContainerSize;
				final int toIndex = i == nbServers - 1 ? container.size() : (i + 1) * subContainerSize;
				jobs.add(container.subList(fromIndex, toIndex));
			}
		}
		else {
			for (int i = 0; i < servers.size(); ++i) {
				jobs.add(container);
			}
		}
		return jobs;
	}

	/**
	 * Ensures that the main container contains all results from every servers.
	 * @param mainContainer The main ResultsContainer.
	 * @param subContainers The ArrayList of individually cumputed sub-containers.
	 */
	private void syncContainer(final ResultsContainer mainContainer, final ArrayList<ResultsContainer> subContainers) {
		if (!isSecure) {
			return;
		}
		int i = 0;
		for (final ResultsContainer subContainer : subContainers) {
			for (final ResultEntry subEntry : subContainer) {
				try {
					mainContainer.get(i++).setValue(subEntry.getValue());
				}
				catch (final IndexOutOfBoundsException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
