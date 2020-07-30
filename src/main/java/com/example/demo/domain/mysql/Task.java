package com.example.demo.domain.mysql;

import com.example.demo.domain.mongo.Pipeline;
import lombok.Data;

import javax.persistence.*;
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

    public Task(Pipeline pipeline) {
        LocalDateTime nowTime = LocalDateTime.now();
        this.setName(pipeline.getNameId() + nowTime);
        this.setPipelineId(pipeline.getId());
        this.setStartTime(nowTime);
    }
}
