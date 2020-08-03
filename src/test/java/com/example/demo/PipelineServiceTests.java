package com.example.demo;

import com.example.demo.domain.mongo.*;
import com.example.demo.domain.mysql.File;
import com.example.demo.domain.mysql.Job;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.KubeJobType;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.helper.FileManager;
import com.example.demo.helper.PipelineManager;
import com.example.demo.helper.RequestManager;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.FileRepository;
import com.example.demo.service.PipelineService;
import com.example.demo.service.helper.TaskData;
import io.kubernetes.client.openapi.models.V1EnvVar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

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
    void createTaskDataTest() {
        RunPipelineRequest request = RequestManager.createRunPipelineRequest(pipeline);
        Pipeline pipeline = pipelineRepository.findAll().get(0);
        Map<String, List<File>> inputsMap = pipelineService.createInputFileMap(request.getData());

        TaskData taskData = pipelineService.createTaskData(inputsMap, pipeline);

        assertThat(taskData.getTask().getPipelineId()).isEqualTo(pipeline.getId());
        assertThat(taskData.getJobs().size()).isEqualTo(2);
        assertThat(taskData.getRuns().size()).isEqualTo(4);
        assertThat(taskData.getRuns()).extracting("stepId").contains("untar_fasta", "bwa_mem_bundle_0_7_17");
        assertThat(taskData.getJobFiles().size()).isEqualTo(6);
        assertThat(taskData.getJobFiles()).extracting("file").extracting("name").contains("TESTX_H7YRLADXX_S1_L001_R1_001.fastq.gz", "TESTX_H7YRLADXX_S1_L001_R2_001.fastq.gz", "TESTX_H7YRLADXX_S1_L001", "human_g1k_v37_decoy.fasta.tar");

        Map<Job, List<V1EnvVar>> envs = taskData.getEnvs();
        List<Job> jobs = taskData.getJobs();
        for(Map.Entry<Job, List<V1EnvVar>> elem: envs.entrySet()) {
            assertThat(jobs).contains(elem.getKey());
            assertThat(elem.getValue()).extracting("name").contains("input_read_1", "input_read_2", "sample", "input_tar_with_reference");
            assertThat(elem.getValue()).extracting("value").contains("TESTX_H7YRLADXX_S1_L001_R1_001.fastq.gz", "TESTX_H7YRLADXX_S1_L001_R2_001.fastq.gz", "TESTX_H7YRLADXX_S1_L002_R1_001.fastq.gz", "TESTX_H7YRLADXX_S1_L002_R2_001.fastq.gz", "TESTX_H7YRLADXX_S1_L001", "TESTX_H7YRLADXX_S1_L002", "human_g1k_v37_decoy.fasta.tar");
        }
        //System.out.println(kubeJobs.get(0).getJobEnvs());
    }
}