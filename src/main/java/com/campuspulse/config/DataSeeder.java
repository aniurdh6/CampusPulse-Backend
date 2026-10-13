package com.campuspulse.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.campuspulse.model.Booking;
import com.campuspulse.model.BookingStatus;
import com.campuspulse.model.CampusResource;
import com.campuspulse.model.MaintenanceRequest;
import com.campuspulse.model.Priority;
import com.campuspulse.model.ResourceType;
import com.campuspulse.model.Role;
import com.campuspulse.model.User;
import com.campuspulse.repository.BookingRepository;
import com.campuspulse.repository.MaintenanceRepository;
import com.campuspulse.repository.ResourceRepository;
import com.campuspulse.repository.UserRepository;
import com.campuspulse.security.PasswordHasher;
import com.campuspulse.service.ActivityLogService;

/** Fills an empty database with demo accounts, resources and a little activity. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final ResourceRepository resources;
    private final BookingRepository bookings;
    private final MaintenanceRepository maintenance;
    private final ActivityLogService logs;
    private final PasswordHasher hasher;
    private final Clock clock;

    public DataSeeder(UserRepository users, ResourceRepository resources, BookingRepository bookings,
                      MaintenanceRepository maintenance, ActivityLogService logs, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.resources = resources;
        this.bookings = bookings;
        this.maintenance = maintenance;
        this.logs = logs;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        users.save(user("Campus Admin", "admin@campus.edu", "Admin@123", Role.ADMIN, "Administration"));
        User faculty = users.save(user("Dr. Meera Rao", "faculty@campus.edu", "Faculty@123", Role.FACULTY, "ECE"));
        User student = users.save(user("Demo Student", "student@campus.edu", "Student@123", Role.STUDENT, "ECE"));

        CampusResource room101 = resource("ECE Classroom 101", ResourceType.CLASSROOM, "Block A, floor 1", 60,
                "Projector,Whiteboard,AC", "Tiered seating, ceiling projector and a lectern.");
        resource("Classroom 204", ResourceType.CLASSROOM, "Block A, floor 2", 40,
                "Whiteboard", "Small classroom with movable desks.");
        resource("Seminar Hall", ResourceType.CLASSROOM, "Block B, ground floor", 150,
                "Projector,Sound system,AC,Podium", "Used for guest lectures and demo days.");
        CampusResource embedded = resource("Embedded Systems Lab", ResourceType.LAB, "Block C, floor 1", 30,
                "Computers,Oscilloscopes,Soldering stations", "Microcontroller boards and bench power supplies at every seat.");
        resource("Computer Lab 2", ResourceType.LAB, "Block B, floor 1", 50,
                "Computers,AC,Projector", "Fifty workstations with the full development toolchain.");
        resource("Electronics Lab", ResourceType.LAB, "Block C, ground floor", 30,
                "Oscilloscopes,Function generators,Power supplies", "Analog and digital circuit benches.");
        CampusResource projector = resource("Portable projector", ResourceType.EQUIPMENT, "Equipment store", 1,
                "Projector,HDMI", "Collect and return at the equipment store desk.");
        resource("Digital oscilloscope kit", ResourceType.EQUIPMENT, "Equipment store", 1,
                "Oscilloscope,Probes", "Two-channel scope with probes.");
        resource("Arduino starter kit", ResourceType.EQUIPMENT, "Equipment store", 1,
                "Microcontroller,Sensors", "Board, breadboard and a sensor pack.");
        resource("Camera and tripod", ResourceType.EQUIPMENT, "Equipment store", 1,
                "Camera,Tripod", "For project demos and event recordings.");

        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        booking(room101, faculty, tomorrow, LocalTime.of(10, 0), LocalTime.of(12, 0), "Signals and systems lecture", BookingStatus.CONFIRMED);
        booking(embedded, student, tomorrow, LocalTime.of(14, 0), LocalTime.of(16, 0), "Mini project testing", BookingStatus.PENDING);
        booking(projector, faculty, tomorrow, LocalTime.of(9, 0), LocalTime.of(11, 0), "Guest talk", BookingStatus.CONFIRMED);

        MaintenanceRequest issue = new MaintenanceRequest();
        issue.setReporter(student);
        issue.setResource(embedded);
        issue.setLocation("Embedded Systems Lab, bench 4");
        issue.setTitle("Oscilloscope channel 2 shows no signal");
        issue.setDescription("Channel 2 stays flat even with a known good probe and signal.");
        issue.setPriority(Priority.HIGH);
        maintenance.save(issue);

        logs.log("System", "SEED", "Created demo accounts, resources and sample activity");
        log.info("Demo data created. Sign in with admin@campus.edu / Admin@123, "
                + "faculty@campus.edu / Faculty@123 or student@campus.edu / Student@123");
    }

    private User user(String name, String email, String password, Role role, String department) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(hasher.hash(password));
        u.setRole(role);
        u.setDepartment(department);
        return u;
    }

    private CampusResource resource(String name, ResourceType type, String location, int capacity,
                                    String features, String description) {
        CampusResource r = new CampusResource();
        r.setName(name);
        r.setType(type);
        r.setLocation(location);
        r.setCapacity(capacity);
        r.setFeatures(features);
        r.setDescription(description);
        return resources.save(r);
    }

    private void booking(CampusResource resource, User user, LocalDate day, LocalTime start, LocalTime end, String purpose,
                         BookingStatus status) {
        Booking b = new Booking();
        b.setResource(resource);
        b.setUser(user);
        b.setBookingDate(day);
        b.setStartTime(start);
        b.setEndTime(end);
        b.setPurpose(purpose);
        b.setStatus(status);
        bookings.save(b);
    }
}
