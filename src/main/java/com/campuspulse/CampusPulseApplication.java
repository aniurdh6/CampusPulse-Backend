package com.campuspulse;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusPulseApplication {

    public static void main(String[] args) {
        // Booking times are campus-local, so pin the JVM zone before anything reads the clock.
        String zone = System.getenv().getOrDefault("APP_TIMEZONE", "Asia/Kolkata");
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(CampusPulseApplication.class, args);
    }
}
