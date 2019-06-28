package de.tudresden.inf.mci.brailleplot.exporter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BrailleGerman8DotsTest {

    @Test
    public void testNullIsThrownInPrettyPrintCLI(){
        Assertions.assertThrows(NullPointerException.class, () -> {
            BrailleGerman8Dots B = new BrailleGerman8Dots();
            B.getValue(null);
        });
    }
}