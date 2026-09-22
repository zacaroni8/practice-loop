package practiceloop;

import java.time.LocalDateTime;

public class Session {
    public final int id;
    public final Integer activityId; // nullable — no activity linked
    public final String name;
    public final String description;
    public final LocalDateTime scheduledTime;
    public final int plannedMinutes;
    public final int leadMinutes;
    public final Integer completedMinutes; // null until the session runs
    public final Integer xpAwarded; // null until claimed
    public final String status; // "scheduled" / "completed"

    public Session(int id, Integer activityId, String name, String description,
                    LocalDateTime scheduledTime, int plannedMinutes, int leadMinutes,
                    Integer completedMinutes, Integer xpAwarded, String status) {
        this.id = id;
        this.activityId = activityId;
        this.name = name;
        this.description = description;
        this.scheduledTime = scheduledTime;
        this.plannedMinutes = plannedMinutes;
        this.leadMinutes = leadMinutes;
        this.completedMinutes = completedMinutes;
        this.xpAwarded = xpAwarded;
        this.status = status;
    }

    @Override
    public String toString() {
        return scheduledTime + " - " + name + " (" + plannedMinutes + " min)";
    }
}
