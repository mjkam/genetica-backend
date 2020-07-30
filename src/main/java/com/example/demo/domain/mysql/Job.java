package com.example.demo.domain.mysql;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

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

    private String name;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    public Job(Task task, int idx) {
        this.status = JobStatus.Queued;
        this.task = task;
        this.name = task.getName() + "-" + (idx+1);
    }
}
