package com.example.drone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final DemoDataService demoDataService;
    private final boolean autoSeedEnabled;

    public DemoDataInitializer(
            DemoDataService demoDataService,
            @Value("${drone.demo.auto-seed:true}") boolean autoSeedEnabled
    ) {
        this.demoDataService = demoDataService;
        this.autoSeedEnabled = autoSeedEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!autoSeedEnabled) {
            return;
        }

        if (demoDataService.seedInitialScenarioIfEmpty()) {
            LOGGER.info("Cenario demo inicial criado porque o banco estava vazio.");
        }
    }
}
