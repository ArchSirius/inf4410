# Partie 1

## Question 1

## Question 2
1. La classe `Server` implémente l'interface `ServerInterface`, qui dérive de l'interface `java.rmi.Remote`. Ainsi, la classe `Server` implémente un **registre RMI**.
12. Le serveur distant (`Server`) est initialisé avec la méthode `Server.run()`.
3. Le serveur local (`Server`) est initialisé avec la méthode `Server.run()`.
4. Le client crée un nouvel objet `FakeServer` pour l'appel normal.
5. Le client se connecte au **serveur local** pour l'appel RMI local avec la méthode `Client.loadServerStub()`.
6. Le client se connecte au **serveur distant** pour l'appel RMI distant avec la méthode `Client.loadServerStub()`.
7. Le code client est executé avec la méthode `Client.run()`.
8. Le client exécute l'**appel normal** avec la méthode `Client.appelNormal()`.
9. Le client lance l'exécution locale (par `FakeServer`) d'un calcul avec la méthode `FakeServer.execute()`.
10. Le client exécute l'**appel RMI local** avec la méthode `Client.appelRMILocal()`.
11. Le client lance l'exécution sur le serveur local d'un calcul avec la méthode `ServerInterface.execute()`.
12. Le client exécute l'**appel RMI distant** avec la méthode `Client.appelRMIDistant()`.
13. Le client lance l'exécution sur le serveur distant d'un calcul avec la méthode `ServerInterface.execute()`.
