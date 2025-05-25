package com.simpligility.android.morse;

import junit.framework.TestCase;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MorseCodeConverterInAppTest is the unit test suite for {@link com.simpligility.android.morse.MorseCodeConverter}.
 * This class is a duplicate of MorseCodeConverterInAppTest showing how a pure unit test can run within the same project
 * as the Android application.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class MorseCodeConverterInAppTest extends TestCase {

    /**
     * Test the timing parameters for signals.
     */
    public void testSetup() {
        assertEquals(MorseCodeConverter.GAP, 100);
        assertEquals(MorseCodeConverter.DASH, 300);
        assertEquals(MorseCodeConverter.DOT, 100);
    }

    /**
     * Test the string "SOS".
     */
    public void testSOS() {
        long[] sosArrayExpected = new long[]{0, 100, 100, 100, 100, 100, 300, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 100, 0};
        long[] actual = MorseCodeConverter.pattern("SOS");
        assertArrayEquals(sosArrayExpected, actual);
    }

    public void testCaseSensitivity() {
        assertArrayEquals(MorseCodeConverter.pattern("sos"), MorseCodeConverter.pattern("SOS"));
        assertArrayEquals(MorseCodeConverter.pattern("sOs"), MorseCodeConverter.pattern("SOS"));
    }

    public void testSomeNumbers() {
        long[] expected = new long[]{0, 100, 100, 300, 100, 300, 100, 300, 100, 300, 300, 100, 100, 100, 100, 300, 100, 300, 100, 300, 300,
                100, 100, 100, 100, 100, 100, 300, 100, 300, 0};
        long[] actual = MorseCodeConverter.pattern("123");
        assertArrayEquals(expected, actual);
    }

    public void testWhitespaceTreatment() {
        long[] expected = new long[]{0, 100, 100, 100, 100, 100, 100, 100, 700, 100, 100, 300, 100, 300, 0};
        long[] actual = MorseCodeConverter.pattern("H W");
        assertArrayEquals(expected, actual);
    }

    public void testChars() {
        assertArrayEquals(new long[]{100, 100, 300}, MorseCodeConverter.pattern('A'));

        assertArrayEquals(new long[]{300, 100, 300}, MorseCodeConverter.pattern('m'));

        assertArrayEquals(new long[]{300, 100, 300}, MorseCodeConverter.pattern('M'));

        assertArrayEquals(new long[]{100}, MorseCodeConverter.pattern(' '));

        assertArrayEquals(new long[]{100, 100, 100, 100, 100, 100, 100, 100, 300}, MorseCodeConverter.pattern('4'));

        assertArrayEquals(new long[]{100}, MorseCodeConverter.pattern('?'));
    }
}
