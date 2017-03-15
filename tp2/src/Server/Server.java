package Server;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import LoadBalancer.LoadBalancer;

/**
 * The server computes tasks. 
 * In non-secure mode, it can send false results at specified rate. 
 * It will reject task lists that are too big on a certain rate based on its capacity.
 *
 */
public class Server implements ServerAPI {

	// Configuration files
	public static final String CONFIG_SERVER_FILE = "../config/server.properties";
	public static final String CONFIG_SHARED_FILE = LoadBalancer.CONFIG_SHARED_FILE;

	// Member variables
	private final int portRmi;
	private final int portServer;
	private final int capacity;
	private int falseResultRate;
	private final Random random;

	/**
	 * Program entry point.
	 * @param args Command-line arguments - must contain a capacity and a false result rate.
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Too few arguments.");
			System.exit(1);
		}
		int capacity = 0;
		int falseResultRate = 0;
		try {
			capacity = Integer.parseInt(args[0]);
			falseResultRate = Integer.parseInt(args[1]);
		}
		catch (final NumberFormatException e) {
			e.printStackTrace();
			System.exit(1);
		}
		final Server server = new Server(capacity, falseResultRate);
		server.run();
	}

	/**
	 * Constructor.
	 * @param capacity The capacity of this server.
	 * @param falseResultRate The rate of false results in non-secure mode.
	 */
	public Server(final int capacity, final int falseResultRate) {
		if (capacity < 1) {
			System.out.println("Invalid capacity. Assuming 1.");
			this.capacity = 1;
		}
		else {
			this.capacity = capacity;
		}
		int portRmi = 0;
		int portServer = 0;
		boolean isSecure = false;
		try {
			portRmi = getRmiPortFromConfig();
			portServer = getServerPortFromConfig();
			isSecure = getSecureModeFromConfig();
		}
		catch (final IOException e) {
			System.err.println("Could not read config file: " + e.getMessage());
			System.exit(1);
		}
		setFalseRate(falseResultRate, isSecure);
		this.portRmi = portRmi;
		this.portServer = portServer;
		random = new Random(System.nanoTime());
	};

	/**
	 * Connects Server to RMI.
	 */
	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {
			final ServerAPI stub = (ServerAPI)
					UnicastRemoteObject.exportObject(this, portServer);
			LocateRegistry.getRegistry(portRmi)
					.rebind("server", stub);
			System.out.println("Server ready.");
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
	};

	/**
	 * Extracts RMI port from configuration file.
	 * @return RMI port.
	 * @throws IOException If an error occurred when reading from the input stream.
	 * @throws NumberFormatException If the string does not contain a parsable integer.
	 */
	private int getRmiPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_SERVER_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portRMI"));
	}

	/**
	 * Extracts Server port from configuration file.
	 * @return Server port.
	 * @throws IOException If an error occurred when reading from the input stream.
	 * @throws NumberFormatException If the string does not contain a parsable integer.
	 */
	private int getServerPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_SERVER_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portServer"));
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
	 * Sets the false result rate.
	 * @param falseResultRate The new false result rate.
	 * @param isSecure Whether or not secure mode is enabled.
	 */
	private void setFalseRate(final int falseResultRate, final boolean isSecure) {
		if (!isSecure && (falseResultRate < 0 || falseResultRate > 100)) {
			System.out.println("Invalid error rate. Assuming 0.");
			this.falseResultRate = 0;
		}
		else if (isSecure && falseResultRate != 0) {
			System.out.println("Safe mode enabled. Forcing false result rate to 0.");
			this.falseResultRate = 0;
		}
		else {
			this.falseResultRate = falseResultRate;
		}
	}

	/**
	 * Computes an ArrayList of instructions and return corresponding results. 
	 * Some results can be false based on specified rate. 
	 * Some instructions can be rejected based on capacity and task volume.
	 * @param instructions The instructions to compute.
	 * @return Results.
	 * @throws RejectedException If the instructions are rejected.
	 * @throws RemoteException If an exception occurred.
	 */
	@Override
	public ArrayList<Integer> doOperations(ArrayList<String> instructions) throws RejectedException, RemoteException {
		if (!accept(instructions)) {
			throw new RejectedException("Too many operations");
		}
		final ArrayList<Integer> results = new ArrayList<>();
		for (final String instruction : instructions) {
			final String[] elements = instruction.split(" ");
			if (elements.length < 1) {
				throw new RemoteException("Invalid instruction " + instruction);
			}
			final String operation = elements[0];
			final String[] operands = Arrays.copyOfRange(elements, 1, elements.length);
			if (operands.length < 1) {
				throw new RemoteException("Too few arguments");
			}
			// From this point on, assume only one argument is used and no other operations exist
			final int operand;
			try {
				operand = Integer.parseInt(operands[0]);
			}
			catch (final NumberFormatException e) {
				throw new RemoteException(e.getMessage());
			}
			results.add(doOperation(getOperation(operation), operand));
		}
		return results;
	}

	/**
	 * Computes the Pell number of specified operand.
	 * @param operand The operand used to compute the Pell number.
	 * @return The Pell number of specified operand.
	 * @throws RemoteException If an exception occurred.
	 */
	@Override
	public int pell(final int operand) throws RemoteException {
		if (isError()) {
			return generateRandom4k();
		}
		// Using non-optimal implementation
		return Operations.pell(operand);
	}

	/**
	 * Computes the next prime number after specified operand.
	 * @param operand The operand used to compute the next prime number.
	 * @return The next prime number after specified operand.
	 * @throws RemoteException If an exception occurred.
	 */
	@Override
	public int prime(final int operand) throws RemoteException {
		if (isError()) {
			return generateRandom4k();
		}
		// Using non-optimal implementation
		return Operations.prime(operand);
	}

	/**
	 * Returns server capacity.
	 * @return Server capacity.
	 * @throws RemoteException If an exception occurred.
	 */
	@Override
	public int getCapacity() throws RemoteException {
		return capacity;
	}

	/**
	 * Determines if next operation should return a false result based on specified false result rate.
	 * @return true if next operation should return a false result.
	 */
	private boolean isError() {
		if (falseResultRate == 0) {
			return false;
		}
		if (falseResultRate == 100) {
			return true;
		}
		return random.nextInt(100) < falseResultRate;
	}

	/**
	 * Generates a random number between 0 (inclusive) and 4000 (exclusive).
	 * @return A random number between 0 (inclusive) and 4000 (exclusive).
	 */
	private int generateRandom4k() {
		return random.nextInt(4000);
	}

	/**
	 * Determines whether the instructions are accepted or not.
	 * @param instructions The instructions to compute.
	 * @return true if the instructions are accepted.
	 */
	private boolean accept(final ArrayList<String> instructions) {
		final double rejectionRate = 0.2d * ((double) instructions.size() / capacity - 1.0d);
		if (rejectionRate <= 0 || random.nextDouble() < rejectionRate) {
			return true;
		}
		return false;
	}

	/**
	 * Converts a String to a safe ServerAPI.Operation.
	 * @param operation The input String to convert.
	 * @return The corresponding ServerAPI.Operation.
	 * @throws RemoteException If the input String cannot be converted or if an exception occurred.
	 */
	private Operation getOperation(final String operation) throws RemoteException {
		switch(operation) {
			case "pell":
				return Operation.PELL;
			case "prime":
				return Operation.PRIME;
			default:
				throw new RemoteException("Unsupported operation: \"" + operation + "\"");
		}
	}
}
