/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javachat;

/**
 *
 * @author DANIEL
 */
// UtilisateurDAO.java
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurDAO {
    private Connection conn;

    public UtilisateurDAO(Connection conn) {
        this.conn = conn;
    }

    public void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS utilisateurs (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                     "pseudo TEXT," +
                     "date_creation TEXT)";
        try (Statement statement = conn.createStatement()) {
            statement.execute(sql);
        }
        if (!hasColumn("date_creation")) {
            try (Statement statement = conn.createStatement()) {
                statement.execute("ALTER TABLE utilisateurs ADD COLUMN date_creation TEXT");
            }
        }
    }

    public void insert(Utilisateur u) throws SQLException {
        enregistrer(u);
    }

    public void enregistrer(Utilisateur u) throws SQLException {
        String sql = "INSERT INTO utilisateurs(pseudo, date_creation) " +
                     "SELECT ?, ? WHERE NOT EXISTS " +
                     "(SELECT 1 FROM utilisateurs WHERE pseudo = ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getPseudo());
            ps.setString(2, u.getDateCreation().toString());
            ps.setString(3, u.getPseudo());
            ps.executeUpdate();
        }
    }

    public List<Utilisateur> listerTous() throws SQLException {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        String sql = "SELECT pseudo, date_creation FROM utilisateurs ORDER BY id";
        try (Statement statement = conn.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                String date = result.getString("date_creation");
                LocalDateTime dateCreation = date == null
                        ? LocalDateTime.now() : LocalDateTime.parse(date);
                utilisateurs.add(new Utilisateur(result.getString("pseudo"), "hors ligne", dateCreation));
            }
        }
        return utilisateurs;
    }

    public boolean existe(String pseudo) throws SQLException {
        String sql = "SELECT 1 FROM utilisateurs WHERE pseudo = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, pseudo);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean hasColumn(String columnName) throws SQLException {
        try (Statement statement = conn.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(utilisateurs)")) {
            while (result.next()) {
                if (columnName.equalsIgnoreCase(result.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
