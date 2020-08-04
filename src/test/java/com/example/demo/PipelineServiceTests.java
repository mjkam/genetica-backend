package com.example.demo;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.File;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.helper.FileBuilder;
import com.example.demo.helper.PipelineBuilder;
import com.example.demo.helper.PipelineRunRequestBuilder;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import com.example.demo.service.PipelineRunner;
import io.kubernetes.client.openapi.models.V1EnvVar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.internal.bytebuddy.matcher.ElementMatchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@SpringBootTest
public class PipelineServiceTests {
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


    List<File> files;
    File reference;
    File fq11;
    File fq12;
    File fq21;
    File fq22;


    @BeforeEach
    public void setUp() {
        files = FileBuilder.createFiles();
        reference = files.get(0);
        fq11 = files.get(1);
        fq12 = files.get(2);
        fq21 = files.get(3);
        fq22 = files.get(4);

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
        List<String> names = Arrays.asList("input_read_1", "input_read_2", "input_tar_with_reference", "sample");
        List<String> values = files.stream().map(f -> f.getName()).collect(Collectors.toList());

        assertThat(kubeJobs.size()).isEqualTo(2);
        assertThat(kubeJobs.get(0).getKubeEnvs()).extracting("name").containsAll(names);
        assertThat(kubeJobs).extracting("kubeEnvs").flatExtracting(l -> (List<V1EnvVar>)l).extracting("value").containsAll(values);
    }
}