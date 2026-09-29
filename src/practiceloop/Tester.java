package practiceloop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
public class Tester
{
    @BeforeAll
    static void initAll() {
        
    }

    
    @BeforeEach
    void init() {
        
    }

    @Test
    public void xpCalculatorZeroCompleted()
    {
        XpCalculator calc = new XpCalculator();
        assertEquals(0,calc.computeXp(40,20,0));
    }

    @Test
    public void xpCalculatorQuarterCompleted()
    {
        XpCalculator calc = new XpCalculator();
        assertEquals(6,calc.computeXp(40,20,5));
    }

    @Test
    public void xpCalculatorHalfCompleted()
    {
        XpCalculator calc = new XpCalculator();
        assertEquals(16,calc.computeXp(40,20,10));
    }

    @Test
    public void xpCalculatorThreeQuartersCompleted()
    {
        XpCalculator calc = new XpCalculator();
        assertEquals(27,calc.computeXp(40,20,15));
    }

    @Test
    public void xpCalculatorCompleted()
    {
        XpCalculator calc = new XpCalculator();
        assertEquals(40,calc.computeXp(40,20,20));
    }

}