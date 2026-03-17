package com.tracker.location_rider.bootstrap;

import com.tracker.location_rider.entity.RiderEntity;
import com.tracker.location_rider.repository.RiderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RiderDataSeeder implements ApplicationRunner {

    private final RiderRepository riderRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (riderRepository.count() > 0) {
            log.info("Rider table already contains data, skipping seed");
            return;
        }

        riderRepository.saveAll(List.of(
                buildRider("rider-london-1", "Olivia Parker", "LON-1001", LocalDate.of(1990, 3, 14)),
                buildRider("rider-london-2", "Mason Reed", "LON-1002", LocalDate.of(1988, 7, 2)),
                buildRider("rider-london-3", "Sophia Turner", "LON-1003", LocalDate.of(1994, 1, 21)),
                buildRider("rider-london-4", "Ethan Brooks", "LON-1004", LocalDate.of(1992, 11, 5)),
                buildRider("rider-london-5", "Ava Bennett", "LON-1005", LocalDate.of(1996, 5, 17)),
                buildRider("rider-london-6", "Lucas Foster", "LON-1006", LocalDate.of(1989, 9, 9)),
                buildRider("rider-london-7", "Mia Hayes", "LON-1007", LocalDate.of(1995, 12, 1)),
                buildRider("rider-london-8", "Noah Collins", "LON-1008", LocalDate.of(1991, 4, 23)),
                buildRider("rider-london-9", "Isla Morgan", "LON-1009", LocalDate.of(1993, 8, 30)),
                buildRider("rider-london-10", "Leo Ward", "LON-1010", LocalDate.of(1987, 10, 12))
        ));
        log.info("Seeded default London riders");
    }

    private RiderEntity buildRider(String identifier, String name, String licenseNumber, LocalDate dateOfBirth) {
        return RiderEntity.builder()
                .identifier(identifier)
                .name(name)
                .licenseNumber(licenseNumber)
                .dateOfBirth(dateOfBirth)
                .build();
    }
}
