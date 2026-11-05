package practiceloop;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

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

    public static List<Integer> parseOffsets(String csv)
    {
        if(csv == null || csv.equals("")) {return List.of(5);}
        List<String> temp = new ArrayList<>(Arrays.asList(csv.split(",")));
        List<Integer> returnList = new ArrayList<Integer>();
        while(!temp.isEmpty())
        {
            returnList.add(Integer.parseInt(temp.remove(0)));
        }
        return returnList;
    }

    public static String joinOffsets(List<Integer> offsets)
    {
        String returnString = "";
        for(Integer i : offsets)
        {
            returnString += Integer.toString(i) + ",";
        }
        returnString = returnString.substring(0,returnString.length()); //get rid of extraneous comma
        return returnString;
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
                "base_xp INTEGER," +
                "status TEXT NOT NULL DEFAULT 'scheduled')";
        String settings = "CREATE TABLE IF NOT EXISTS settings (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                "equipped_color TEXT)";
        
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute(activities);
            st.execute(sessions);
            st.execute(settings);
            st.execute("INSERT OR IGNORE INTO settings (id, equipped_color) VALUES (1, NULL)");
            addColumnIfMissing(st, "activities", "default_reminder_offsets", "TEXT");
            addColumnIfMissing(st, "sessions", "reminder_offsets", "TEXT");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set up database", e);
        }
    }

    /**
     * CREATE TABLE IF NOT EXISTS only helps for a brand-new table - it does nothing
     * for a table that already existed before this column was added (e.g. Zac's,
     * David's, or the mentor's existing practiceloop.db). ALTER TABLE ADD COLUMN has
     * no "IF NOT EXISTS" of its own, so this just tries it and swallows the specific
     * "duplicate column" failure that means it was already added by a previous run.
     */
    private void addColumnIfMissing(Statement st, String table, String column, String type) throws SQLException {
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
        } catch (SQLException e) {
            if (!e.getMessage().contains("duplicate column name")) {
                throw e;
            }
        }
    }

    public Activity createActivity(String name, String description, int plannedMinutes,
                                    int leadMinutes, double xpPerMinute, List<Integer> reminderOffsets) {
        String sql = "INSERT INTO activities (name, description, default_planned_minutes, " +
                "default_lead_minutes, default_xp_per_minute, reminder_offsets) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setInt(3, plannedMinutes);
            ps.setInt(4, leadMinutes);
            ps.setDouble(5, xpPerMinute);
            ps.setString(6, joinOffsets(reminderOffsets));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new Activity(id, name, description, plannedMinutes, leadMinutes, xpPerMinute, reminderOffsets);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create activity", e);
        }
    }

    public Session createSession(Integer activityId, String name, String description,
                                  LocalDateTime scheduledTime, int plannedMinutes, int leadMinutes, int baseXp, List<Integer> reminderOffsets) {
        String sql = "INSERT INTO sessions (activity_id, name, description, scheduled_time, " +
                "planned_minutes, lead_minutes, base_xp, status, reminder_offsets) VALUES (?, ?, ?, ?, ?, ?, ?, 'scheduled', ?)";
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
            ps.setInt(7, baseXp);
            ps.setString(8, joinOffsets(reminderOffsets));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new Session(id, activityId, name, description, scheduledTime,
                        plannedMinutes, leadMinutes, null, baseXp, null, "scheduled", reminderOffsets);
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

    public void claimXp(int sessionId, int xpAwarded)
    {
        String sql = "UPDATE sessions SET xp_awarded = ? WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql))
        {
            ps.setInt(1, xpAwarded);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Failed to claim XP", e);
        }
    }

    public int getTotalXp()
    {
        String sql = "SELECT SUM(xp_awarded) FROM sessions";
        int total = 0;
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql))
        {
            while (rs.next())
            {
                total += rs.getInt(1);
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeException("Failed to get total XP", e);
        }
        return total;
    }

    public List<Session> listSessions() {
        String sql = "SELECT id, activity_id, name, description, scheduled_time, planned_minutes, " +
                "lead_minutes, completed_minutes, base_xp, xp_awarded, status, reminder_offsets " +
                "FROM sessions ORDER BY scheduled_time";
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
                int baseXp = rs.getInt("base_xp");
                int rawXp = rs.getInt("xp_awarded");
                Integer xpAwarded = rs.wasNull() ? null : rawXp;
                String status = rs.getString("status");
                List<Integer> reminderOffsets = SessionStore.parseOffsets(rs.getString("reminder_offsets"));
                result.add(new Session(id, activityId, name, description, scheduledTime,
                        plannedMinutes, leadMinutes, completedMinutes, baseXp, xpAwarded, status, reminderOffsets));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list sessions", e);
        }
        return result;
    }

    public List<Activity> listActivities() {
        String sql = "SELECT id, name, description, default_planned_minutes, default_lead_minutes, " +
                "default_xp_per_minute, default_reminder_offsets FROM activities ORDER BY name";
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
                        rs.getDouble("default_xp_per_minute"),
                        SessionStore.parseOffsets(rs.getString("default_reminder_offsets"))
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list activities", e);
        }
        return result;
    }

    public String getEquippedColor()
    {
        String sql = "SELECT equipped_color FROM settings WHERE id = 1";
        String returnedColor = null;
        try(Connection c = connect(); Statement st = c.createStatement();
            ResultSet rs = st.executeQuery(sql))
        {
            if(rs.next())
            {
                return rs.getString("equipped_color");
            }
        }
        catch(SQLException e)
        {
            throw new RuntimeException("Failed to find equipped color", e);
        }
        return returnedColor;
    }

    public void setEquippedColor(String hexColor)
    {
        String sql = "UPDATE settings SET equipped_color = ? WHERE id = 1";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql))
        {
            ps.setString(1, hexColor);
            ps.executeUpdate();
        }
        catch(SQLException e)
        {
            throw new RuntimeException("Failed to update color", e);
        }
    }
}
