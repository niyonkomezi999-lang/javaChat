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

// MessagePrive.java
public class MessagePrive extends Message {
    private Utilisateur destinataire;

    public MessagePrive(Utilisateur expediteur, Utilisateur destinataire, String contenu) {
        this(expediteur, destinataire, contenu, LocalDateTime.now());
    }

    public MessagePrive(Utilisateur expediteur, Utilisateur destinataire, String contenu,
                        LocalDateTime horodatage) {
        super(expediteur, contenu, horodatage);
        this.destinataire = destinataire;
    }

    public Utilisateur getDestinataire() {
        return destinataire;
    }

    @Override
    public String toString() {
        return "[" + horodatage + "] (Privé de " + expediteur.getPseudo() +
               " à " + destinataire.getPseudo() + "): " + contenu;
    }
}
