# Compilation

Le script `compile.sh` permet de compiler les classes nécessaires au travail pratique.
* `./compile.sh` compile toutes les classes;
* `./compile.sh server` compile le serveur;
* `./compile.sh client` compile le client;
* `./compile.sh lb` compile le répartiteur;
* `./compile.sh clean` nettoie le répertoire d'exécutables (`bin`).


# Exécution

Le script `run.sh` permet d'exécuter le programme demandé.
* `./run.sh server` exécute le serveur (voir sous-section *Serveur*);
* `./run.sh client` exécute le client (voir sous-section *Client*);
* `./run.sh lb` exécute le répartiteur (voir sous-section *Répartiteur*);


## Serveur

Le serveur est l'unité qui effectue les calculs.
Il peut refuser des tâches trop volumineuses ou envoyer des réponses erronnées.

### Prérequis
Les instances serveurs doivent être en exécution avant l'exécution du répartiteur.
`rmiregistry` doit d'exécuter dans le répertoire `bin` et utiliser le port défini par `portRMI` dans le fichier `config/server.properties`.
Exemple :
```
cd bin
rmiregistry 5001 &
```

### Configuration
Les valeurs de configuration et les fichiers utilisés sont les suivants:
* `config/server.properties`:`portRmi` ([1024-65536]) définit le port utilisé par RMI;
* `config/server.properties`:`portServer` ([1024-65536]) définit le port sur lequel écoute une instance serveur;
* `config/shared.properties`:`securise` (booléen) définit le mode d'exécution (sécurisé ou non sécurisé).

### Exécution
Le programme `Server` nécessite deux arguments : une capacité (entier positif) et un taux d'erreurs (entier entre 0 et 100).
Par exemple,
```
./run.sh server 5 20
```
créera une instance du serveur ayant une capacité de 5 et un taux d'erreurs de 20%.
En mode sécurisé, le taux d'erreur sera de 0% peut importe la valeur entrée.


## Répartiteur (*load balancer*)

Le répartiteur est l'élément central de l'architecture.
Il lance l'exécution de calculs sur une grappe de serveurs en répartissant la charge en mode sécurisé et en comparant les multiples résultats en mode non sécurisé.

### Prérequis
`rmiregistry` doit d'exécuter dans le répertoire `bin` et utiliser le port défini par `portRMI` dans le fichier `config/loadBalancer.properties`.
Exemple :
```
cd bin
rmiregistry 5001 &
```

### Configuration
Les valeurs de configuration et les fichiers utilisés sont les suivants:
* `config/loadBalancer.properties`:`hostnames` (adresses IP séparées par ';') définit le port utilisé par RMI;
* `config/loadBalancer.properties`:`portRMI` ([1024-65536]) définit le port utilisé par RMI;
* `config/loadBalancer.properties`:`portLoadBalancer` ([1024-65536]) définit le port sur lequel écoute le répartiteur;
* `config/shared.properties`:`securise` (booléen) définit le mode d'exécution (sécurisé ou non sécurisé).

### Exécution
Le programme `LoadBalancer` ne requiert aucun argument. Il s'exécute comme suit :
```
./run.sh lb
```


## Client
Le client constitue le point d'entrée de l'architecture pour un utilisateur.
Il demande au répartiteur de lancer un calcul sur les serveurs et affiche le résultat final au client.
Il affiche le temps d'exécution à l'usager.

### Prérequis
Les fichiers d'opérations doivent être présents également dans le répertoire `config/operations` du répartiteur pour y être lus localement.

### Configuration
Les valeurs de configuration et les fichiers utilisés sont les suivants:
* `config/loadBalancer.properties`:`portRMI` ([1024-65536]) définit le port utilisé par RMI du répartiteur.

### Exécution
Le programme `Server` nécessite deux arguments : une capacité (entier positif) et un taux d'erreurs (entier entre 0 et 100).
Par exemple,
```
./run.sh client 132.207.12.33 operations-588
```
utilisera le répartiteur à l'adresse 132.207.12.33 pour effectuer les opérations du fichier `config/operations/operations-588`.


# Tests de performance
On propose 6 tests de performance afin de mesurer le temps d'exécution de divers scénarios sur le système.
Afin de calculer un nombre suffisant d'opérations, une concaténation de 500 opérations fût utilisée ainsi :
```
cd config/operations
cat * > operations-all
```
puis les tests suivants ont été réalisés :

1. En mode sécurisé, 1 serveur de capacité 2 et 1 serveur de capacité 3;
2. En mode sécurisé, 2 serveurs de capacité 2 et 1 serveur de capacité 3;
3. En mode sécurisé, 2 serveurs de capacité 2 et 2 serveurs de capacité 3;
4. En mode non sécurisé, 3 serveurs sans faute de capacité 5;
5. En mode non sécurisé, 1 serveur ayant un taux de faute de 50% de capacité 5 et 2 serveurs sans faute de capacité 5;
6. En mode non sécurisé, 1 serveur ayant un taux de faute de 80% de capacité 5 et 2 serveurs sans faute de capacité 5;

Pour chaque test la séquence suivante fût réalisée:

1. Mettre à jour les fichiers de configuration;
2. Démarrer les instances serveurs;
3. Démarrer le répartiteur;
4. Exécuter 10 fois une instance client avec le fichier `operations-all` afin d'obtenir un échantillon valide.
