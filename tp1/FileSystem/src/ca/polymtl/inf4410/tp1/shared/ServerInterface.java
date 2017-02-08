package ca.polymtl.inf4410.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

public interface ServerInterface extends Remote {
	UUID generateclientid() throws RemoteException;
	Boolean create(String filename) throws RemoteException;
	HashMap<String, UUID> list() throws RemoteException;
	HashMap<String, byte[]> syncLocalDir() throws RemoteException;
	byte[] get(String filename, byte[] checksum) throws RemoteException;
	Entry<byte[], UUID> lock(String filename, UUID clientid, String checksum) throws RemoteException;
	Boolean push(String filename, byte[] data, UUID clientid) throws RemoteException;
}
