package com.oasis.reservation.ui;

import com.oasis.reservation.AppLog;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Shows a friendly message to the user and writes the technical details to the log file. */
final class Errors {

    private static final Logger LOG = AppLog.get(Errors.class);

    private Errors() {
    }

    static void show(Component parent, String action, Exception e) {
        LOG.log(Level.SEVERE, "Failed to " + action, e);
        JOptionPane.showMessageDialog(parent,
                "Sorry, we could not " + action + ".\nPlease try again. If it keeps happening, "
                        + "check the log file for details.",
                "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }
}
