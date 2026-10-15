/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javachat;

/**
 *
 * @author DANIEL
 */
import java.time.LocalDateTime;

// MessageTexte.java
public class MessageTexte extends Message {
    public MessageTexte(Utilisateur expediteur, String contenu) {
        super(expediteur, contenu);
    }

    public MessageTexte(Utilisateur expediteur, String contenu, LocalDateTime horodatage) {
        super(expediteur, contenu, horodatage);
    }

    @Override
    public String toString() {
        return "[" + horodatage + "] " + expediteur.getPseudo() + ": " + contenu;
    }
}
