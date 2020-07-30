package com.example.demo.domain.mysql;

import com.example.demo.domain.mongo.Step;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
public class Job {
    @Id
    @GeneratedValue
    @Column(name = "job_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "job")
    private List<Run> runs;

    private String name;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    public void changeStatus(JobStatus status) {
        if(status.equals(JobStatus.Running)) {
            status = JobStatus.Running;
            startTime = LocalDateTime.now();
        } else if(status.equals(JobStatus.Failed)) {

        } else if(status.equals(JobStatus.Succeeded)) {

        }
    }

    public JobStatus getJobStatus() {
        if(runs.stream().anyMatch(r -> r.isFailed())) return JobStatus.Failed;
        if(runs.stream().anyMatch(r -> r.isRunning())) return JobStatus.Running;
        return
    }

    public List<Run> getNextRuns(List<Step> nextSteps) {
        return runs.stream().filter(r -> nextSteps.stream().anyMatch(s -> s.getId().equals(r.getStepId()))).collect(Collectors.toList());
    }
}
