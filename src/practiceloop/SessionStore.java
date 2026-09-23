package practiceloop;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The only class in this project that talks to JDBC/SQLite directly.
 */
public class SessionStore {
    private final String url;

    public SessionStore(String dbPath) {
        this.url = "jdbc:sqlite:" + dbPath;
        createTablesIfNeeded();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void createTablesIfNeeded() {
        String activities = "CREATE TABLE IF NOT EXISTS activities (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "default_planned_minutes INTEGER," +
                "default_lead_minutes INTEGER," +
                "default_xp_per_minute REAL)";
        String sessions = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "activity_id INTEGER REFERENCES activities(id)," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "scheduled_time TEXT NOT NULL," +
                "planned_minutes INTEGER NOT NULL," +
                "lead_minutes INTEGER," +
                "completed_minutes INTEGER," +
                "xp_awarded INTEGER," +
                "status TEXT NOT NULL DEFAULT 'scheduled')";
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute(activities);
            st.execute(sessions);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set up database", e);
        }
    }

    public Activity createActivity(String name, String description, int plannedMinutes,
                                    int leadMinutes, double xpPerMinute) {
        String sql = "INSERT INTO activities (name, description, default_planned_minutes, " +
                "default_lead_minutes, default_xp_per_minute) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt(3, plannedMinutes);
            ps.setInt(4, leadMinutes);
            ps.setDouble(5, xpPerMinute);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new Activity(id, name, description, plannedMinutes, leadMinutes, xpPerMinute);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create activity", e);
        }
    }

    public Session createSession(Integer activityId, String name, String description,
                                  LocalDateTime scheduledTime, int plannedMinutes, int leadMinutes) {
        String sql = "INSERT INTO sessions (activity_id, name, description, scheduled_time, " +
                "planned_minutes, lead_minutes, status) VALUES (?, ?, ?, ?, ?, ?, 'scheduled')";
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (activityId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, activityId);
            }
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, scheduledTime.toString());
            ps.setInt(5, plannedMinutes);
            ps.setInt(6, leadMinutes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new Session(id, activityId, name, description, scheduledTime,
                        plannedMinutes, leadMinutes, null, null, "scheduled");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create session", e);
        }
    }

    public void completeSession(int sessionId, int completedMinutes) {
        String sql = "UPDATE sessions SET status = 'completed', completed_minutes = ? WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, completedMinutes);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to complete session", e);
        }
    }

    public List<Session> listSessions() {
        String sql = "SELECT id, activity_id, name, description, scheduled_time, planned_minutes, " +
                "lead_minutes, completed_minutes, xp_awarded, status FROM sessions ORDER BY scheduled_time";
        List<Session> result = new ArrayList<>();
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                int rawActivityId = rs.getInt("activity_id");
                Integer activityId = rs.wasNull() ? null : rawActivityId;
                String name = rs.getString("name");
                String description = rs.getString("description");
                LocalDateTime scheduledTime = LocalDateTime.parse(rs.getString("scheduled_time"));
                int plannedMinutes = rs.getInt("planned_minutes");
                int leadMinutes = rs.getInt("lead_minutes");
                int rawCompleted = rs.getInt("completed_minutes");
                Integer completedMinutes = rs.wasNull() ? null : rawCompleted;
                int rawXp = rs.getInt("xp_awarded");
                Integer xpAwarded = rs.wasNull() ? null : rawXp;
                String status = rs.getString("status");
                result.add(new Session(id, activityId, name, description, scheduledTime,
                        plannedMinutes, leadMinutes, completedMinutes, xpAwarded, status));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list sessions", e);
        }
        return result;
    }

    public List<Activity> listActivities() {
        String sql = "SELECT id, name, description, default_planned_minutes, default_lead_minutes, " +
                "default_xp_per_minute FROM activities ORDER BY name";
        List<Activity> result = new ArrayList<>();
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new Activity(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("default_planned_minutes"),
                        rs.getInt("default_lead_minutes"),
                        rs.getDouble("default_xp_per_minute")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list activities", e);
        }
        return result;
    }
}
