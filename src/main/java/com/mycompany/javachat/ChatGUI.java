package com.mycompany.javachat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

public class ChatGUI extends JFrame {
    private static final Color NAVY = new Color(24, 34, 55);
    private static final Color ACCENT = new Color(75, 111, 237);
    private static final Color BACKGROUND = new Color(246, 248, 252);
    private static final Color MUTED = new Color(132, 143, 160);

    private final JPanel groupChatArea = creerListeMessages();
    private final JPanel privateChatArea = creerListeMessages();
    private final JPanel systemChatArea = creerListeMessages();
    private final JTextField groupInputField = new JTextField();
    private final JTextField privateInputField = new JTextField();
    private final JButton groupSendButton = new JButton("Envoyer");
    private final JButton privateSendButton = new JButton("Envoyer en privé");
    private final JComboBox<String> recipientBox = new JComboBox<>();
    private final JList<String> usersList = new JList<>();
    private final JLabel statusLabel = new JLabel("Connexion...");
    private final JPanel[] ongletEntetes = new JPanel[3];
    private final JLabel[] ongletIcones = new JLabel[3];
    private final JLabel[] ongletTitres = new JLabel[3];
    private final String pseudo;
    private volatile PrintWriter sortie;
    private volatile Socket socket;
    private volatile boolean fermetureVolontaire;

