package com.example.demo.async;

import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ServiceExecutor {
    private ExecutorService executorService;

    public ServiceExecutor() {
        executorService = Executors.newFixedThreadPool(10);
    }

    public void runExecutor(Runnable runnableService) throws Exception{
        executorService.submit(runnableService);
    }
}
