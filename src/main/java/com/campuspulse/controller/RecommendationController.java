package com.campuspulse.controller;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campuspulse.dto.Dtos.RecommendationDto;
import com.campuspulse.dto.Dtos.RecommendationRequest;
import com.campuspulse.service.RecommendationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendations;

    public RecommendationController(RecommendationService recommendations) {
        this.recommendations = recommendations;
    }

    @PostMapping
    public List<RecommendationDto> recommend(@Valid @RequestBody RecommendationRequest request) {
        return recommendations.recommend(request);
    }
}
