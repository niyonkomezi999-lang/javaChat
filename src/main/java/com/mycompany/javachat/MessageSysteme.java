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

// MessageSysteme.java
public class MessageSysteme extends Message {
    public MessageSysteme(String contenu) {
        super(new Utilisateur("SYSTEM", "info"), contenu);
    }

    public MessageSysteme(String contenu, LocalDateTime horodatage) {
        super(new Utilisateur("SYSTEM", "info"), contenu, horodatage);
    }

    @Override
    public String toString() {
        return "[SYSTEM] " + contenu;
    }
}
