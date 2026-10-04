package com.oasis.reservation;

import com.oasis.reservation.db.Database;
import com.oasis.reservation.ui.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {

    private Main() {
    }

    private static final Logger LOG = AppLog.get(Main.class);

    public static void main(String[] args) {
        AppLog.init();
        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
                LOG.log(Level.SEVERE, "Uncaught exception on thread " + t.getName(), e));
        Database db = Database.fromSystemProperty();
        try {
            db.initialise();
            LOG.info("Started; database = " + db.location());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not open database " + db.location(), e);
            JOptionPane.showMessageDialog(null, "Could not open the database file:\n" + db.location()
                            + "\n\nCheck that the folder is writable and the file is not damaged."
                            + "\nDetails were written to the log file.",
                    "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        AppContext ctx = AppContext.create(db);
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // fall back to the default look and feel
            }
            new LoginFrame(ctx).setVisible(true);
        });
    }
}
