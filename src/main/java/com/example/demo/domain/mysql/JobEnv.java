package com.example.demo.domain.mysql;

import lombok.*;

import javax.persistence.*;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
public class JobEnv {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    private Long runId;

    private Boolean isValid;

    private String envKey;

    private String envVal;

    public JobEnv(Job job, String key, String value, boolean isValid) {
        this.job = job;
        this.envKey = key;
        this.envVal = value;
        this.isValid = isValid;
    }
}
