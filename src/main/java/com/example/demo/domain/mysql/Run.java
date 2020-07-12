package com.example.demo.domain.mysql;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
public class Run {
    @Id
    @GeneratedValue
    @Column(name = "run_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    private String stepId;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;
}
