package com.example.demo.domain.mysql;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
public class File {
    @Id
    @GeneratedValue
    @Column(name = "file_id")
    private Long id;

    private String name;

    private Long size;

    private String sampleId;

    public File(String name, Long size, String sampleId) {
        this.name = name;
        this.size = size;
        this.sampleId = sampleId;
    }
}
