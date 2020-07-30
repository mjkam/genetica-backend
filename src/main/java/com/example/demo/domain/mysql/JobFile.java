package com.example.demo.domain.mysql;

import lombok.Data;

import javax.persistence.*;
import java.util.Map;

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

    public JobFile(Job job, Map<String, File> toolInputFileMap, String ioId) {
        this.job = job;
        this.file = toolInputFileMap.get(ioId);
        this.ioType = "input"; // Todo: Enum 으로 변경
        this.targetId = ioId;
    }
}
