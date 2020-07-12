package com.example.demo.domain.mongo;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StepIO {
    private String id;
    private String source;
}
