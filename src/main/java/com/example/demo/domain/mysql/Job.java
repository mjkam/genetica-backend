package com.example.demo.domain.mysql;

import com.example.demo.domain.mongo.Step;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor
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

    public Job(Task task, int idx) {
        this.task = task;
        this.name = task.getName() + "-" + (idx + 1);
    }
}
