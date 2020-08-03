package com.example.demo.domain.mongo;

import com.example.demo.domain.mysql.Run;
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

    public boolean isReady(List<Run> finishedRuns) {
        if(source.contains(".")) return finishedRuns.stream().anyMatch(r -> r.getStepId().equals(source.split(".")[0]));
        return true;
    }
}
