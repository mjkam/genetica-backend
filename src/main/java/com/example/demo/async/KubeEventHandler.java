package com.example.demo.async;

import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJobType;
import com.example.demo.service.MonitorService;

import java.util.*;

public class KubeEventHandler implements Runnable {
    private Map<String, String> labels;
    private JobStatus resultStatus;
    private String nodeName;
    private MonitorService monitorService;

    public KubeEventHandler(Map<String, String> labels,
                            String resultStatus,
                            String nodeName,
                            MonitorService monitorService) {
        this.labels = labels;
        this.resultStatus = JobStatus.valueOf(resultStatus);
        this.nodeName = nodeName;
        this.monitorService = monitorService;
    }

    @Override
    public void run() {
        KubeJobType kubeJobType = KubeJobType.valueOf(labels.get("type"));
        Long taskId = Long.valueOf(labels.get("taskId"));
        Long jobId = Long.valueOf(labels.get("jobId"));
        Long runId = Long.valueOf(labels.get("runId"));

        if(kubeJobType.equals(KubeJobType.INITIALIZER)) {
            monitorService.handleInitializer(taskId, jobId, runId, resultStatus, nodeName);
        }

        if(kubeJobType.equals(KubeJobType.ANALYSIS)) {
            monitorService.handleAnalysis(taskId, jobId, runId, resultStatus);
        }
    }
}