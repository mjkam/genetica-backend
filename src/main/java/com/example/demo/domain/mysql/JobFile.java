package com.example.demo.domain.mysql;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Map;

@Entity
@Data
@NoArgsConstructor
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

    public JobFile(Job job, File file, String ioId) {
        this.job = job;
        this.file = file;
        this.ioType = "input"; // Todo: Enum 으로 변경 //필요 없을수도?
        this.targetId = ioId;
    }
}
