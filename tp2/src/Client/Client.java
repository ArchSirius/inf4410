package Client;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.text.DecimalFormat;
import java.util.Properties;

import LoadBalancer.LoadBalancerAPI;

/**
 * The client program. 
 * It connects to the specified Load Balancer, 
 * and sends a specific operation file to compute (by filename).
 *
 */
public class Client {

	// Configuration files
	public static final String CONFIG_LB_FILE = "../config/loadBalancer.properties";

	// Member variables
	private final LoadBalancerAPI serverStub;
	private final int portRmi;

	/**
	 * Program entry point.
	 * @param args Command-line arguments - must contain a Load Balancer hostname and an operation filename.
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("You must specify the hostname and the operations filename");
			System.exit(-1);
		}
		final String distantHostname = args[0];
		final String pathName = args[1];
		final Client client = new Client(distantHostname);
		client.run(pathName);
	}

	/**
	 * Constructor.
	 * @param hostname LoadBalancer hostname.
	 */
	public Client(final String hostname) {
		int portRmi = 0;
		try {
			portRmi = getRmiPortFromConfig();
		}
		catch (final IOException | NumberFormatException e) {
			System.err.println("Could not read config file: " + e.getMessage());
			System.exit(1);
		}
		this.portRmi = portRmi;
		serverStub = loadServerStub(hostname);
	}

	/**
	 * Connects to the Load Balancer to compute instructions.
	 * @param pathName Operations file name.
	 */
	private void run(final String pathName) {
		try {
			final long startTime, endTime;
			startTime = System.nanoTime();
			int result = serverStub.execute(pathName);
			endTime = System.nanoTime();
			System.out.println("Result for " + pathName + " = " + result);
			System.out.println("Execution time: " + formatNs2MsStr(endTime - startTime) + " ms");
		}
		catch (final RemoteException e) {
			System.err.println("Error executing function on server: " + e.getMessage());
		}
	}

	/**
	 * Loads a LoadBalancer stub.
	 * @param hostname The Load Balancer address.
	 * @return LoadBalancer stub.
	 */
	private LoadBalancerAPI loadServerStub(final String hostname) {
		try {
			return (LoadBalancerAPI) LocateRegistry
					.getRegistry(hostname, portRmi)
					.lookup("server");
		}
		catch (final RemoteException e) {
			System.err.println("[" + hostname + "] Remote exception: " + e.getMessage());
			System.exit(1);
		}
		catch (final NotBoundException e) {
			System.err.println("No binding for server in registry: " + e.getMessage());
			System.exit(1);
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
	 * Converts long nanoseconds to printable String milliseconds
	 * @param ns Nanoseconds to convert.
	 * @return Printable milliseconds.
	 */
	private String formatNs2MsStr(final long ns) {
		final DecimalFormat df = new DecimalFormat("####.000");
		return df.format(ns / 1000000d).replaceAll(",", ".");
	}
}
