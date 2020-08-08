package com.example.demo.async;

import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJobType;
import com.example.demo.service.MonitorService;
import io.kubernetes.client.openapi.models.V1EnvVar;

import java.util.*;

public class KubeEventHandler implements Runnable {
    private Map<String, String> labels;
    private JobStatus resultStatus;
    private String nodeName;
    private MonitorService monitorService;
    private List<V1EnvVar> kubeEnvs;

    public KubeEventHandler(Map<String, String> labels,
                            String resultStatus,
                            String nodeName,
                            List<V1EnvVar> kubeEnvs,
                            MonitorService monitorService) {
        this.labels = labels;
        this.resultStatus = JobStatus.valueOf(resultStatus);
        this.nodeName = nodeName;
        this.kubeEnvs = kubeEnvs;
        this.monitorService = monitorService;
    }

    @Override
    public void run() {
        KubeJobType kubeJobType = KubeJobType.valueOf(labels.get("type"));
        Long taskId = Long.valueOf(labels.get("taskId"));
        Long jobId = Long.valueOf(labels.get("jobId"));
        Long runId = Long.valueOf(labels.get("runId"));
        String sampleId = null;
        Optional<V1EnvVar> env  = kubeEnvs.stream().filter(e -> e.getName().equals("sample")).findAny();
        if(env.isPresent()) {
            sampleId = env.get().getValue();
        }

        monitorService.handleJobEvent(runId, sampleId, resultStatus, nodeName, kubeJobType);
    }
}