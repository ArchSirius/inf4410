# Partie 1

## Question 1
Nous pouvons observer qu'en ce qui à trait à la fonction normale, le temps pour appeler une fonction ne dépend pas de la taille des paramètres. Le temps demeure approximativement constant en fonction de la taille des paramètres.

Par contre, pour ce qui est des deux autre fonctions (locale et distante), le temps croît exponentiellement avec la taille des paramètres. Ceci est dû au fait que lors de l'appel RMI, les données doivent être sérialisées, écrites dans un socket, lues du socket et désérialisées.

Pour ce qui est de la fonction distante, celle-ci doit en plus envoyer les informations à un serveur distant. Ceci entraîne donc des délais de latence additionnels.

* Avantages Java RMI : Très simple d'utilisation. Il est possible d'envoyer des objets sur un socket sans ce soucier des détails d'implémentation. Ceci permet donc un développement plus rapide.
* Désavantages Java RMI : Il y a des coûts additionnels à l'abstraction. Le programmeur ne contrôle pas les détails d'implémentation. Il peut donc être difficile d'effectuer des actions qui sortent de celles prescrites pas la librairie.

## Question 2
1. La classe `Server` implémente l'interface `ServerInterface`, qui dérive de l'interface `java.rmi.Remote`. Ainsi, la classe `Server` implémente un **registre RMI**.
2. Le serveur distant (`Server`) est initialisé avec la méthode `Server.run()`.
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
