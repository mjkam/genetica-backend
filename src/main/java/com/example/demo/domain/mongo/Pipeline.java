package com.example.demo.domain.mongo;

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

    public List<String> getStepIds() {
        return steps.stream().map(step -> step.getId()).collect(Collectors.toList());
    }
}
