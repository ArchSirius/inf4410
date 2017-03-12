import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ServerAPI extends Remote {

	enum Operation {
		PELL,
		PRIME
	};

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
	ArrayList<Integer> doOperations(ArrayList<String> instructions) throws RemoteException;
	int pell(int operand) throws RemoteException;
	int prime(int operand) throws RemoteException;

	int getCapacity() throws RemoteException;
}
