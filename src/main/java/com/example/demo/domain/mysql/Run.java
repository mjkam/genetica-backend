package com.example.demo.domain.mysql;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Run {
    @Id
    @GeneratedValue
    @Column(name = "run_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    private String stepId;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    public Run(Job job, String stepId) {
        this.job = job;
        this.stepId = stepId;
        this.status = JobStatus.Queued;
    }
}
