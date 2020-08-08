package com.example.demo.dto;

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
    private Long jobId;
    private Long runId;
    private KubeJobType kubeJobType;
    private List<V1EnvVar> kubeEnvs;
    private String imageName;
    private List<String> command;

    KubeJob(Long jobId, Long runId, KubeJobType kubeJobType, List<V1EnvVar> kubeEnvs, String imageName, List<String> command) {
        this.jobId = jobId;
        this.runId = runId;
        this.kubeJobType = kubeJobType;
        this.kubeEnvs = kubeEnvs;
        this.imageName = imageName;
        this.command = command;
    }

    public static KubeJob createInitializer(Long jobId, Long runId, List<V1EnvVar> envs) {
        String baseImageName = "338282184009.dkr.ecr.ap-northeast-2.amazonaws.com/myrepo:genetica_base";
        List<String> commands = Arrays.asList("rm -rf *");
        return new KubeJob(jobId, runId, KubeJobType.INITIALIZER, envs, baseImageName, commands);
    }

    public static KubeJob createJob(Long jobId, Long runId, List<V1EnvVar> envs, String imageName, List<String> commands) {
        return new KubeJob(jobId, runId, KubeJobType.ANALYSIS, envs, imageName, commands);
    }

    public String getCommandStr() {
        return command.stream().collect(Collectors.joining(" && "));
    }

    public Map<String, String> getLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("type", kubeJobType.toString());
        labels.put("jobId", String.valueOf(jobId));
        labels.put("runId", String.valueOf(runId));
        return labels;
    }

    public String getImageName() {
        return imageName;
    }

    public String getJobName() {
        return String.format("%d-%d", jobId, runId);
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
