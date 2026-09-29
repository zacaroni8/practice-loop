package practiceloop;

public class XpCalculator
{
    public int computeXp(int recommendedXp, int plannedMinutes, int completedMinutes)
    {
        int computedXp = (int) Math.floor(recommendedXp*Math.pow((completedMinutes/(double)plannedMinutes),1.3));
        return computedXp;
    }
}