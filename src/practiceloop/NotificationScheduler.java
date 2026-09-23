package practiceloop;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fires real OS notifications for scheduled sessions. Call
 * checkAndFireNotifications() periodically (Main wires this to a
 * JavaFX Timeline) - it figures out on its own which notifications
 * are due and makes sure each one only fires once.
 */
public class NotificationScheduler {
    private final SessionStore store;
    private TrayIcon trayIcon;
    private final Set<Integer> firedReminder = new HashSet<>();
    private final Set<Integer> firedStartingNow = new HashSet<>();

    public NotificationScheduler(SessionStore store) {
        this.store = store;
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            trayIcon = new TrayIcon(createIconImage(), "Practice Loop");
            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                trayIcon = null;
            }
        }
    }

    private static Image createIconImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillOval(0, 0, 16, 16);
        g.dispose();
        return image;
    }

    public void checkAndFireNotifications() {
        if (trayIcon == null) return;

        LocalDateTime now = LocalDateTime.now();
        for (Session s : store.listSessions()) {
            if (!"scheduled".equals(s.status)) continue;

            LocalDateTime reminderTime = s.scheduledTime.minusMinutes(s.leadMinutes);

            if (!now.isBefore(reminderTime) && now.isBefore(s.scheduledTime)
                    && firedReminder.add(s.id)) {
                long minutesUntil = Duration.between(now, s.scheduledTime).toMinutes();
                trayIcon.displayMessage(
                        s.name,
                        s.description + " - starts at " + s.scheduledTime + " (" + minutesUntil + " min)",
                        TrayIcon.MessageType.INFO
                );
            }

            if (!now.isBefore(s.scheduledTime) && firedStartingNow.add(s.id)) {
                trayIcon.displayMessage(s.name, "Starting now", TrayIcon.MessageType.INFO);
            }
        }
    }

    public void fireCompletion(Session s) {
        if (trayIcon == null) return;
        trayIcon.displayMessage(s.name, "Session complete", TrayIcon.MessageType.INFO);
    }
}
