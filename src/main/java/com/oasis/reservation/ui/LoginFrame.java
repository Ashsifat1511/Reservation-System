package com.oasis.reservation.ui;

import com.oasis.reservation.AppContext;
import com.oasis.reservation.security.PasswordHasher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/** Username + password login. Access is denied for invalid credentials. */
public class LoginFrame extends JFrame {

    private final AppContext ctx;
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JButton loginButton = new JButton("Login");

    public LoginFrame(AppContext ctx) {
        super("Online Reservation System - Login");
        this.ctx = ctx;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel title = new JLabel("Online Reservation System", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(16, 16, 4, 16));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; form.add(new JLabel("Username:"), g);
        g.gridx = 1; form.add(usernameField, g);
        g.gridx = 0; g.gridy = 1; form.add(new JLabel("Password:"), g);
        g.gridx = 1; form.add(passwordField, g);
        g.gridx = 1; g.gridy = 2; g.anchor = GridBagConstraints.EAST; form.add(loginButton, g);

        loginButton.addActionListener(e -> attemptLogin());
        getRootPane().setDefaultButton(loginButton);

        add(title, BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        if (username.isEmpty() || password.length == 0) {
            PasswordHasher.wipe(password);
            JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                    "Missing Fields", JOptionPane.WARNING_MESSAGE);
            return;
        }
        loginButton.setEnabled(false);
        // Hash verification is deliberately slow, so keep it off the UI thread.
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try {
                    return ctx.users().authenticate(username, password);
                } finally {
                    PasswordHasher.wipe(password);
                }
            }

            @Override
            protected void done() {
                loginButton.setEnabled(true);
                try {
                    if (get()) {
                        dispose();
                        new MainFrame(ctx, username).setVisible(true);
                    } else {
                        passwordField.setText("");
                        JOptionPane.showMessageDialog(LoginFrame.this, "Invalid username or password.",
                                "Access Denied", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    Errors.show(LoginFrame.this, "log you in", ex);
                }
            }
        }.execute();
    }
}
