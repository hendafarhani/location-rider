package com.tracker.location_rider.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@Entity
@Table(name = "RIDERS")
@NoArgsConstructor
@AllArgsConstructor
public class RiderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "identifier", unique = true)
    private String identifier;

    @Column(name = "license_number", unique = true)
    private String licenseNumber;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
}
