package com.mycompany.javachat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatClient {
    public static void main(String[] args) throws IOException {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Votre pseudo : ");
        String pseudo = console.readLine();
        if (pseudo == null || pseudo.trim().isEmpty()) {
            System.out.println("Le pseudo ne peut pas être vide.");
            return;
        }

        try (Socket socket = new Socket("localhost", 12345);
             BufferedReader entree = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter sortie = new PrintWriter(
                     socket.getOutputStream(), true, StandardCharsets.UTF_8)) {
            sortie.println(pseudo.trim());

            String bienvenue = entree.readLine();
            afficher(bienvenue);
            if (bienvenue == null || bienvenue.startsWith("ERROR|")) {
                return;
            }

            Thread ecoute = new Thread(() -> {
                try {
                    String message;
                    while ((message = entree.readLine()) != null) {
                        afficher(message);
                    }
                    System.out.println("Connexion au serveur terminée.");
                } catch (IOException e) {
                    System.out.println("Connexion au serveur interrompue.");
                }
            });
            ecoute.setDaemon(true);
            ecoute.start();

            String texte;
            while ((texte = console.readLine()) != null) {
                sortie.println(texte);
                if ("/quit".equals(texte.trim())) {
                    break;
                }
            }
        }
    }

    private static void afficher(String message) {
        if (message == null) {
            return;
        }
        if (message.startsWith("MESSAGE|PUBLIC|")) {
            String[] parties = message.split("\\|", 4);
            if (parties.length == 4) {
                System.out.println(parties[2] + " : " + parties[3]);
            }
        } else if (message.startsWith("MESSAGE|PRIVATE|")) {
            String[] parties = message.split("\\|", 5);
            if (parties.length == 5) {
                System.out.println("[Privé] " + parties[2] + " : " + parties[4]);
            }
        } else if (message.startsWith("USERS|")) {
            System.out.println("Connectés : " + message.substring("USERS|".length()));
        } else {
            int separateur = message.indexOf('|');
            System.out.println(separateur < 0 ? message : message.substring(separateur + 1));
        }
    }
}
