package practiceloop;

public class Activity {
    public final int id;
    public final String name;
    public final String description;
    public final int defaultPlannedMinutes;
    public final int defaultLeadMinutes;
    public final double defaultXpPerMinute;

    public Activity(int id, String name, String description, int defaultPlannedMinutes,
                     int defaultLeadMinutes, double defaultXpPerMinute) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultPlannedMinutes = defaultPlannedMinutes;
        this.defaultLeadMinutes = defaultLeadMinutes;
        this.defaultXpPerMinute = defaultXpPerMinute;
    }

    @Override
    public String toString() {
        return name;
    }
}
