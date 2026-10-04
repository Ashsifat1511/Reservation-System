package com.oasis.reservation.ui;

import com.oasis.reservation.AppContext;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** Shown after login: a tab for booking and a tab for cancelling, plus logout. */
public class MainFrame extends JFrame {

    public MainFrame(AppContext ctx, String username) {
        super("Online Reservation System - " + username);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Book Ticket", new ReservationPanel(ctx, username));
        tabs.addTab("Cancel Ticket", new CancellationPanel(ctx));

        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            dispose();
            new LoginFrame(ctx).setVisible(true);
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 4, 12));
        header.add(new JLabel("Logged in as: " + username), BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }
}
