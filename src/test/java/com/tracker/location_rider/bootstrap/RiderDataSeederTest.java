package com.tracker.location_rider.bootstrap;

import com.tracker.location_rider.entity.RiderEntity;
import com.tracker.location_rider.repository.RiderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiderDataSeederTest {

    @Mock
    private RiderRepository riderRepository;

    @Mock
    private ApplicationArguments args;

    @InjectMocks
    private RiderDataSeeder seeder;

    @Test
    void shouldSeedDefaultRidersWhenRepositoryIsEmpty() {
        when(riderRepository.count()).thenReturn(0L);

        seeder.run(args);

        ArgumentCaptor<List<RiderEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(riderRepository).saveAll(captor.capture());
        List<RiderEntity> seeded = captor.getValue();
        assertThat(seeded)
                .hasSize(10)
                .allSatisfy(rider -> assertThat(rider.getIdentifier()).isNotBlank());
    }

    @Test
    void shouldSkipSeedingWhenRepositoryAlreadyContainsData() {
        when(riderRepository.count()).thenReturn(5L);

        seeder.run(args);

        verify(riderRepository, never()).saveAll(any());
    }
}

