package com.example.demo.domain.mongo;

import lombok.*;

import java.util.List;

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
    private Tool run;
}
