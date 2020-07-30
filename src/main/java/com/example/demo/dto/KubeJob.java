package com.example.demo.dto;

import com.example.demo.domain.mysql.JobEnv;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class KubeJob {
    private Long taskId;
    private Long jobId;
    private Long runId;
    private KubeJobType kubeJobType;
    private List<JobEnv> jobEnvs;
    private String imageName;
    private List<String> command;

    public KubeJob(Long taskId, Long jobId, Long runId, KubeJobType kubeJobType, List<JobEnv> jobEnvs, String imageName, List<String> command) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.runId = runId;
        this.kubeJobType = kubeJobType;
        this.jobEnvs = jobEnvs;
        this.imageName = imageName;
        this.command = command;
    }

    public KubeJob(Long taskId, Long jobId, Long runId, KubeJobType kubeJobType, List<JobEnv> jobEnvs) {
        this.taskId = taskId;
        this.jobId = jobId;
        this.runId = runId;
        this.kubeJobType = kubeJobType;
        this.jobEnvs = jobEnvs;
        this.imageName = "338282184009.dkr.ecr.ap-northeast-2.amazonaws.com/myrepo:genetica_base";
        this.command = Arrays.asList("rm -rf *");
    }

    public String getCommandStr() {
        return command.stream().collect(Collectors.joining(" && "));
    }

    public List<V1EnvVar> getEnvVars() {
        return jobEnvs.stream().map(e -> {
            V1EnvVar env = new V1EnvVar();
            env.setName(e.getEnvKey());
            env.setValue(e.getEnvVal());
            return env;
        }).collect(Collectors.toList());
    }

    public Map<String, String> getLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("type", kubeJobType.toString());
        labels.put("taskId", String.valueOf(taskId));
        labels.put("jobId", String.valueOf(jobId));
        labels.put("runId", String.valueOf(runId));
        return labels;
    }

    public String getImageName() {
        return imageName;
    }

    public String getJobName() {
        return String.format("%d-%d-%d", taskId, jobId, runId);
    }

    public Map<String, String> getNodeSelector() {
        Map<String, String> nodeSelector = new HashMap<>();
        if(!kubeJobType.equals(KubeJobType.INITIALIZER)) {
            nodeSelector.put("jobId", String.valueOf(jobId));
        }
        return nodeSelector;
    }

    public Map<String, Quantity> getResourceLimit() {
        Map<String, Quantity> limit = new HashMap<>();
        if(kubeJobType.equals(KubeJobType.INITIALIZER)) {
            limit.put("cpu", new Quantity("3"));
            limit.put("memory", new Quantity("5000000Ki"));
        }
        return limit;
    }
}
