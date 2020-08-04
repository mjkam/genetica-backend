package com.example.demo.domain.mongo;

import com.example.demo.domain.mysql.Run;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Step {
    private String id;
    private String label;
    private List<StepIO> in;
    private List<StepIO> out;
    private Tool tool;

    public Boolean isRunnable(List<Run> finishedRuns) {
        return in.stream().allMatch(input -> input.isReady(finishedRuns));
    }

    public Map<String,String> createEnvMap(List<V1EnvVar> kubeEnvs) {
        Map<String, String> env = new HashMap<>();
        for(StepIO io: in) {
            V1EnvVar kubeEnv = kubeEnvs.stream().filter(e -> e.getName().equals(io.getSource())).findFirst().orElseThrow(() -> new RuntimeException());
            env.put(io.getId(), kubeEnv.getValue());
        }
        return env;
    }
}
