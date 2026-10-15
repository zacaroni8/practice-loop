package practiceloop;

public class LevelCalculator
{
    public int xpCostOfLevel(int level)
    {
        return (int)Math.round(35*Math.log(level+1));
    }

    public int levelForXp(int totalXp)
    {
        int remainingXp = totalXp;
        int currentLevel = 1;
        while(remainingXp>0 && currentLevel < 30)
        {
            remainingXp-=xpCostOfLevel(currentLevel+1);
            currentLevel++;
        }
        if (remainingXp<0)
            currentLevel--;
        return currentLevel;
        
    }
}