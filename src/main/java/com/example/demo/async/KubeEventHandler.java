package com.example.demo.async;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mysql.Job;
import com.example.demo.domain.mysql.JobEnv;
import com.example.demo.domain.mysql.JobFile;
import com.example.demo.domain.mysql.Run;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.JobEnvRepository;
import com.example.demo.repository.mysql.JobFileRepository;
import com.example.demo.repository.mysql.JobRepository;
import com.example.demo.repository.mysql.RunRepository;
import com.example.demo.service.CommandLineService;
import com.example.demo.service.KubeClientService;
import com.example.demo.service.MonitorService;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import org.apache.commons.collections4.ListUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KubeEventHandler implements Runnable {
    private Map<String, String> labels;
    private String resultStatus;
    private String nodeName;
    private MonitorService monitorService;

    @PersistenceContext
    EntityManager em;

    public KubeEventHandler(Map<String, String> labels,
                            String resultStatus,
                            String nodeName,
                            MonitorService monitorService) {
        this.labels = labels;
        this.resultStatus = resultStatus;
        this.nodeName = nodeName;
        this.monitorService = monitorService;
    }
    @Override
    public void run() {
        String type = labels.get("type");
        Long taskId = Long.valueOf(labels.get("taskId"));
        Long jobId = Long.valueOf(labels.get("jobId"));
        Long runId = Long.valueOf(labels.get("runId"));

        if(type.equals("initializer")) {
            monitorService.handleInitializer(taskId, jobId, this.resultStatus, nodeName);
        }

        if(type.equals("job")) {
            monitorService.handleJobResult(taskId, jobId, runId, this.resultStatus);
        }
    }
}