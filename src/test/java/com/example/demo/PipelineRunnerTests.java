package com.example.demo;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.helper.FileBuilder;
import com.example.demo.helper.PipelineBuilder;
import com.example.demo.helper.PipelineRunRequestBuilder;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import com.example.demo.service.PipelineRunner;
import com.example.demo.util.KubeUtil;
import io.kubernetes.client.openapi.models.V1EnvVar;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest
public class PipelineRunnerTests {
    @Mock
    FileRepository fileRepository;
    @Mock
    PipelineRepository pipelineRepository;
    @Mock
    TaskRepository taskRepository;
    @Mock
    JobRepository jobRepository;
    @Mock
    RunRepository runRepository;
    @Mock
    JobFileRepository jobFileRepository;

    @InjectMocks
    PipelineRunner pipelineRunner;

    private Pipeline pipeline;
    private Job job;
    private File reference;
    private File fq11;
    private File fq12;
    private File fq21;
    private File fq22;

    @Captor
    ArgumentCaptor<Task> taskCaptor;
    @Captor
    ArgumentCaptor<Job> jobCaptor;
    @Captor
    ArgumentCaptor<Job> runCaptor;
    @Captor
    ArgumentCaptor<Job> jobFileCaptor;


    @BeforeEach
    public void setUp() {
        pipeline = PipelineBuilder.createSmallPipeline();
        job = mock(Job.class);
        given(job.getId()).willReturn(1L);

        reference = new File("human_g1k_v37_decoy.fasta.tar", 1000L);
        fq11 = new File("L001_R1.fastq.gz", 1000L, "L001");
        fq12 = new File("L001_R2.fastq.gz", 1000L, "L001");
        fq21 = new File("L002_R1.fastq.gz", 1000L, "L002");
        fq22 = new File("L002_R2.fastq.gz", 1000L, "L002");

        given(fileRepository.findByIdIn(Arrays.asList(1L))).willReturn(Arrays.asList(reference));
        given(fileRepository.findByIdIn(Arrays.asList(2L, 4L))).willReturn(Arrays.asList(fq11, fq21));
        given(fileRepository.findByIdIn(Arrays.asList(3L, 5L))).willReturn(Arrays.asList(fq12, fq22));
    }

    @Test
    void runPipelineTest() {
        //given
        Pipeline pipeline = PipelineBuilder.createSmallPipeline();
        RunPipelineRequest request = PipelineRunRequestBuilder.createRunPipelineRequest(pipeline);

        given(pipelineRepository.findById(anyString())).willReturn(Optional.of(pipeline));

        //when
        List<KubeJob> kubeJobs = pipelineRunner.parseRunRequest(request);

        //then
        //1. KubeJob 테스트
        assertThat(kubeJobs.size()).isEqualTo(2);
        assertThat(kubeJobs.get(0).getKubeEnvs()).contains(createV1EnvVars().get(0));
        assertThat(kubeJobs.get(1).getKubeEnvs()).contains(createV1EnvVars().get(1));
        //Todo: KubeJob을 비교하는 method를 만들어야함

        //2. Task 테스트
        //3. Job 테스트
        //4. Run 테스트
        //5. JobFile 테스트


//        List<String> names = Arrays.asList("input_read_1", "input_read_2", "input_tar_with_reference", "sample");
//        List<String> values = files.stream().map(f -> f.getName()).collect(Collectors.toList());
//
//        assertThat(kubeJobs.size()).isEqualTo(2);
//        assertThat(kubeJobs.get(0).getKubeEnvs()).extracting("name").containsAll(names);
//        assertThat(kubeJobs).extracting("kubeEnvs").flatExtracting(l -> (List<V1EnvVar>)l).extracting("value").containsAll(values);
//
//        verify(taskRepository, times(1)).save(any(Task.class));
//        verify(jobRepository, times(2)).save(any(Job.class));
//
//        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
//        verify(runRepository, times(2)).saveAll(captor.capture());
//        assertThat(captor.getAllValues().size()).isEqualTo(2);
//        assertThat(captor.getAllValues().get(0).size()).isEqualTo(3);
//        assertThat(captor.getAllValues().get(1).size()).isEqualTo(3);
//
//        captor = ArgumentCaptor.forClass(List.class);
//        verify(jobFileRepository, times(2)).saveAll(captor.capture());
//        assertThat(captor.getAllValues().size()).isEqualTo(2);
//        assertThat(captor.getAllValues().get(0).size()).isEqualTo(3);
//        assertThat(captor.getAllValues().get(1).size()).isEqualTo(3);
    }

    private List<V1EnvVar> createV1EnvVars() {
        List<V1EnvVar> envs = new ArrayList<>();
        V1EnvVar env1 = KubeUtil.createKubeEnv("sample", "L001");
        V1EnvVar env2 = KubeUtil.createKubeEnv("sample", "L002");
        envs.add(env1);
        envs.add(env2);

        return envs;
    }
}