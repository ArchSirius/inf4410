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
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;

public class LoadBalancer implements LoadBalancerAPI, ServerThreadCallback {

	final static String CONFIG_LB_FILE      = "../config/loadBalancer.properties";
	final static String CONFIG_SHARED_FILE  = "../config/shared.properties";
	final static String OPERATIONS_DIR_PATH = "../config/operations/";

	final static int TIMEOUT_MS = 10000; // 10 seconds

	final List<ServerAPI> servers = new ArrayList<>();
	final int portRmi;
	final int portLoadBalancer;
	final boolean isSecure;

	public static void main(String[] args) {
		final LoadBalancer loadBalancer = new LoadBalancer();
		loadBalancer.run();
	}

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
	 * Load a server stub
	 * @param hostname The address of the server
	 * @return
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

	private int getRmiPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_LB_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portRMI"));
	}

	private int getLbPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_LB_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portLoadBalancer"));
	}

	/**
	 * Specify the number of instance to send the job specified inside the properties file.
	 * @param shared Input stream used to gather the shared properties
	 * @throws IOException
	 */
	private boolean getSecureModeFromConfig() throws IOException {
		final InputStream input = new FileInputStream(CONFIG_SHARED_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Boolean.parseBoolean(properties.getProperty("securise"));
	}

	private String[] getHostnamesFromConfig() throws IOException {
		final InputStream input = new FileInputStream(CONFIG_LB_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return properties.getProperty("hostnames").split(";");
	}

	/**
	 * Execute the operations specified in the file
	 * @param operationsFilePath Name of the operation file.
	 * @return The result from the servers
	 * @throws RemoteException
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
		while (!results.isEmpty());
		return computeResult(validResults);
	}

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

	@Override
	public void onFailure(ServerAPI server) {
		unregisterServer(server);
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
	private ArrayList<String> loadInstructions(final String operationsFile) throws IOException {
		ArrayList<String> instructions = new ArrayList<>();
		instructions = new ArrayList<>(
				Files.readAllLines(Paths.get(OPERATIONS_DIR_PATH + operationsFile))
		);
		return instructions;
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

	private void unregisterServer(final ServerAPI server) {
		servers.remove(server);
	}

	private ArrayList<ResultsContainer> determineJobs(final ResultsContainer container) {
		final int nbServers = servers.size();
		final ArrayList<ResultsContainer> jobs = new ArrayList<>();
		if (isSecure) {
			final int subContainerSize = container.size() / nbServers;
			for (int i = 0; i < nbServers; ++i) {
				final int fromIndex = i * nbServers;
				final int toIndex = i == nbServers - 1 ? container.size() : i * subContainerSize;
				jobs.add(container.splice(fromIndex, toIndex));
			}
		}
		else {
			for (int i = 0; i < servers.size(); ++i) {
				jobs.add(container);
			}
		}
		return jobs;
	}

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
