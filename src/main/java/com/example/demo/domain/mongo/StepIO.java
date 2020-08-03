package com.example.demo.domain.mongo;

<<<<<<< HEAD
=======
import com.example.demo.domain.mysql.Run;
>>>>>>> 43cfa364b26eb221f5df6ad76fce34a71b3a88ac
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

<<<<<<< HEAD
//    public boolean isReady(List<JobEnv> jobEnvs) {
//        return jobEnvs.stream().anyMatch(env -> env.getEnvKey().equals(source));
//    }
=======
    public boolean isReady(List<Run> finishedRuns) {
        if(source.contains(".")) return finishedRuns.stream().anyMatch(r -> r.getStepId().equals(source.split(".")[0]));
        return true;
    }
>>>>>>> 43cfa364b26eb221f5df6ad76fce34a71b3a88ac
}
