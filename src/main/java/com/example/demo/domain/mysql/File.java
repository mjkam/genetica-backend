package com.example.demo.domain.mysql;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public class File {
    @Id
    @GeneratedValue
    @Column(name = "file_id")
    private Long id;

    private String name;

    private Long size;

    private String sampleId;

    private boolean isUsable;

    public File(String name, Long size, String sampleId) {
        this.name = name;
        this.size = size;
        this.sampleId = sampleId;
        this.isUsable = true;
    }

    public File(String name, Long size) {
        this.name = name;
        this.size = size;
        this.isUsable = true;
    }

    public void setUnusable() {
        this.isUsable = false;
    }
}
