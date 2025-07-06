package com.tracker.location_rider.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location implements Serializable {
    private double latitude;
    private double longitude;
    private double radius;
}
