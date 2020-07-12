package com.example.demo.domain.mysql;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
public class JobFile {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    private String ioType;

    private String targetId;
}
