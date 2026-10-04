package com.oasis.reservation.ui;

import com.oasis.reservation.AppContext;
import com.oasis.reservation.AppLog;
import com.oasis.reservation.model.Reservation;
import com.oasis.reservation.service.BookingValidator;
import com.oasis.reservation.service.ValidationException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Reservation form. The train name is filled in automatically from the train number. */
public class ReservationPanel extends JPanel {

    private static final Logger LOG = AppLog.get(ReservationPanel.class);

    static final String[] CLASS_TYPES = {
            "-- Select --", "Sleeper (SL)", "AC 3 Tier (3A)", "AC 2 Tier (2A)",
            "AC First Class (1A)", "Chair Car (CC)", "Second Sitting (2S)"
    };

    private final AppContext ctx;
    private final String username;

    private final JTextField passengerName = new JTextField(22);
    private final JTextField trainNumber = new JTextField(22);
    private final JTextField trainName = new JTextField(22);
    private final JComboBox<String> classType = new JComboBox<>(CLASS_TYPES);
    private final JTextField journeyDate = new JTextField(22);
    private final JTextField source = new JTextField(22);
    private final JTextField destination = new JTextField(22);

    public ReservationPanel(AppContext ctx, String username) {
        super(new GridBagLayout());
        this.ctx = ctx;
        this.username = username;
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        trainName.setEditable(false);
        trainName.setFocusable(false);
        journeyDate.setToolTipText("Format: " + BookingValidator.DATE_PATTERN);
        trainNumber.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { lookupTrainName(); }
            @Override public void removeUpdate(DocumentEvent e) { lookupTrainName(); }
            @Override public void changedUpdate(DocumentEvent e) { lookupTrainName(); }
        });

        int row = 0;
        addRow(row++, "Passenger Name *", passengerName);
        addRow(row++, "Train Number *", trainNumber);
        addRow(row++, "Train Name", trainName);
        addRow(row++, "Class Type *", classType);
        addRow(row++, "Date of Journey * (" + BookingValidator.DATE_PATTERN + ")", journeyDate);
        addRow(row++, "Source Station *", source);
        addRow(row++, "Destination Station *", destination);

        JButton book = new JButton("Insert / Book");
        JButton clear = new JButton("Clear");
        book.addActionListener(e -> book());
        clear.addActionListener(e -> clearForm());
        JPanel buttons = new JPanel();
        buttons.add(clear);
        buttons.add(book);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 1; g.gridy = row; g.anchor = GridBagConstraints.EAST;
        add(buttons, g);
    }

    private void addRow(int row, String label, JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.gridy = row;
        g.gridx = 0; g.anchor = GridBagConstraints.WEST;
        add(new JLabel(label), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        add(field, g);
    }

    private void lookupTrainName() {
        String text = trainNumber.getText().trim();
        if (!BookingValidator.isNumeric(text)) {
            trainName.setText("");
            return;
        }
        try {
            trainName.setText(ctx.trains().findTrainName(Integer.parseInt(text)).orElse(""));
        } catch (SQLException ex) {
            trainName.setText("");
            LOG.log(Level.WARNING, "Train lookup failed for " + text, ex);
        }
    }

    private void book() {
        String selectedClass = classType.getSelectedIndex() > 0 ? (String) classType.getSelectedItem() : "";
        List<String> errors = BookingValidator.validate(passengerName.getText(), trainNumber.getText(),
                trainName.getText(), selectedClass, journeyDate.getText(), source.getText(),
                destination.getText(), LocalDate.now());
        if (!errors.isEmpty()) {
            showErrors(errors);
            return;
        }
        try {
            Reservation r = ctx.reservationService().book(username, passengerName.getText(),
                    trainNumber.getText(), selectedClass, journeyDate.getText(), source.getText(),
                    destination.getText());
            showConfirmation(r);
            clearForm();
        } catch (ValidationException ex) {
            showErrors(ex.errors());
        } catch (SQLException ex) {
            Errors.show(this, "save your booking", ex);
        }
    }

    private void showErrors(List<String> errors) {
        JOptionPane.showMessageDialog(this, "Please fix the following:\n- " + String.join("\n- ", errors),
                "Invalid Input", JOptionPane.WARNING_MESSAGE);
    }

    private void showConfirmation(Reservation r) {
        JTextArea details = new JTextArea(r.toDisplayText());
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel panel = new JPanel(new java.awt.BorderLayout(0, 8));
        panel.add(new JLabel("Your ticket has been booked. Please note your PNR number."),
                java.awt.BorderLayout.NORTH);
        panel.add(details, java.awt.BorderLayout.CENTER);
        JOptionPane.showMessageDialog(this, panel, "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearForm() {
        passengerName.setText("");
        trainNumber.setText("");
        classType.setSelectedIndex(0);
        journeyDate.setText("");
        source.setText("");
        destination.setText("");
    }
}
