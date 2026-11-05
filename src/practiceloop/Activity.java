package practiceloop;

import java.util.List;

public class Activity {
    public final int id;
    public final String name;
    public final String description;
    public final int defaultPlannedMinutes;
    public final int defaultLeadMinutes;
    public final double defaultXpPerMinute;
    public final List<Integer> reminderOffsets;

    public Activity(int id, String name, String description, int defaultPlannedMinutes,
                     int defaultLeadMinutes, double defaultXpPerMinute, List<Integer> reminderOffsets) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultPlannedMinutes = defaultPlannedMinutes;
        this.defaultLeadMinutes = defaultLeadMinutes;
        this.defaultXpPerMinute = defaultXpPerMinute;
        this.reminderOffsets = reminderOffsets;
    }

    @Override
    public String toString() {
        return name;
    }
}
