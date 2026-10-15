package com.mycompany.javachat;

/**
 *
 * @author DANIEL
 */
// Message.java (classe abstraite)
import java.time.LocalDateTime;

public abstract class Message {
    protected Utilisateur expediteur;
    protected String contenu;
    protected LocalDateTime horodatage;

    public Message(Utilisateur expediteur, String contenu) {
        this(expediteur, contenu, LocalDateTime.now());
    }

    public Message(Utilisateur expediteur, String contenu, LocalDateTime horodatage) {
        this.expediteur = expediteur;
        this.contenu = contenu;
        this.horodatage = horodatage;
    }

    public Utilisateur getExpediteur() { return expediteur; }
    public String getContenu() { return contenu; }
    public LocalDateTime getHorodatage() { return horodatage; }

    public abstract String toString();
}
