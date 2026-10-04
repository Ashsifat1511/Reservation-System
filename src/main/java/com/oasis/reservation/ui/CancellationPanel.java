package com.oasis.reservation.ui;

import com.oasis.reservation.AppContext;
import com.oasis.reservation.model.Reservation;
import com.oasis.reservation.service.PnrGenerator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.SQLException;
import java.util.Optional;

/** Enter a PNR, fetch the booking, then cancel it after an "Are you sure?" confirmation. */
public class CancellationPanel extends JPanel {

    private final AppContext ctx;
    private final JTextField pnrField = new JTextField(14);
    private final JTextArea details = new JTextArea(10, 40);
    private final JButton cancelButton = new JButton("Confirm Cancellation");
    private String fetchedPnr;

    public CancellationPanel(AppContext ctx) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JButton fetch = new JButton("Fetch");
        fetch.addActionListener(e -> fetch());
        pnrField.addActionListener(e -> fetch());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("PNR Number:"));
        top.add(pnrField);
        top.add(fetch);

        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        details.setBorder(BorderFactory.createTitledBorder("Booking Details"));

        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancel());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(cancelButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(details), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void fetch() {
        reset();
        String pnr = pnrField.getText().trim();
        if (pnr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a PNR number.", "Missing PNR",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!PnrGenerator.isWellFormed(pnr)) {
            JOptionPane.showMessageDialog(this, "A PNR number is " + PnrGenerator.PNR_LENGTH + " digits.",
                    "Invalid PNR", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Optional<Reservation> found = ctx.reservations().findByPnr(pnr);
            if (found.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No booking found for PNR " + pnr + ".",
                        "Not Found", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            details.setText(found.get().toDisplayText());
            fetchedPnr = pnr;
            cancelButton.setEnabled(true);
        } catch (SQLException ex) {
            Errors.show(this, "look up that booking", ex);
        }
    }

    private void cancel() {
        if (fetchedPnr == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel booking " + fetchedPnr + "?",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (ctx.reservations().deleteByPnr(fetchedPnr)) {
                JOptionPane.showMessageDialog(this, "Booking " + fetchedPnr + " has been cancelled.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Booking " + fetchedPnr + " no longer exists.",
                        "Not Found", JOptionPane.WARNING_MESSAGE);
            }
            pnrField.setText("");
            reset();
        } catch (SQLException ex) {
            Errors.show(this, "cancel the booking", ex);
        }
    }

    private void reset() {
        fetchedPnr = null;
        details.setText("");
        cancelButton.setEnabled(false);
    }
}
