package com.example.drone.controller;

import com.example.drone.exception.*;
import com.example.drone.service.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/demo")
public class InternalDemoController {

    public static final String RESET_CONFIRMATION = "RESET_DEMO_DATA";

    private final DemoDataService demoDataService;

    public InternalDemoController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/reset-and-seed")
    public DemoScenarioResponse resetAndSeed(@RequestParam(defaultValue = "") String confirmation) {
        if (!RESET_CONFIRMATION.equals(confirmation)) {
            throw new InvalidInputException("confirmation must be RESET_DEMO_DATA");
        }

        DemoDataService.DemoScenario scenario = demoDataService.resetAndSeed();

        return new DemoScenarioResponse(
                scenario.drones().size(),
                scenario.orders().size(),
                scenario.obstacles().size(),
                1,
                scenario.plan().trips().size(),
                scenario.plan().unallocatedOrders().size()
        );
    }

    public record DemoScenarioResponse(
            int drones,
            int orders,
            int obstacles,
            int reviews,
            int trips,
            int unallocatedOrders
    ) {
    }
}
