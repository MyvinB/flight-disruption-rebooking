package com.eurowings.disruption.controller;

import com.eurowings.disruption.dto.DisruptionOptionsResponse;
import com.eurowings.disruption.service.DisruptionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DisruptionController {

    private final DisruptionService disruptionService;

    public DisruptionController(DisruptionService disruptionService) {
        this.disruptionService = disruptionService;
    }

    @GetMapping("/disruption-options")
    public DisruptionOptionsResponse getDisruptionOptions() {
        return disruptionService.getDisruptionOptions();
    }
}
