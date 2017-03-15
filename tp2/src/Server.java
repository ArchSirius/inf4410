/**
 * @author Samuel Rondeau et Samuel Lavoie-Marchildon
 * @created March 12 2017
 * @Description Application serveur utilisée pour effectuer un calcul.
 */

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

public class Server implements ServerAPI {

	final static String CONFIG_SERVER_FILE = "../config/server.properties";
	final static String CONFIG_SHARED_FILE = LoadBalancer.CONFIG_SHARED_FILE;

	private final int portRmi;
	private final int portServer;
	private final int capacity;
	private int falseResultRate;
	private final Random random;

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

	private int getRmiPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_SERVER_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portRMI"));
	}

	private int getServerPortFromConfig() throws IOException, NumberFormatException {
		final InputStream input = new FileInputStream(CONFIG_SERVER_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Integer.parseInt(properties.getProperty("portServer"));
	}

	private boolean getSecureModeFromConfig() throws IOException {
		final InputStream input = new FileInputStream(CONFIG_SHARED_FILE);
		final Properties properties = new Properties();
		properties.load(input);
		input.close();
		return Boolean.parseBoolean(properties.getProperty("securise"));
	}

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

	@Override
	public ArrayList<Integer> doOperations(ArrayList<String> instructions) throws RemoteException {
		if (!accept(instructions)) {
			throw new RemoteException("Too many operations");
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
	 * Calcul la valeur de pell
	 * @param operand Nombre sur lequel la valeur de pell est calculé
	 * @return Le nombre de pell ou une valeur aléatoire
	 * @throws RemoteException
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
	 * Calcule le prochain nombre premier
	 * @param operand Nombre sur lequel prime est calculé
	 * @return Prime ou une valeur aléatoire
	 * @throws RemoteException
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
	 * Retourne la capacité du serveur
	 * @return
	 * @throws RemoteException
	 */
	@Override
	public int getCapacity() throws RemoteException {
		return capacity;
	}

	/**
	 * Détermine si le réseau doit renvoyer une erreur
	 * @return
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
	 * Génère un nombre aléatoire entre 0 (inclusivement) et 4000 (exclusivement)
	 * @return
	 */
	private int generateRandom4k() {
		return random.nextInt(4000);
	}

	private boolean accept(final ArrayList<String> instructions) {
		final double rejectionRate = 0.2d * ((double) instructions.size() / capacity - 1.0d);
		if (rejectionRate <= 0 || random.nextDouble() < rejectionRate) {
			return true;
		}
		return false;
	}

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
