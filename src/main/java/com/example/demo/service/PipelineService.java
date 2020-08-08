package com.example.demo.service;

import com.example.demo.dto.KubeJob;
import com.example.demo.dto.request.RunPipelineRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PipelineService {
    private final PipelineRunner pipelineRunner;
    private final KubeClient kubeClient;

    public void runPipeline(RunPipelineRequest request) {
        List<KubeJob> kubeJobs = pipelineRunner.parseRunRequest(request);
        for(KubeJob kubeJob: kubeJobs) {
            kubeClient.runJob(kubeJob);
        }
    }
}