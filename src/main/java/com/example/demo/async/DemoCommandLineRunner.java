package com.example.demo.async;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class DemoCommandLineRunner implements CommandLineRunner {
    private final KubeMonitor kubeMonitor;

    @Override
    public void run(String... args) {
        kubeMonitor.run();
    }

}