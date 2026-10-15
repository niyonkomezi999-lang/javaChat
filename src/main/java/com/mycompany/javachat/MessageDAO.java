/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javachat;

/**
 *
 * @author DANIEL
 */
// MessageDAO.java
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private Connection conn;

    public MessageDAO(Connection conn) {
        this.conn = conn;
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "expediteur TEXT," +
                     "destinataire TEXT," +
                     "contenu TEXT," +
                     "type TEXT," +
                     "date_envoi TEXT)";
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
        if (!hasColumn("destinataire")) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("ALTER TABLE messages ADD COLUMN destinataire TEXT");
            }
        }
        if (!hasColumn("date_envoi")) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("ALTER TABLE messages ADD COLUMN date_envoi TEXT");
            }
        }
        if (hasColumn("horodatage")) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("UPDATE messages SET date_envoi = horodatage " +
                                  "WHERE date_envoi IS NULL");
            }
        }
    }

    public void insert(Message m) throws SQLException {
        enregistrer(m);
    }

    public void enregistrer(Message m) throws SQLException {
        String destinataire = m instanceof MessagePrive
                ? ((MessagePrive) m).getDestinataire().getPseudo() : null;
        String sql = "INSERT INTO messages(expediteur, destinataire, contenu, type, date_envoi) " +
                     "VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getExpediteur().getPseudo());
            ps.setString(2, destinataire);
            ps.setString(3, m.getContenu());
            ps.setString(4, m.getClass().getSimpleName());
            ps.setString(5, m.getHorodatage().toString());
            ps.executeUpdate();
        }
    }

    public List<Message> listerTous() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT expediteur, destinataire, contenu, type, date_envoi " +
                     "FROM messages ORDER BY id";
        try (Statement statement = conn.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                String expediteur = result.getString("expediteur");
                String contenu = result.getString("contenu");
                String destinataire = result.getString("destinataire");
                String type = result.getString("type");
                String date = result.getString("date_envoi");
                LocalDateTime dateEnvoi = date == null
                        ? LocalDateTime.now() : LocalDateTime.parse(date);
                Utilisateur auteur = new Utilisateur(expediteur, "hors ligne");

                if ("MessageSysteme".equals(type)) {
                    messages.add(new MessageSysteme(contenu, dateEnvoi));
                } else if ("MessagePrive".equals(type) && destinataire != null) {
                    messages.add(new MessagePrive(auteur,
                            new Utilisateur(destinataire, "hors ligne"), contenu, dateEnvoi));
                } else {
                    messages.add(new MessageTexte(auteur, contenu, dateEnvoi));
                }
            }
        }
        return messages;
    }

    public List<Message> listerHistorique(String pseudo) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT expediteur, destinataire, contenu, type, date_envoi " +
                     "FROM messages WHERE type = 'MessageTexte' " +
                     "OR type = 'MessageSysteme' " +
                     "OR (type = 'MessagePrive' AND (expediteur = ? OR destinataire = ?)) " +
                     "ORDER BY id";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, pseudo);
            statement.setString(2, pseudo);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String expediteur = result.getString("expediteur");
                    String destinataire = result.getString("destinataire");
                    String contenu = result.getString("contenu");
                    String type = result.getString("type");
                    String date = result.getString("date_envoi");
                    LocalDateTime dateEnvoi = date == null
                            ? LocalDateTime.now() : LocalDateTime.parse(date);
                    Utilisateur auteur = new Utilisateur(expediteur, "hors ligne");

                    if ("MessageSysteme".equals(type)) {
                        messages.add(new MessageSysteme(contenu, dateEnvoi));
                    } else if ("MessagePrive".equals(type) && destinataire != null) {
                        messages.add(new MessagePrive(auteur,
                                new Utilisateur(destinataire, "hors ligne"), contenu, dateEnvoi));
                    } else {
                        messages.add(new MessageTexte(auteur, contenu, dateEnvoi));
                    }
                }
            }
        }
        return messages;
    }

    private boolean hasColumn(String columnName) throws SQLException {
        try (Statement statement = conn.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(messages)")) {
            while (result.next()) {
                if (columnName.equalsIgnoreCase(result.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
