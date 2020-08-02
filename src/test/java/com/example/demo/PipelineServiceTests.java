package com.example.demo;

import com.example.demo.domain.mongo.*;
import com.example.demo.domain.mysql.File;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.KubeJobType;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.helper.FileManager;
import com.example.demo.helper.PipelineManager;
import com.example.demo.helper.RequestManager;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.FileRepository;
import com.example.demo.service.PipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
public class PipelineServiceTests {
    @Autowired
    PipelineRepository pipelineRepository;

    @Autowired
    FileRepository fileRepository;

    @Autowired
    PipelineService pipelineService;

    private Pipeline pipeline;


    @BeforeEach
    void setup() {
        pipelineRepository.deleteAll();
        List<File> files = FileManager.createWESInputFiles();
        files.stream().forEach(f -> fileRepository.save(f));

        pipeline = PipelineManager.createSmallPipeline();
        pipelineRepository.save(pipeline);
    }

    @Test
    void parseRequestTest() {
        RunPipelineRequest request = RequestManager.createRunPipelineRequest(pipeline);

        List<KubeJob> kubeJobs = pipelineService.parseRequest(request);

        assertThat(kubeJobs.size()).isEqualTo(1);
        assertThat(kubeJobs.get(0).getKubeJobType()).isEqualTo(KubeJobType.INITIALIZER);
//        assertThat(kubeJobs.get(0).getJobEnvs()).extracting("envKey").contains("input_read_1", "input_read_2", "sample", "input_tar_with_reference");
//        assertThat(kubeJobs.get(0).getJobEnvs()).extracting("envVal").contains("TESTX_H7YRLADXX_S1_L001_R1_001.fastq.gz", "TESTX_H7YRLADXX_S1_L001_R2_001.fastq.gz", "TESTX_H7YRLADXX_S1_L001", "human_g1k_v37_decoy.fasta.tar");
        //System.out.println(kubeJobs.get(0).getJobEnvs());
    }


}
