package practiceloop;

import java.time.LocalDateTime;
import java.util.List;

public class Session {
    public final int id;
    public final Integer activityId; // nullable — no activity linked
    public final String name;
    public final String description;
    public final LocalDateTime scheduledTime;
    public final int plannedMinutes;
    public final int leadMinutes;
    public final Integer completedMinutes; // null until the session runs
    public final int baseXp;
    public final Integer xpAwarded; // null until claimed
    public final String status; // "scheduled" / "completed"
    public final List<Integer> reminderOffsets;

    public Session(int id, Integer activityId, String name, String description,
                    LocalDateTime scheduledTime, int plannedMinutes, int leadMinutes,
                    Integer completedMinutes, int baseXp, Integer xpAwarded, String status, List<Integer> reminderOffsets) {
        this.id = id;
        this.activityId = activityId;
        this.name = name;
        this.description = description;
        this.scheduledTime = scheduledTime;
        this.plannedMinutes = plannedMinutes;
        this.leadMinutes = leadMinutes;
        this.completedMinutes = completedMinutes;
        this.baseXp = baseXp;
        this.xpAwarded = xpAwarded;
        this.status = status;
        this.reminderOffsets = reminderOffsets;
    }

    @Override
    public String toString() {
        return scheduledTime + " - " + name + " (" + plannedMinutes + " min)";
    }
}
