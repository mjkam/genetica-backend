package com.example.demo.domain.mysql;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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

    public boolean isSucceeded() {
        if(status.equals(JobStatus.Succeeded)) return true;
        return false;
    }

    public void changeStatus(JobStatus status) {
        this.status = status;

        if(status.equals(JobStatus.Succeeded) || status.equals(JobStatus.Failed)) {
            this.finishTime = LocalDateTime.now();
        } else if(status.equals(JobStatus.Running)) {
            this.startTime = LocalDateTime.now();
        }
    }
}
