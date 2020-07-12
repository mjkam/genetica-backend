package com.example.demo.domain.mysql;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class JobEnv {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    private String envKey;

    private String envVal;
}
