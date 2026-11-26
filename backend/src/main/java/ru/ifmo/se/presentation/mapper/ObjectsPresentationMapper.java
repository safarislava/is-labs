package ru.ifmo.se.presentation.mapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import ru.ifmo.se.application.dto.result.ObjectsResult;
import ru.ifmo.se.presentation.dto.response.BatchObjectsResponse;
import ru.ifmo.se.presentation.dto.response.BookCreatureResponse;
import ru.ifmo.se.presentation.dto.response.MagicCityResponse;
import ru.ifmo.se.presentation.dto.response.RingResponse;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class ObjectsPresentationMapper {

    @Inject
    private MagicCityPresentationMapper magicCityMapper;

    @Inject
    private RingPresentationMapper ringMapper;

    @Inject
    private BookCreaturePresentationMapper bookCreatureMapper;

    public BatchObjectsResponse toResponse(ObjectsResult result) {
        if (result == null) {
            return null;
        }

        List<MagicCityResponse> cities = result.getCities() == null
                ? Collections.emptyList()
                : result.getCities().stream()
                        .map(magicCityMapper::toResponse)
                        .toList();

        List<RingResponse> rings = result.getRings() == null
                ? Collections.emptyList()
                : result.getRings().stream()
                        .map(ringMapper::toResponse)
                        .toList();

        List<BookCreatureResponse> creatures = result.getCreatures() == null
                ? Collections.emptyList()
                : result.getCreatures().stream()
                        .map(bookCreatureMapper::toResponse)
                        .toList();

        return new BatchObjectsResponse(cities, rings, creatures);
    }
}
