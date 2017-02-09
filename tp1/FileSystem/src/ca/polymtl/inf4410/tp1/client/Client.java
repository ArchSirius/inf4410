package ca.polymtl.inf4410.tp1.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.lang.StringBuilder;
import java.lang.System;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class Client {

	private ServerInterface distantServerStub = null;

	public static void main(String[] args) {
		// ***** Entrez ici l'adresse IP du serveur *****
		String distantHostname = "132.207.12.214";
		String action = null;
		String argument = null;

		if (args.length > 0) {
			action = args[0];
			if (args.length > 1) {
				argument = args[1];
			}
		}

		try {
			Client client = new Client(distantHostname);
			client.run(action, argument);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Demande au client de generer un UUID si le fichier .client_id n'existe pas.
	 * Ecrit le UUI dans le fichier .client_id.
	 * @return Le UUID cree par le client.
	 */
	private UUID generateClientId() {
		try {
			UUID id = distantServerStub.generateclientid();
			Files.write(Paths.get(".client_id"), id.toString().getBytes());
			return id;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Demande de creer un fichier sur le client.
	 * @param nom - Le nom du fichier.
	 */
	private void create(String name) {
		if (name == null) {
			System.out.println("Vous devez specifier un nom de fichier");
			return;
		}
		try {
			if(distantServerStub.create(name)) {
				System.out.println(name.concat(" ajouté."));
			} else {
				System.out.println(name.concat(" existe déjà."));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Ecrit la liste des fichiers presents sur le fichier.
	 */
	private void list() {
		try {
			Map<String, UUID> files = distantServerStub.list();
			for (Map.Entry<String, UUID> file : files.entrySet()) {
				String owner = file.getValue() == null ? "non verouillé" : file.getValue().toString() ;
				System.out.println(file.getKey().concat("\t").concat(owner));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Synchronise les fichiers locaux avec les fichiers sur le serveur.
	 * Les fichiers locaux, si existant, sont ecraser par les fichier venant du serveur.
	 * Un fichier est créée s'il n'existe pas déjà localement.
	 */
	private void syncLocalDir() {
		try {
			Map<String, byte[]> files = distantServerStub.syncLocalDir();
			for (Map.Entry<String, byte[]> file : files.entrySet()) {
				Files.write(Paths.get(file.getKey()), file.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Chercher la dernière version d'un fichier sur le serveur.
	 * @param name - Le nom du fichier à aller chercher.
	 */
	private void get(String name) {
		if (name == null) {
			System.out.println("Vous devez specifier un nom de fichier");
			return;
		}
		try {
			byte[] checksum = getFileChecksum(name);
			byte[] data = distantServerStub.get(name, checksum);
			// Le serveur retourne null si la version local est la même que la version serveur.
			// Écrire seulement si la version serveur est différente.
			if (data != null) {
				Files.write(Paths.get(name), data);
			}
		} catch (Exception e) {
			System.out.println("N'a pas pu get sur le serveur le fichier ".concat(name));
		}

	}

	/**
	 * Demande au serveur de vérouiller un fichier.
	 * Si l'opération a été concluante, la copie locale est remplacée par la copie serveur.
	 * @param name - Le nom du fichier à vérouiller.
	 */
	private void lock(String name) {
		if (name == null) {
			System.out.println("Vous devez specifier un nom de fichier");
			return;
		}
		try {
			byte[] checksum = getFileChecksum(name);
			UUID clientId = getClientId();
			// Les données retournées par le serveur sont les données du fichier et l'id du client ayant le locké
			java.util.Map.Entry<byte[],UUID> data = distantServerStub.lock(name, clientId, checksum);
			// Si l'id retourné par le serveur n'est pas le même que l'id de l'utilisateur, échec du vérouillage.
			if (!data.getValue().equals(clientId)) {
				System.out.println(name.concat(" est déjà verrouillé par ").concat(data.getValue().toString()));
				return;
			}
			// Si l'id retourné par le serveur est le même que l'id de l'utilisateur et
			// que le fichier serveur est différent que le fichier local, écrire les données localement.
			if (data != null) {
				Files.write(Paths.get(name), data.toString().getBytes());
			}
		} catch (Exception e) {
			System.out.println("N'a pas pu lock sur le serveur le fichier ".concat(name));
		}

	}

	/**
	 * Pousser un fichier vérouiller par l'utilisateur sur le serveur.
	 * @param name - Nom du fichier à pousser sur le serveur
	 */
	private void push(String name) {
		if (name == null) {
			System.out.println("Vous devez specifier un nom de fichier");
			return;
		}
		try {
			byte[] data;
			try {
				data = Files.readAllBytes(Paths.get(name));
			} catch (IOException e) {
				System.out.println("Vous ne semblez pas avoir le fichier ".concat(name));
			}
			UUID clientId = getClientId();
			boolean success = distantServerStub.push(name, data, clientId);
			if (success) {
				System.out.println(name.concat(" a été envoyé au serveur"));
			} else {
				System.out.println("opération refusée : vous devez verrouiller d'abord verrouiller le fichier.");
			}
		} catch (Exception e) {
			System.out.println("N'a pas pu push sur le serveur le fichier ".concat(name));
		}

	}

	/**
	 * Retourne l'id de l'utilisateur s'il existe. Sinon génère l'id de l'utilisateur.
	 * @return L'id de l'utilsiateur
	 */
	private UUID getClientId() {
		try {
			byte[] content = Files.readAllBytes(Paths.get(".client_id"));
			ByteBuffer bb = ByteBuffer.wrap(content);
			long high = bb.getLong();
			long low = bb.getLong();
			return new UUID(high, low);
		} catch (Exception e) {
			return generateClientId();
		}
	}

	/**
	 * Génère le checksum d'un fichier
	 * @param name - Le nom du fichier
	 * @return Le checksum du fichier
	 */
	private byte[] getFileChecksum(String name) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] checksum;
			try {
				byte[] data = Files.readAllBytes(Paths.get(name));
				checksum = md.digest(data);
			} catch (IOException e) {
				checksum = "-1".getBytes();
			}
			return checksum;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	interface ServerOperation {
		void operation(String arg);
	}


	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	private void run(String action, String argument) throws RemoteException {
		if (action == null) return;
		switch (action) {
			case "create":
				create(argument);
				break;
			case "list":
				list();
				break;
			case "syncLocalDir":
				syncLocalDir();
				break;
			case "get":
				get(argument);
				break;
			case "lock":
				lock(argument);
				break;
			case "push":
				push(argument);
				break;
		}
	}

	private ServerInterface loadServerStub(String hostname){
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

}
