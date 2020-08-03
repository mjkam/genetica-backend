package com.example.demo.service.helper;

import com.example.demo.domain.mysql.File;
import com.example.demo.domain.mysql.JobFile;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OutputData {
    private List<File> files = new ArrayList<>();
    private List<JobFile> jobFiles = new ArrayList<>();

    public void addFile(File f) {
        this.files.add(f);
    }

    public void addJobFile(JobFile jf) {
        this.jobFiles.add(jf);
    }
}
