/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.javachat;

import java.time.LocalDateTime;

/**
 *
 * @author DANIEL
 */
// Utilisateur.java
public class Utilisateur {
    private String pseudo;
    private String statut;
    private LocalDateTime dateCreation;

    public Utilisateur(String pseudo, String statut) {
        this(pseudo, statut, LocalDateTime.now());
    }

    public Utilisateur(String pseudo, String statut, LocalDateTime dateCreation) {
        this.pseudo = pseudo;
        this.statut = statut;
        this.dateCreation = dateCreation;
    }

    public String getPseudo() { return pseudo; }
    public String getStatut() { return statut; }
    public LocalDateTime getDateCreation() { return dateCreation; }

    @Override
    public String toString() {
        return pseudo + " [" + statut + "]";
    }
}
