package com.example.demo;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJobType;
import com.example.demo.helper.PipelineBuilder;
import com.example.demo.repository.mysql.FileRepository;
import com.example.demo.repository.mysql.JobFileRepository;
import com.example.demo.service.JobEventHandler;
import com.example.demo.service.KubeClient;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
public class MonitorServiceTests {
    @Mock
    private FileRepository fileRepository;
    @Mock
    private JobFileRepository jobFileRepository;
    @Mock
    private KubeClient kubeClient;

    @InjectMocks
    private JobEventHandler jobEventHandler;

    @Test
    public void test1() {
        Pipeline pipeline = PipelineBuilder.createSmallPipeline();
        Job job = mock(Job.class);
        Run run = new Run(job, "stepId");
        given(job.getId()).willReturn(1L);

        jobEventHandler.handle(run, KubeJobType.ANALYSIS, JobStatus.Succeeded, "nodeName");

        File file1 = new File("L001_sorted.bam", 1000L, "L001");
        File file2 = new File("L002_sorted.bam", 1000L, "L002");
        JobFile jobFile1 = new JobFile(job, file1, "aligned_bam");
        JobFile jobFile2 = new JobFile(job, file2, "aligned_bam");
        verify(fileRepository).save(file1);
        verify(fileRepository).save(file2);
        verify(jobFileRepository).save(jobFile1);
        verify(jobFileRepository).save(jobFile2);

    }

    @Test
    public void initializerSucceedTest() {
        Job job = mock(Job.class);
        Run run = new Run(job, "stepId");
        given(job.getId()).willReturn(1L);

        jobEventHandler.handle(run, KubeJobType.INITIALIZER, JobStatus.Succeeded, "nodeName");

        verify(kubeClient).addLabelToNode(anyString(), anyLong());
    }
}
