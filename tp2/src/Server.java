/**
 * @author Samuel Rondeau et Samuel Lavoie-Marchildon
 * @created March 12 2017
 * @Description Application serveur utilisée pour effectuer un calcul.
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

public class Server implements ServerAPI {

	final static String CONFIG_SERVER_FILE = "../config/server.properties";

	final int CAPACITY;
	final int FALSE_RESULT_RATE;
	final int PORT_RMI;
	final int PORT_SERVER;
	final Random random;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Too few arguments");
			System.exit(1);
		}
		int capacity = 0;
		int falseResultRate = 0;
		int portRmi = 0;
		int portServer = 0;
		try {
			capacity = Integer.parseInt(args[0]);
			falseResultRate = Integer.parseInt(args[1]);

			FileInputStream input = new FileInputStream(LoadBalancer.CONFIG_SHARED_FILE);
			Properties prop = new Properties();
			prop.load(input);
			boolean securise = Boolean.parseBoolean(prop.getProperty("securise"));
			if (securise) {
				falseResultRate = 0;
			}
			input = new FileInputStream(CONFIG_SERVER_FILE);
			prop = new Properties();
			prop.load(input);
			String parsedPort = prop.getProperty("portRMI");
			portRmi = Integer.parseInt(parsedPort);
			parsedPort = prop.getProperty("portServer");
			portServer = Integer.parseInt(parsedPort);
		}
		catch (final NumberFormatException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		final Server server = new Server(capacity, falseResultRate, portRmi, portServer);
		server.run();
	}

	public Server(final int capacity, final int falseResultRate, final int portRmi, final int portServer) {
		if (capacity < 0) {
			System.err.println("Invalid capacity");
			System.exit(1);
		}
		if (falseResultRate < 0 || falseResultRate > 100) {
			System.err.println("Invalid error rate");
			System.exit(1);
		}
		CAPACITY = capacity;
		FALSE_RESULT_RATE = falseResultRate;
		PORT_RMI = portRmi;
		PORT_SERVER = portServer;
		random = new Random(System.nanoTime());
	};

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		try {
			final ServerAPI stub = (ServerAPI) UnicastRemoteObject.exportObject(this, PORT_SERVER);
			final Registry registry = LocateRegistry.getRegistry(PORT_RMI);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		}
		catch (final ConnectException e) {
			System.err.println("Could not connect to RMI registry. Is rmiregistry running?");
			System.err.println();
			System.err.println("Error: " + e.getMessage());
		}
		catch (final Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	};

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
		return CAPACITY;
	}

	/**
	 * Détermine si le réseau doit renvoyer une erreur
	 * @return
	 */
	private boolean isError() {
		if (FALSE_RESULT_RATE == 0) {
			return false;
		}
		if (FALSE_RESULT_RATE == 100) {
			return true;
		}
		return random.nextInt(100) < FALSE_RESULT_RATE;
	}

	/**
	 * Génère un nombre aléatoire entre 0 (inclusivement) et 4000 (exclusivement)
	 * @return
	 */
	private int generateRandom4k() {
		return random.nextInt(4000);
	}

	private boolean accept(final ArrayList<String> instructions) {
		final double rejectionRate = 0.2d * ((double) instructions.size() / CAPACITY - 1.0d);
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
