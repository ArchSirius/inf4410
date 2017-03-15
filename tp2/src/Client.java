/**
 * @author Samuel Lavoie-Marchildon et Samuel Rondeau
 * @created March 12 2017
 * @Description Application client utilisée pour envoyer un tâche au Load balancer.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.text.DecimalFormat;
import java.util.Properties;

public class Client {

	final static String CONFIG_LB_FILE = "../config/loadBalancer.properties";

	private final LoadBalancerAPI serverStub;
	private final int portRmi;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("You must specify the hostname and the path name");
			System.exit(-1);
		}
		final String distantHostname = args[0];
		final String pathName = args[1];
		final Client client = new Client(distantHostname);
		client.run(pathName);

	}

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

	private LoadBalancerAPI loadServerStub(final String hostname) {
		try {
			return (LoadBalancerAPI) LocateRegistry
					.getRegistry(hostname, 5001)
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

	private int getRmiPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_LB_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portRMI"));
	}

	private String formatNs2MsStr(final long ns) {
		final DecimalFormat df = new DecimalFormat("####.000");
		return df.format(ns / 1000000d).replaceAll(",", ".");
	}
}
