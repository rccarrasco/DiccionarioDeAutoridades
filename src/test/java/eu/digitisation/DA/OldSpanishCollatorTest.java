/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.digitisation.DA;

import java.text.Collator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rafa
 */
public class OldSpanishCollatorTest {
    
  
    /**
     * Test of getInstance method, of class OldSpanishCollator.
     */
    @Test
    public void testOrder() {
        System.out.println("Lexicographic order with OldSpanishCollator");
        Collator collator =  OldSpanishCollator.getInstance();
        assertEquals(collator.compare("cuna", "chita"), -1); // this is modern, indeed.
        assertEquals(collator.compare("lápiz", "leer"), -1); // this is modern, indeed
    }
    
}
