/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javachat;

/**
 *
 * @author DANIEL
 */
// MainDB.java
import java.sql.*;

public class MainDB {
    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:chat.db")) {
            UtilisateurDAO udao = new UtilisateurDAO(conn);
            MessageDAO mdao = new MessageDAO(conn);

            udao.createTable();
            mdao.createTable();

            Utilisateur u1 = new Utilisateur("Alice", "en ligne");
            udao.insert(u1);

            Message m1 = new MessageTexte(u1, "Hello DB!");
            mdao.insert(m1);

            System.out.println("Données insérées avec succès !");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

