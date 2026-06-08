#!/bin/bash
PID_FILE="$HOME/serveurWeb/run/myweb.pid"

if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    # On tue le processus proprement
    kill "$PID"
    # On supprime le fichier PID devenu inutile
    rm "$PID_FILE"
else
    echo "Le fichier PID n'existe pas. Le serveur est probablement déjà arrêté."
fi