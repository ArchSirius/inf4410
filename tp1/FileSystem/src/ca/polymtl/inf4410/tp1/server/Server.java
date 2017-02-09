package ca.polymtl.inf4410.tp1.server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.UUID;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class Server implements ServerInterface {

	private final HashMap<String, UUID> lock;

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public Server() {
		super();
		lock = new HashMap<String, UUID>();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lance ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}

	/*
	 * Generates a unique identifier (UUID) for a client.
	 * 
	 * @throws RemoteException RMI exception
	 * @return unique identifier
	 */
	@Override
	public UUID generateclientid() throws RemoteException {
		return UUID.randomUUID();
	}

	/*
	 * Creates a new empty file if a file with specified filename does not exist.
	 * 
	 * @throws RemoteException RMI exception
	 * @param  filename name of the new file
	 * @return          true if creation was successful, otherwise false
	 */
	@Override
	public Boolean create(String filename) throws RemoteException {
		final File file = new File("./" + filename);
		try {
			return file.createNewFile();
		}
		catch (final IOException e) {
			return false;
		}
	}

	/*
	 * Returns a list of files with respective owners, if any.
	 * 
	 * @throws RemoteException RMI exception
	 * @return Hashmap of filenames and respective owners (nullable)
	 */
	@Override
	public HashMap<String, UUID> list() throws RemoteException {
		final File directory = new File(".");
		final String[] filenames = directory.list();
		final HashMap<String, UUID> list = new HashMap<String, UUID>();
		for (final String filename : filenames) {
			list.put(filename, lock.get(filename));
		}
		return list;
	}

	/*
	 * Sync the client directory with current files.
	 * 
	 * @throws RemoteException RMI exception
	 * @return Hashmap of filenames and files content
	 */
	@Override
	public HashMap<String, byte[]> syncLocalDir() throws RemoteException {
		final File directory = new File(".");
		final File[] files = directory.listFiles();
		final HashMap<String, byte[]> list = new HashMap<String, byte[]>();
		for (final File file : files) {
			try {
				list.put(file.getName(), Files.readAllBytes(file.toPath()));
			}
			catch (final IOException e) {
			}
		}
		return list;
	}

	/*
	 * Returns specified file if checksum differs.
	 * 
	 * @throws RemoteException RMI exception
	 * @param  filename name of the file to fetch
	 * @param  checksum checksum of the client's version of the file
	 * @return          file if checksum differs, otherwise null
	 */
	@Override
	public byte[] get(String filename, byte[] checksum) throws RemoteException {
		final File file = new File("./" + filename);

		// If file does dot exist, throw exception
		if (!file.exists()) {
			throw new RemoteException("File \"" + filename + "\" does not exist.");
		}

		// Read file content
		final byte[] data;
		try {
			data = Files.readAllBytes(file.toPath());
		}
		catch (final IOException e) {
			throw new RemoteException(e.getMessage());
		}

		try {
			// Compute checksum
			final MessageDigest md = MessageDigest.getInstance("MD5");
			// If checksum does not differ, do not send file
			if (md.digest(data).equals(checksum)) {
				return null;
			}
			// If checksum differs, send file
			return data;
		}
		catch (final NoSuchAlgorithmException e) {
		}
		return null;
	}

	/*
	 * Locks a file from editing except from owner.
	 * 
	 * @throws RemoteException RMI exception
	 * @param  filename name of the file to fetch
	 * @param  clientid client unique identifier
	 * @param  checksum checksum of the client's version of the file
	 * @return          Tuple containing file if checksum differs and client has write access, otherwise null,
	 *                  and unique identifier of owner
	 */
	@Override
	public Entry<byte[], UUID> lock(String filename, UUID clientid, byte[] checksum) throws RemoteException {
		final File file = new File("./" + filename);

		// If file does dot exist, throw exception
		if (!file.exists()) {
			throw new RemoteException("File \"" + filename + "\" does not exist.");
		}

		if (lock.containsKey(filename)) {
			final UUID owner = lock.get(filename);
			// If file is locked by someone else
			if (!owner.equals(clientid)) {
				return new SimpleEntry<byte[], UUID>(null, owner);
			}
		}
		// Lock the file
		else {
			lock.put(filename, clientid);
		}

		// Read file content
		final byte[] data;
		try {
			data = Files.readAllBytes(file.toPath());
		}
		catch (final IOException e) {
			throw new RemoteException(e.getMessage());
		}

		try {
			// Compute checksum
			final MessageDigest md = MessageDigest.getInstance("MD5");
			// If checksum does not differ, do not send file
			if (md.digest(data).equals(checksum)) {
				return new SimpleEntry<byte[], UUID>(null, clientid);
			}
			// If checksum differs, send file
			return new SimpleEntry<byte[], UUID>(data, clientid);
		}
		catch (final NoSuchAlgorithmException e) {
		}
		return null;
	}

	/*
	 * Writes content to a file if it exists and client is owner.
	 * 
	 * @throws RemoteException RMI exception
	 * @param  filename name of the file to overwrite
	 * @param  data     file content
	 * @param  clientid client unique identifier
	 * @return          true if write was successful, otherwise false
	 */
	@Override
	public Boolean push(String filename, byte[] data, UUID clientid) throws RemoteException {
		final File file = new File("./" + filename);

		// If file does dot exist, throw exception
		if (!file.exists()) {
			throw new RemoteException("File \"" + filename + "\" does not exist.");
		}

		// If the client does not own the file, reject
		if (!(lock.containsKey(filename) && lock.get(filename).equals(clientid))) {
			return false;
		}

		// Write content to file
		try {
			Files.write(file.toPath(), data, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (final IOException e) {
			throw new RemoteException(e.getMessage());
		}

		// Remove lock
		lock.remove(filename);
		return true;
	}
}
