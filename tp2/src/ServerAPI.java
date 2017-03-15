import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * This is the Server API.
 *
 */
public interface ServerAPI extends Remote {

	// Supported operations
	enum Operation {
		PELL,
		PRIME
	};

	/**
	 * Execute specified operation with single operand if it is supported.
	 * @param operation The operation to execute.
	 * @param operand The operand to compute.
	 * @return Result.
	 * @throws RemoteException If an exception occurred.
	 */
	default int doOperation(Operation operation, int operand) throws RemoteException {
		switch (operation) {
			case PELL:
				return pell(operand);
			case PRIME:
				return prime(operand);
			default:
				throw new RemoteException("Unsupported operation: \"" + operation + "\"");
		}
	};

	/**
	 * Computes an ArrayList of instructions and return corresponding results. 
	 * Some results can be false based on specified rate. 
	 * Some instructions can be rejected based on capacity and task volume.
	 * @param instructions The instructions to compute.
	 * @return Results.
	 * @throws RejectedException If the instructions are rejected.
	 * @throws RemoteException If an exception occurred.
	 */
	ArrayList<Integer> doOperations(ArrayList<String> instructions) throws RejectedException, RemoteException;

	/**
	 * Computes the Pell number of specified operand.
	 * @param operand The operand used to compute the Pell number.
	 * @return The Pell number of specified operand.
	 * @throws RemoteException If an exception occurred.
	 */
	int pell(int operand) throws RemoteException;

	/**
	 * Computes the next prime number after specified operand.
	 * @param operand The operand used to compute the next prime number.
	 * @return The next prime number after specified operand.
	 * @throws RemoteException If an exception occurred.
	 */
	int prime(int operand) throws RemoteException;

	/**
	 * Returns server capacity.
	 * @return Server capacity.
	 * @throws RemoteException If an exception occurred.
	 */
	int getCapacity() throws RemoteException;
}
