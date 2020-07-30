package com.example.demo.domain.mongo;

import com.example.demo.domain.mysql.JobEnv;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class StepIO {
    private String id;
    private String source;
    private String script;

    public boolean isReady(List<JobEnv> jobEnvs) {
        return jobEnvs.stream().anyMatch(env -> env.getEnvKey().equals(source));
    }
}
