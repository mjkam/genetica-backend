package com.example.demo;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJobType;
import com.example.demo.helper.PipelineBuilder;
import com.example.demo.repository.mysql.FileRepository;
import com.example.demo.repository.mysql.JobFileRepository;
import com.example.demo.service.JobEventHandler;
import com.example.demo.service.KubeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

@ActiveProfiles("test")
@SpringBootTest
public class JobEventHandlerTests {
    @Mock
    private KubeClient kubeClient;
    @Mock
    private JobFileRepository jobFileRepository;
    @Mock
    private FileRepository fileRepository;
    @InjectMocks
    private JobEventHandler jobEventHandler;

    private Pipeline pipeline;
    private Job job;
    private File file1;
    private File file2;
    private File file3;
    private File file4;
    private File file5;
    private File file6;
    private JobFile jobFile1;
    private JobFile jobFile2;
    private JobFile jobFile3;
    private JobFile jobFile4;
    private JobFile jobFile5;
    private JobFile jobFile6;

    @Captor
    ArgumentCaptor<File> fileCaptor;
    @Captor
    ArgumentCaptor<JobFile> jobFileCaptor;

    @BeforeEach
    void setUp() {
        pipeline = PipelineBuilder.createSmallPipeline();
        job = mock(Job.class);
        given(job.getId()).willReturn(1L);
        file1 = new File("L001_R1.fastq.gz", 1000L, "L001");
        file2 = new File("L001_R2.fastq.gz", 1000L, "L001");
        file3 = new File("human_g1k_v37_decoy.fasta.tar", 1000L, "L001");
        file4 = new File("human_g1k_v37_decoy.fasta", 1000L);
        file5 = new File("L001_sorted.bam", 1000L, "L001");
        file6 = new File("L001_sorted.bam.bai", 1000L, "L001");
        jobFile1 = new JobFile(job, file1, "input_read_1");
        jobFile2 = new JobFile(job, file2, "input_read_2");
        jobFile3 = new JobFile(job, file3, "input_tar_with_reference");
        jobFile4 = new JobFile(job, file4, "untar_fasta.output_fasta");
        jobFile5 = new JobFile(job, file5, "bwa_mem_bundle_0_7_17.aligned_bam");
        jobFile6 = new JobFile(job, file6, "bwa_mem_bundle_0_7_17.aligned_bam_bai");
    }

    @Test
    public void Analysis_Succeeded_Test() {
        Run run = new Run(job, "bwa_mem_bundle_0_7_17");
        run.changeStatus(JobStatus.Pending);
        List<JobFile> jobFiles = Arrays.asList(jobFile1, jobFile2, jobFile3, jobFile4);
        given(jobFileRepository.findByJobId(1L)).willReturn(jobFiles);

        jobEventHandler.handle(run, pipeline, "L001", KubeJobType.ANALYSIS, JobStatus.Succeeded, "nodeName");

        assertThat(run.isSucceeded()).isEqualTo(true);

        verify(fileRepository, times(2)).save(fileCaptor.capture());
        List<File> capturedFiles = fileCaptor.getAllValues();
        assertThat(capturedFiles.get(0)).isEqualTo(file5);
        assertThat(capturedFiles.get(1)).isEqualTo(file6);

        verify(jobFileRepository, times(2)).save(jobFileCaptor.capture());
        List<JobFile> capturedJobFiles = jobFileCaptor.getAllValues();
        assertThat(capturedJobFiles.get(0)).isEqualTo(jobFile5);
        assertThat(capturedJobFiles.get(1)).isEqualTo(jobFile6);
    }

    @Test
    public void Initializer_Succeeded_Test() {
        Run run = new Run(job, "");

        jobEventHandler.handle(run, pipeline, "L001", KubeJobType.INITIALIZER, JobStatus.Succeeded, "nodeName");

        assertThat(run.isSucceeded()).isEqualTo(true);
        verify(kubeClient).addLabelToNode(anyString(), anyLong());
    }

    @Test
    public void Initializer_Pending_Test() {
        Run run = new Run(job, "");

        jobEventHandler.handle(run, pipeline, "L001", KubeJobType.INITIALIZER, JobStatus.Pending, "nodeName");

        assertThat(run.getStatus()).isEqualTo(JobStatus.Pending);
        assertThat(run.getStartTime()).isNull();
        assertThat(run.isSucceeded()).isEqualTo(false);
    }
}
