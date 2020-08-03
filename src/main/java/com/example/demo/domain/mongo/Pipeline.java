package com.example.demo.domain.mongo;

import com.example.demo.domain.mysql.Job;
import com.example.demo.domain.mysql.Run;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.stream.Collectors;

@Document
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pipeline {
    @Id
    private String id;

    private String nameId;

    private List<ToolIO> inputs;

    private List<ToolIO> outputs;

    private List<Step> steps;

    public List<Run> getAllRuns(Job job) {
        List<Run> runs = steps.stream().map(step -> new Run(job, step.getId())).collect(Collectors.toList());
        runs.add(new Run(job, ""));//initializer job
        return runs;
    }

    public List<Step> getNextSteps(List<Run> finishedRuns) {
        return steps.stream().filter(step -> step.isRunnable(finishedRuns)).collect(Collectors.toList());
    }

    public Step getStep(String stepId) {
        return steps.stream().filter(s -> s.getId().equals(stepId)).findFirst().orElseThrow(()-> new RuntimeException());
    }
}