    public ChatGUI(String pseudo) {
        this.pseudo = pseudo;
        setTitle("JavaChat - " + pseudo);
        setSize(900, 600);
        setMinimumSize(new Dimension(700, 450));
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel contenu = new JPanel(new BorderLayout());
        contenu.setBackground(BACKGROUND);
        contenu.add(creerBarreLaterale(), BorderLayout.WEST);

        JPanel zonePrincipale = new JPanel(new BorderLayout());
        zonePrincipale.setBackground(BACKGROUND);
        zonePrincipale.add(creerEntete(pseudo), BorderLayout.NORTH);
        zonePrincipale.add(creerZoneChat(), BorderLayout.CENTER);
        contenu.add(zonePrincipale, BorderLayout.CENTER);
        setContentPane(contenu);

        groupSendButton.setEnabled(false);
        groupInputField.setEnabled(false);
        privateSendButton.setEnabled(false);
        privateInputField.setEnabled(false);
        recipientBox.setEnabled(false);
        groupSendButton.addActionListener(event -> envoyerPublic());
        groupInputField.addActionListener(event -> envoyerPublic());
        privateSendButton.addActionListener(event -> envoyerPrive());
        privateInputField.addActionListener(event -> envoyerPrive());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                fermerConnexion();
            }
        });

        setLocationRelativeTo(null);
        setVisible(true);
        connecter(pseudo);
    }

    private JPanel creerBarreLaterale() {
        JPanel barre = new JPanel(new BorderLayout());
        barre.setBackground(NAVY);
        barre.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel("  JavaChat");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 22));
        logo.setBorder(BorderFactory.createEmptyBorder(25, 15, 25, 10));
        barre.add(logo, BorderLayout.NORTH);

        JPanel participants = new JPanel(new BorderLayout(0, 12));
        participants.setOpaque(false);
        participants.setBorder(BorderFactory.createEmptyBorder(8, 18, 15, 12));

        JLabel titre = new JLabel("UTILISATEURS EN LIGNE");
        titre.setForeground(new Color(165, 178, 202));
        titre.setFont(new Font("SansSerif", Font.BOLD, 11));
        participants.add(titre, BorderLayout.NORTH);

        usersList.setBackground(NAVY);
        usersList.setForeground(new Color(232, 237, 247));
        usersList.setSelectionBackground(new Color(43, 58, 87));
        usersList.setSelectionForeground(Color.WHITE);
        usersList.setFixedCellHeight(42);
        usersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, "\u25CF   " + value, index, selected, cellHasFocus);
                label.setFont(new Font("SansSerif", Font.PLAIN, 14));
                label.setForeground(selected ? Color.WHITE : new Color(220, 228, 241));
                label.setBackground(selected ? new Color(43, 58, 87) : NAVY);
                label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
                return label;
            }
        });
        participants.add(usersList, BorderLayout.CENTER);
        barre.add(participants, BorderLayout.CENTER);

        JLabel pied = new JLabel("  MESSAGERIE EN DIRECT");
        pied.setForeground(new Color(132, 148, 177));
        pied.setFont(new Font("SansSerif", Font.BOLD, 10));
        pied.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 10));
        barre.add(pied, BorderLayout.SOUTH);
        return barre;
    }

    private JPanel creerEntete(String pseudo) {
        JPanel entete = new JPanel(new BorderLayout());
        entete.setBackground(Color.WHITE);
        entete.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 234, 241)),
                BorderFactory.createEmptyBorder(18, 26, 16, 26)));

        JPanel titres = new JPanel(new BorderLayout(0, 4));
        titres.setOpaque(false);
        JLabel titre = new JLabel("Salon général");
        titre.setForeground(NAVY);
        titre.setFont(new Font("SansSerif", Font.BOLD, 20));
        JLabel sousTitre = new JLabel("Connecté en tant que " + pseudo);
        sousTitre.setForeground(MUTED);
        sousTitre.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titres.add(titre, BorderLayout.NORTH);
        titres.add(sousTitre, BorderLayout.SOUTH);
        entete.add(titres, BorderLayout.WEST);

        statusLabel.setText("\u25CF  Connexion...");
        statusLabel.setForeground(new Color(190, 142, 43));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        entete.add(statusLabel, BorderLayout.EAST);
        return entete;
    }

    private JPanel creerZoneChat() {
        JTabbedPane onglets = new JTabbedPane();
        onglets.setFont(new Font("SansSerif", Font.BOLD, 13));
        onglets.setBackground(BACKGROUND);
        onglets.setForeground(NAVY);
        onglets.setOpaque(false);
        onglets.setFocusable(false);
        onglets.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        onglets.addTab("Groupe", creerOnglet(groupChatArea, creerZoneSaisieGroupe()));
        onglets.addTab("Messages privés", creerOnglet(privateChatArea, creerZoneSaisiePrivee()));
        onglets.addTab("Système", creerOnglet(systemChatArea, null));
        onglets.setToolTipTextAt(0, "Messages visibles par tous les utilisateurs");
        onglets.setToolTipTextAt(1, "Messages privés avec un utilisateur choisi");
        onglets.setToolTipTextAt(2, "Notifications et informations du serveur");

        onglets.setTabComponentAt(0, creerEnteteOnglet(onglets, 0, "\u25A3", "Groupe"));
        onglets.setTabComponentAt(1, creerEnteteOnglet(onglets, 1, "\u2709", "Messages privés"));
        onglets.setTabComponentAt(2, creerEnteteOnglet(onglets, 2, "\u2699", "Système"));
        onglets.addChangeListener(event -> appliquerStyleOnglets(onglets));
        appliquerStyleOnglets(onglets);
        return envelopperOnglets(onglets);
    }

    private JPanel creerEnteteOnglet(JTabbedPane onglets, int index, String icone, String titre) {
        JPanel entete = new JPanel(new BorderLayout(8, 0));
        JLabel labelIcone = new JLabel(icone);
        JLabel labelTitre = new JLabel(titre);
        labelIcone.setFont(new Font("SansSerif", Font.BOLD, 15));
        labelTitre.setFont(new Font("SansSerif", Font.BOLD, 12));
        entete.add(labelIcone, BorderLayout.WEST);
        entete.add(labelTitre, BorderLayout.CENTER);
        entete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        entete.setToolTipText(onglets.getToolTipTextAt(index));

        ongletEntetes[index] = entete;
        ongletIcones[index] = labelIcone;
        ongletTitres[index] = labelTitre;

        MouseAdapter selectionnerOnglet = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                onglets.setSelectedIndex(index);
            }
        };
        entete.addMouseListener(selectionnerOnglet);
        labelIcone.addMouseListener(selectionnerOnglet);
        labelTitre.addMouseListener(selectionnerOnglet);
        return entete;
    }

    private void appliquerStyleOnglets(JTabbedPane onglets) {
        for (int index = 0; index < ongletEntetes.length; index++) {
            boolean selectionne = index == onglets.getSelectedIndex();
            JPanel entete = ongletEntetes[index];
            entete.setBackground(selectionne ? new Color(232, 238, 255) : Color.WHITE);
            entete.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            selectionne ? new Color(203, 215, 255) : new Color(232, 236, 243),
                            1, true),
                    BorderFactory.createEmptyBorder(9, 15, 9, 15)));
            entete.setOpaque(true);

            Color couleur = selectionne ? ACCENT : MUTED;
            ongletIcones[index].setForeground(couleur);
            ongletTitres[index].setForeground(selectionne ? NAVY : MUTED);
        }
    }

    private JPanel creerListeMessages() {
        JPanel messages = new JPanel();
        messages.setLayout(new BoxLayout(messages, BoxLayout.Y_AXIS));
        messages.setBackground(BACKGROUND);
        messages.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return messages;
    }

    private JPanel envelopperOnglets(JTabbedPane onglets) {
        JPanel zone = new JPanel(new BorderLayout());
        zone.setBackground(BACKGROUND);
        zone.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        zone.add(onglets, BorderLayout.CENTER);
        return zone;
    }

    private JPanel creerOnglet(JPanel zoneTexte, JPanel saisie) {
        JPanel onglet = new JPanel(new BorderLayout());
        onglet.setBackground(BACKGROUND);
        JScrollPane defilement = new JScrollPane(zoneTexte);
        defilement.setBorder(BorderFactory.createEmptyBorder());
        defilement.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        defilement.getVerticalScrollBar().setUnitIncrement(18);
        defilement.getViewport().setBackground(BACKGROUND);
        onglet.add(defilement, BorderLayout.CENTER);
        if (saisie != null) {
            onglet.add(saisie, BorderLayout.SOUTH);
        }
        return onglet;
    }

    private JPanel creerZoneSaisieGroupe() {
        JPanel zone = new JPanel(new BorderLayout(12, 10));
        zone.setBackground(Color.WHITE);
        zone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 234, 241)),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        configurerChamp(groupInputField, "Écrivez un message pour le groupe");
        configurerBouton(groupSendButton);
        zone.add(groupInputField, BorderLayout.CENTER);
        zone.add(groupSendButton, BorderLayout.EAST);

        JLabel aide = new JLabel("/aide  \u2022  /liste  \u2022  /quit");
        aide.setForeground(MUTED);
        aide.setFont(new Font("SansSerif", Font.PLAIN, 11));
        zone.add(aide, BorderLayout.SOUTH);
        return zone;
    }

    private JPanel creerZoneSaisiePrivee() {
        JPanel zone = new JPanel(new BorderLayout(10, 10));
        zone.setBackground(Color.WHITE);
        zone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 234, 241)),
                BorderFactory.createEmptyBorder(12, 18, 14, 18)));

        recipientBox.addItem("Choisir un destinataire");
        recipientBox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        recipientBox.setForeground(NAVY);
        recipientBox.setBackground(new Color(247, 248, 251));
        recipientBox.setPreferredSize(new Dimension(190, 40));
        configurerChamp(privateInputField, "Écrivez votre message privé");
        configurerBouton(privateSendButton);

        JPanel ligneSaisie = new JPanel(new BorderLayout(10, 0));
        ligneSaisie.setOpaque(false);
        ligneSaisie.add(recipientBox, BorderLayout.WEST);
        ligneSaisie.add(privateInputField, BorderLayout.CENTER);
        ligneSaisie.add(privateSendButton, BorderLayout.EAST);
        zone.add(ligneSaisie, BorderLayout.CENTER);

        JLabel aide = new JLabel("Les destinataires hors ligne recevront le message à leur prochaine connexion.");
        aide.setForeground(MUTED);
        aide.setFont(new Font("SansSerif", Font.PLAIN, 11));
        zone.add(aide, BorderLayout.SOUTH);
        return zone;
    }

    private void configurerChamp(JTextField champ, String aide) {
        champ.setFont(new Font("SansSerif", Font.PLAIN, 14));
        champ.setForeground(NAVY);
        champ.setBackground(new Color(247, 248, 251));
        champ.setCaretColor(ACCENT);
        champ.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 231, 240), 1, true),
                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        champ.setToolTipText(aide);
    }

    private void configurerBouton(JButton bouton) {
        bouton.setFont(new Font("SansSerif", Font.BOLD, 13));
        bouton.setForeground(Color.WHITE);
        bouton.setBackground(ACCENT);
        bouton.setFocusPainted(false);
        bouton.setBorderPainted(false);
        bouton.setOpaque(true);
        bouton.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
    }

    private void connecter(String pseudo) {
        Thread thread = new Thread(() -> {
            try {
                Socket nouvelleSocket = new Socket("localhost", 12345);
                BufferedReader entree = new BufferedReader(new InputStreamReader(
                        nouvelleSocket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter nouvelleSortie = new PrintWriter(
                        nouvelleSocket.getOutputStream(), true, StandardCharsets.UTF_8);
                socket = nouvelleSocket;
                sortie = nouvelleSortie;
                nouvelleSortie.println(pseudo);

                String bienvenue = entree.readLine();
                if (bienvenue == null || bienvenue.startsWith("ERROR|")) {
                    fermerConnexion();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this, extraireTexte(bienvenue), "Connexion impossible",
                            JOptionPane.ERROR_MESSAGE));
                    return;
                }
                SwingUtilities.invokeLater(() -> {
                    groupSendButton.setEnabled(true);
                    groupInputField.setEnabled(true);
                    privateSendButton.setEnabled(true);
                    privateInputField.setEnabled(true);
                    recipientBox.setEnabled(true);
                    statusLabel.setText("\u25CF  Connecté");
                    statusLabel.setForeground(new Color(37, 153, 105));
                    afficherSysteme(extraireTexte(bienvenue), MUTED);
                    groupInputField.requestFocusInWindow();
                });

                String message;
                while ((message = entree.readLine()) != null) {
                    traiterMessage(message);
                }
                if (!fermetureVolontaire) {
                    SwingUtilities.invokeLater(() -> deconnecte(
                            "La connexion au serveur a été interrompue."));
                }
            } catch (IOException e) {
                if (!fermetureVolontaire) {
                    SwingUtilities.invokeLater(() -> deconnecte(
                            "Impossible de se connecter au serveur : " + e.getMessage()));
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void traiterMessage(String message) {
        if (message.startsWith("HISTORY|PUBLIC|")) {
            String[] parties = message.split("\\|", 5);
            if (parties.length == 5) {
                SwingUtilities.invokeLater(() -> afficherMessage(groupChatArea,
                        parties[2], parties[4], parties[3], pseudo.equals(parties[2]), NAVY));
            }
        } else if (message.startsWith("HISTORY|PRIVATE|")) {
            String[] parties = message.split("\\|", 6);
            if (parties.length == 6) {
                SwingUtilities.invokeLater(() -> afficherMessage(privateChatArea,
                        parties[2], parties[5], parties[4], pseudo.equals(parties[2]),
                        new Color(86, 76, 181)));
            }
        } else if (message.startsWith("HISTORY|SYSTEM|")) {
            String[] parties = message.split("\\|", 4);
            if (parties.length == 4) {
                SwingUtilities.invokeLater(() -> afficherSysteme(
                        "[" + parties[2] + "]  " + parties[3], MUTED));
            }
        } else if (message.startsWith("MESSAGE|PUBLIC|")) {
            String[] parties = message.split("\\|", 4);
            if (parties.length == 4) {
                SwingUtilities.invokeLater(() -> afficherMessage(groupChatArea,
                        parties[2], parties[3], heureActuelle(), pseudo.equals(parties[2]), NAVY));
            }
        } else if (message.startsWith("MESSAGE|PRIVATE|")) {
            String[] parties = message.split("\\|", 5);
            if (parties.length == 5) {
                SwingUtilities.invokeLater(() -> afficherMessage(privateChatArea,
                        parties[2], parties[4], heureActuelle(), pseudo.equals(parties[2]),
                        new Color(86, 76, 181)));
            }
        } else if (message.startsWith("USERS|")) {
            String liste = message.substring("USERS|".length());
            SwingUtilities.invokeLater(() -> usersList.setListData(
                    liste.isEmpty() ? new String[0] : liste.split(",")));
        } else if (message.startsWith("CONTACTS|")) {
            String liste = message.substring("CONTACTS|".length());
            SwingUtilities.invokeLater(() -> mettreAJourDestinataires(liste));
        } else if (message.startsWith("ERROR|")) {
            String texte = extraireTexte(message);
            SwingUtilities.invokeLater(() -> afficherSysteme(
                    "[Erreur] " + texte, new Color(190, 65, 75)));
        } else if (message.startsWith("INFO|") || message.startsWith("HELP|")) {
            String texte = extraireTexte(message);
            SwingUtilities.invokeLater(() -> afficherSysteme(texte, MUTED));
        }
    }

    private void mettreAJourDestinataires(String liste) {
        String[] utilisateurs = liste.isEmpty() ? new String[0] : liste.split(",");
        String selection = (String) recipientBox.getSelectedItem();
        recipientBox.removeAllItems();
        recipientBox.addItem("Choisir un destinataire");
        for (String utilisateur : utilisateurs) {
            if (!pseudo.equals(utilisateur)) {
                recipientBox.addItem(utilisateur);
            }
        }
        if (selection != null) {
            recipientBox.setSelectedItem(selection);
        }
    }

    private void envoyerPublic() {
        envoyer(groupInputField, false);
    }

    private void envoyerPrive() {
        String destinataire = (String) recipientBox.getSelectedItem();
        if (destinataire == null || "Choisir un destinataire".equals(destinataire)) {
            JOptionPane.showMessageDialog(this, "Choisissez un destinataire dans la liste.",
                    "Destinataire manquant", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        envoyer(privateInputField, true);
    }

    private void envoyer(JTextField champ, boolean prive) {
        String texte = champ.getText().trim();
        if (texte.isEmpty()) {
            return;
        }
        PrintWriter ecrivain = sortie;
        if (ecrivain == null) {
            JOptionPane.showMessageDialog(this, "Vous n'êtes pas connecté au serveur.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (prive) {
            String destinataire = (String) recipientBox.getSelectedItem();
            ecrivain.println("/prive " + destinataire + " " + texte);
        } else {
            ecrivain.println(texte);
        }
        champ.setText("");
        if (!prive && "/quit".equals(texte)) {
            fermetureVolontaire = true;
            fermerConnexion();
        }
    }

    private void afficherMessage(JPanel zone, String auteur, String texte, String heure,
                                 boolean messageDeMoi, Color couleur) {
        JPanel ligne = new JPanel(new BorderLayout());
        ligne.setOpaque(false);

        JPanel bulle = new JPanel(new BorderLayout(0, 7));
        Color fond = messageDeMoi ? new Color(230, 238, 255) : Color.WHITE;
        bulle.setBackground(fond);
        bulle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        messageDeMoi ? new Color(207, 220, 250) : new Color(231, 235, 242),
                        1, true),
                BorderFactory.createEmptyBorder(10, 13, 9, 13)));
        bulle.setMaximumSize(new Dimension(520, Integer.MAX_VALUE));

        JPanel enteteBulle = new JPanel(new BorderLayout(18, 0));
        enteteBulle.setOpaque(false);
        JLabel nom = new JLabel(messageDeMoi ? "Vous" : auteur);
        nom.setFont(new Font("SansSerif", Font.BOLD, 12));
        nom.setForeground(couleur);
        JLabel horodatage = new JLabel(heure);
        horodatage.setFont(new Font("SansSerif", Font.PLAIN, 10));
        horodatage.setForeground(MUTED);
        enteteBulle.add(nom, BorderLayout.WEST);
        enteteBulle.add(horodatage, BorderLayout.EAST);
        bulle.add(enteteBulle, BorderLayout.NORTH);

        JTextArea corps = new JTextArea(texte);
        corps.setFont(new Font("SansSerif", Font.PLAIN, 14));
        corps.setForeground(NAVY);
        corps.setOpaque(false);
        corps.setEditable(false);
        corps.setLineWrap(true);
        corps.setWrapStyleWord(true);
        corps.setColumns(38);
        corps.setBorder(BorderFactory.createEmptyBorder());
        corps.setMaximumSize(new Dimension(480, Integer.MAX_VALUE));
        bulle.add(corps, BorderLayout.CENTER);

        if (messageDeMoi) {
            ligne.add(bulle, BorderLayout.EAST);
        } else {
            ligne.add(bulle, BorderLayout.WEST);
        }
        ligne.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                Math.max(bulle.getPreferredSize().height, 54)));
        zone.add(ligne);
        zone.add(Box.createVerticalStrut(10));
        actualiserZoneMessages(zone);
    }

    private void afficherSysteme(String texte, Color couleur) {
        JPanel ligne = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
        ligne.setOpaque(false);

        JLabel notification = new JLabel(texte);
        notification.setFont(new Font("SansSerif", Font.PLAIN, 12));
        notification.setForeground(couleur);
        notification.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 234, 241), 1, true),
                BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        notification.setOpaque(true);
        notification.setBackground(Color.WHITE);

        ligne.add(notification);
        ligne.setMaximumSize(new Dimension(Integer.MAX_VALUE,
                notification.getPreferredSize().height + 6));
        systemChatArea.add(ligne);
        systemChatArea.add(Box.createVerticalStrut(8));
        actualiserZoneMessages(systemChatArea);
    }

    private void actualiserZoneMessages(JPanel zone) {
        zone.revalidate();
        zone.repaint();
        SwingUtilities.invokeLater(() -> {
            if (zone.getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) zone.getParent();
                viewport.setViewPosition(new java.awt.Point(0,
                        Math.max(0, zone.getHeight() - viewport.getHeight())));
            }
        });
    }

    private String heureActuelle() {
        return java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    private String extraireTexte(String message) {
        if (message == null) {
            return "Le serveur a fermé la connexion.";
        }
        int separateur = message.indexOf('|');
        return separateur < 0 ? message : message.substring(separateur + 1);
    }

    private void deconnecte(String message) {
        groupSendButton.setEnabled(false);
        groupInputField.setEnabled(false);
        privateSendButton.setEnabled(false);
        privateInputField.setEnabled(false);
        recipientBox.setEnabled(false);
        statusLabel.setText("\u25CF  Déconnecté");
        statusLabel.setForeground(new Color(190, 65, 75));
        afficherSysteme(message, new Color(190, 65, 75));
        JOptionPane.showMessageDialog(this, message, "Connexion interrompue",
                JOptionPane.WARNING_MESSAGE);
    }

    private void fermerConnexion() {
        fermetureVolontaire = true;
        try {
            Socket connexion = socket;
            if (connexion != null) {
                connexion.close();
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la fermeture : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String pseudo = JOptionPane.showInputDialog(null, "Choisissez un pseudo :",
                    "Connexion JavaChat", JOptionPane.QUESTION_MESSAGE);
            if (pseudo != null && !pseudo.trim().isEmpty()) {
                new ChatGUI(pseudo.trim());
            }
        });
    }
}
