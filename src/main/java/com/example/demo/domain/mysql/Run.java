package com.example.demo.domain.mysql;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
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

    public boolean isFailed() {
        if(status.equals(JobStatus.Failed)) return true;
        return false;
    }

    public boolean isRunning() {
        if(status.equals(JobStatus.Running)) return true;
        return false;
    }

    public List<JobEnv> getInputEnvs(List<JobEnv> envs) {

    }

    public List<JobEnv> getOutputEnvs(List<JobEnv> envs) {
    }
}
