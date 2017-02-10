# Partie 1
Le répertoire `ResponseTime_Analyzer` contient les fichiers requis pour exécuter la partie 1.
Seuls les fichiers de code modifiés sont remis dans l'archive.
Ils peuvent écraser ceux présents dans l'archive fournie pour l'exécution.

**Serveur** : Les instructions pour exécuter le serveur initial sont valides pour notre implémentation.

**Client** : Les instructions pour exécuter le client initial sont valides pour notre implémentation.
L'adresse IP de notre machine virtuelle est `132.207.12.214`.
Le client prend comme second argument un entier de 1 à 7.

Exemple:
```
./client 132.207.12.214 4
```

# Partie 2
Le répertoire `FileSystem` contient les fichiers requis pour exécuter la partie 2.
Seuls les fichiers de code modifiés sont remis dans l'archive.
Ils peuvent écraser ceux présents dans l'archive fournie pour l'exécution.

**Serveur** : Les instructions pour exécuter le serveur initial sont valides pour notre implémentation.
Le répertoire `files` doit être présent dans la racine `FileSystem/`

**Client** : Les instructions demandées ont été implémentées selon les spécifications.
Les arguments et commandes disponibles sont:
* create : crée un nouveau fichier vide sur le serveur dont le nom est le second argument.
  Exemple : `./client create foo.txt`.
  L'opération échoue si un fichier de même nom existe déjà.
* list : retourne la liste des fichiers présents sur le serveur.
  Exemple : `./client list`.
* syncLocalDir : synchronise les fichiers locaux avec le serveur.
  Les fichiers locaux seront écrasés pour ceux du serveur.
  Exemple: `./client syncLocalDir`.
* get : récupère dans le répertoire local un fichier provenant du serveur.
  Exemple : `./client get bar.txt`.
  L'opération échoue si le fichier n'existe pas.
* lock : verrouille un fichier distant pour en empêcher l'accès en écriture aux autres utilisateurs.
  Exemple: `./client foo.txt`.
  L'opération échoue si le fichier est déjà verrouillé par un autre utilisateur ou n'existe pas.
* push : écrase un fichier verrouillé sur le serveur avec le fichier local.
  Exemple : `./client push foo.txt`.
  L'opération échoue si le fichier n'est pas verrouillé ou n'existe pas.
