package com.ikea;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for simple App.
 */
public class AppTest
{
    private App app;

    @BeforeEach
    void setup() {
        this.app = new App();
    }
    @DisplayName("Test testGetTimeFrame()")
    @Test
    void testGetTimeFrame() {
        Calendar calendar = Calendar.getInstance();

        // half of minute
        calendar.setTime(new Date(1680264402000L)); // Fri, 31st March 2023 12:06:42 UTC
        assertEquals(
                new Date(1680264390000L), // Fri, 31st March 2023 12:06:30 UTC
                app.getTimeFrame(calendar.getTime())
        );

        // start of minute
        calendar.setTime(new Date(1680264362000L)); // Fri, 31st March 2023 12:06:02 UTC
        assertEquals(
                new Date(1680264360000L), // Fri, 31st March 2023 12:06:00 UTC
                app.getTimeFrame(calendar.getTime())
        );

        // 0 seconds
        calendar.setTime(new Date(1680264360000L)); // Fri, 31st March 2023 12:06:00 UTC
        assertEquals(
                new Date(1680264360000L), // Fri, 31st March 2023 12:06:00 UTC
                app.getTimeFrame(calendar.getTime())
        );

        // 30s on the dot
        calendar.setTime(new Date(1680264390000L)); // Fri, 31st March 2023 12:06:30 UTC
        assertEquals(
                new Date(1680264390000L), // Fri, 31st March 2023 12:06:30 UTC
                app.getTimeFrame(calendar.getTime())
        );

        // end of minute
        calendar.setTime(new Date(1680264419000L)); // Fri, 31st March 2023 12:06:59 UTC
        assertEquals(
                new Date(1680264390000L), // Fri, 31st March 2023 12:06:30 UTC
                app.getTimeFrame(calendar.getTime())
        );
    }
}
