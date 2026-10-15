/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javachat;

/**
 *
 * @author DANIEL
 */
// SimulationChat.java
import java.util.*;

public class SimulationChat {
    public static void main(String[] args) {
        Utilisateur u1 = new Utilisateur("Alice", "en ligne");
        Utilisateur u2 = new Utilisateur("Bob", "occupé");
        Utilisateur u3 = new Utilisateur("Charlie", "hors ligne");

        List<Message> conversation = new ArrayList<>();
        conversation.add(new MessageSysteme("Bienvenue dans le chat !"));
        conversation.add(new MessageTexte(u1, "Salut tout le monde !"));
        conversation.add(new MessageTexte(u2, "Salut Alice !"));
        conversation.add(new MessagePrive(u2, u1, "Tu viens au projet demain ?"));
        conversation.add(new MessageTexte(u3, "Je suis là aussi."));
        conversation.add(new MessageSysteme("Charlie vient de se connecter."));
        conversation.add(new MessageTexte(u1, "Super, on est tous là."));
        conversation.add(new MessagePrive(u1, u3, "Tu peux m’envoyer le code ?"));

        for (Message m : conversation) {
            System.out.println(m);
        }
    }
}

