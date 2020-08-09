package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.KubeJobType;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorService {
    private final JobEventHandler jobEventHandler;
    private final RunRepository runRepository;
    private final PipelineRepository pipelineRepository;
    private final NextRunFinder nextRunFinder;
    private final KubeClient kubeClient;

    public void handleJobEvent(Long runId, String sampleId, JobStatus resultStatus, String nodeName, KubeJobType kubeJobType) {
        Run run = runRepository.findRunWithJoinById(runId);
        Pipeline pipeline = pipelineRepository.findById(run.getJob().getTask().getPipelineId()).orElseThrow(() -> new RuntimeException());

        jobEventHandler.handle(run, pipeline, sampleId, kubeJobType, resultStatus, nodeName);
        List<KubeJob> kubeJobs = nextRunFinder.find(run, sampleId, pipeline);

        for(KubeJob kubeJob: kubeJobs) {
            kubeClient.runJob(kubeJob);
        }
    }
}
