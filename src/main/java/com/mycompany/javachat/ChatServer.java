package com.mycompany.javachat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static final ConcurrentHashMap<String, ClientHandler> clients =
            new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException, SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
             ServerSocket server = new ServerSocket(PORT)) {
            UtilisateurDAO utilisateurDAO = new UtilisateurDAO(connection);
            MessageDAO messageDAO = new MessageDAO(connection);
            utilisateurDAO.createTable();
            messageDAO.createTable();

            System.out.println("Serveur démarré sur le port " + PORT + "...");
            while (true) {
                Socket socket = server.accept();
                Thread thread = new Thread(
                        new ClientHandler(socket, connection, utilisateurDAO, messageDAO));
                thread.start();
            }
        }
    }

    private static void envoyerListeUtilisateurs() {
        List<String> pseudos = new ArrayList<>(clients.keySet());
        Collections.sort(pseudos);
        String liste = String.join(",", pseudos);
        for (ClientHandler client : clients.values()) {
            client.envoyer("USERS|" + liste);
        }
    }

    private static void envoyerListeContacts(Connection connection,
            UtilisateurDAO utilisateurDAO) throws SQLException {
        List<Utilisateur> utilisateurs;
        synchronized (connection) {
            utilisateurs = utilisateurDAO.listerTous();
        }
        List<String> pseudos = new ArrayList<>();
        for (Utilisateur utilisateur : utilisateurs) {
            pseudos.add(utilisateur.getPseudo());
        }
        String liste = String.join(",", pseudos);
        for (ClientHandler client : clients.values()) {
            client.envoyer("CONTACTS|" + liste);
        }
    }

    private static void diffuser(String message) {
        for (ClientHandler client : clients.values()) {
            client.envoyer(message);
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final Connection connection;
        private final UtilisateurDAO utilisateurDAO;
        private final MessageDAO messageDAO;
        private PrintWriter sortie;
        private String pseudo;

        ClientHandler(Socket socket, Connection connection, UtilisateurDAO utilisateurDAO,
                      MessageDAO messageDAO) {
            this.socket = socket;
            this.connection = connection;
            this.utilisateurDAO = utilisateurDAO;
            this.messageDAO = messageDAO;
        }

        @Override
        public void run() {
            try (Socket clientSocket = socket;
                 BufferedReader entree = new BufferedReader(new InputStreamReader(
                         clientSocket.getInputStream(), StandardCharsets.UTF_8))) {
                sortie = new PrintWriter(clientSocket.getOutputStream(), true, StandardCharsets.UTF_8);
                if (!connecter(entree)) {
                    return;
                }

                String ligne;
                while ((ligne = entree.readLine()) != null) {
                    traiter(ligne);
                    if ("/quit".equals(ligne.trim())) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Connexion interrompue avec " + pseudo);
            } finally {
                if (pseudo != null && clients.remove(pseudo, this)) {
                    envoyerListeUtilisateurs();
                    diffuser("INFO| " + pseudo + " s'est déconnecté.");
                }
            }
        }

        private boolean connecter(BufferedReader entree) throws IOException {
            String nom = entree.readLine();
            if (nom == null || !nom.matches("[A-Za-z0-9_-]{1,20}")) {
                envoyer("ERROR|Le pseudo doit contenir 1 à 20 lettres, chiffres, tirets ou _.");
                return false;
            }

            pseudo = nom;
            if (clients.putIfAbsent(pseudo, this) != null) {
                envoyer("ERROR|Ce pseudo est déjà connecté.");
                pseudo = null;
                return false;
            }

            try {
                synchronized (connection) {
                    utilisateurDAO.enregistrer(new Utilisateur(pseudo, "en ligne"));
                }
            } catch (SQLException e) {
                envoyer("ERROR|Impossible d'enregistrer l'utilisateur dans la base.");
                clients.remove(pseudo, this);
                pseudo = null;
                return false;
            }

            envoyer("OK|Bienvenue " + pseudo + " !");
            envoyerHistorique();
            envoyerListeUtilisateurs();
            try {
                envoyerListeContacts(connection, utilisateurDAO);
            } catch (SQLException e) {
                envoyer("ERROR|Impossible de charger la liste des destinataires.");
                System.err.println("Erreur lors du chargement des destinataires : " + e.getMessage());
            }
            diffuser("INFO| " + pseudo + " s'est connecté.");
            return true;
        }

        private void envoyerHistorique() {
            try {
                List<Message> historique;
                synchronized (connection) {
                    historique = messageDAO.listerHistorique(pseudo);
                }
                DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                for (Message message : historique) {
                    String date = message.getHorodatage().format(formatDate);
                    if (message instanceof MessagePrive) {
                        MessagePrive prive = (MessagePrive) message;
                        envoyer("HISTORY|PRIVATE|" + prive.getExpediteur().getPseudo()
                                + "|" + prive.getDestinataire().getPseudo()
                                + "|" + date + "|" + prive.getContenu());
                    } else if (message instanceof MessageSysteme) {
                        envoyer("HISTORY|SYSTEM|" + date + "|" + message.getContenu());
                    } else {
                        envoyer("HISTORY|PUBLIC|" + message.getExpediteur().getPseudo()
                                + "|" + date + "|" + message.getContenu());
                    }
                }
            } catch (SQLException e) {
                envoyer("ERROR|Impossible de charger l'historique des messages.");
                System.err.println("Erreur lors du chargement de l'historique : " + e.getMessage());
            }
        }

        private void traiter(String ligne) {
            if ("/aide".equals(ligne.trim())) {
                envoyer("HELP|Commandes : /aide, /liste, /prive pseudo message, /quit. " +
                        "Tout autre texte est envoyé à tous.");
            } else if ("/liste".equals(ligne.trim())) {
                List<String> pseudos = new ArrayList<>(clients.keySet());
                Collections.sort(pseudos);
                envoyer("USERS|" + String.join(",", pseudos));
            } else if ("/quit".equals(ligne.trim())) {
                envoyer("INFO|Déconnexion.");
            } else if (ligne.startsWith("/prive")) {
                envoyerMessagePrive(ligne);
            } else if (ligne.startsWith("/")) {
                envoyer("ERROR|Commande inconnue. Tapez /aide pour voir les commandes.");
            } else if (ligne.trim().isEmpty()) {
                envoyer("ERROR|Le message ne peut pas être vide.");
            } else {
                MessageTexte message = new MessageTexte(
                        new Utilisateur(pseudo, "en ligne"), ligne);
                if (enregistrer(message)) {
                    diffuser("MESSAGE|PUBLIC|" + pseudo + "|" + ligne);
                }
            }
        }

        private void envoyerMessagePrive(String ligne) {
            String[] morceaux = ligne.split("\\s+", 3);
            if (morceaux.length < 3 || !"/prive".equals(morceaux[0])
                    || morceaux[2].trim().isEmpty()) {
                envoyer("ERROR|Utilisation : /prive pseudo message");
                return;
            }

            String destinataire = morceaux[1];
            ClientHandler client = clients.get(destinataire);
            try {
                boolean utilisateurExiste;
                synchronized (connection) {
                    utilisateurExiste = utilisateurDAO.existe(destinataire);
                }
                if (!utilisateurExiste) {
                    envoyer("ERROR|Ce destinataire n'existe pas encore.");
                    return;
                }
            } catch (SQLException e) {
                envoyer("ERROR|Impossible de vérifier le destinataire.");
                System.err.println("Erreur lors de la vérification du destinataire : " + e.getMessage());
                return;
            }

            MessagePrive message = new MessagePrive(
                    new Utilisateur(pseudo, "en ligne"),
                    new Utilisateur(destinataire, client == null ? "hors ligne" : "en ligne"),
                    morceaux[2]);
            if (enregistrer(message)) {
                String texte = "MESSAGE|PRIVATE|" + pseudo + "|" + destinataire + "|" + morceaux[2];
                if (client != null && client != this) {
                    client.envoyer(texte);
                }
                envoyer(texte);
                if (client == null) {
                    envoyer("INFO|Message enregistré. Il sera livré à " + destinataire
                            + " à sa prochaine connexion.");
                }
            }
        }

        private boolean enregistrer(Message message) {
            try {
                synchronized (connection) {
                    messageDAO.enregistrer(message);
                }
                return true;
            } catch (SQLException e) {
                envoyer("ERROR|Impossible d'enregistrer le message dans la base.");
                System.err.println("Erreur SQLite : " + e.getMessage());
                return false;
            }
        }

        private void envoyer(String message) {
            sortie.println(message);
        }
    }
}
