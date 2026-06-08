#!/bin/bash
# On s'assure que le dossier run existe
mkdir -p ~/serveurWeb/run

# Lancement du serveur Java en arrière-plan (&)
# On redirige les flux vers un fichier texte temporaire si besoin ou /dev/null
java -cp ~/serveurWeb/bin HttpServer > ~/serveurWeb/run/server.log 2>&1 &

# On récupère le PID du dernier processus lancé ($!) et on l'écrit dans le fichier .pid
echo $! > ~/serveurWeb/run/myweb.pid