package com.example.demo;

import com.example.demo.domain.mysql.File;

import java.util.ArrayList;
import java.util.List;

public class FileManager {
    public static List<File> createWESInputFiles() {
        List<File> files = new ArrayList<>();

        File file1 = new File();
        file1.setName("human_g1k_v37_decoy.fasta.tar");
        file1.setSize(100000L);

        File file2 = new File();
        file2.setName("TESTX_H7YRLADXX_S1_L001_R1_001.fastq.gz");
        file2.setSize(1000L);
        file2.setSampleId("TESTX_H7YRLADXX_S1_L001");

        File file3 = new File();
        file3.setName("TESTX_H7YRLADXX_S1_L001_R2_001.fastq.gz");
        file3.setSize(1000L);
        file3.setSampleId("TESTX_H7YRLADXX_S1_L001");

        File file4 = new File();
        file4.setName("TESTX_H7YRLADXX_S1_L002_R1_001.fastq.gz");
        file4.setSize(1000L);
        file4.setSampleId("TESTX_H7YRLADXX_S1_L002");

        File file5 = new File();
        file5.setName("TESTX_H7YRLADXX_S1_L002_R2_001.fastq.gz");
        file5.setSize(1000L);
        file5.setSampleId("TESTX_H7YRLADXX_S1_L002");

        files.add(file1);
        files.add(file2);
        files.add(file3);
        files.add(file4);
        files.add(file5);
        return files;
    }
}
