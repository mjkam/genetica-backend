package com.example.demo.domain.mysql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@Data
public class Task {
    @Id
    @GeneratedValue
    @Column(name = "task_id")
    private Long id;

    private String pipelineId;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;
}
