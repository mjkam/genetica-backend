package com.example.demo.helper;

import com.example.demo.domain.mysql.File;

import java.util.ArrayList;
import java.util.List;

public class FileBuilder {
    public static List<File> createFiles() {
        List<File> files = new ArrayList<>();
        files.add(new File("human_g1k_v37_decoy.fasta.tar", 1000L));
        files.add(new File("L001_R1.fastq.gz", 1000L, "L001"));
        files.add(new File("L001_R2.fastq.gz", 1000L, "L001"));
        files.add(new File("L002_R1.fastq.gz", 1000L, "L002"));
        files.add(new File("L002_R2.fastq.gz", 1000L, "L002"));
        return files;
    }
}
