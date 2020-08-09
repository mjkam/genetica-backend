package com.example.demo;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJob;
import com.example.demo.helper.PipelineBuilder;
import com.example.demo.repository.mysql.JobFileRepository;
import com.example.demo.repository.mysql.RunRepository;
import com.example.demo.service.NextRunFinder;
import com.example.demo.util.KubeUtil;
import io.kubernetes.client.openapi.models.V1EnvVar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
public class NextRunFinderTests {
    @Mock
    private RunRepository runRepository;
    @Mock
    private JobFileRepository jobFileRepository;
    @InjectMocks
    private NextRunFinder nextRunFinder;

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

    @BeforeEach
    void setUp() {
        job = mock(Job.class);
        given(job.getId()).willReturn(1L);
        pipeline = PipelineBuilder.createSmallPipeline();
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
    public void 첫번째잡_끝난후_다음잡찾기_테스트() {
        Run firstRun = mock(Run.class);
        given(firstRun.getJob()).willReturn(job);
        Run secondRun = mock(Run.class);
        given(secondRun.getStepId()).willReturn("bwa_mem_bundle_0_7_17");
        given(secondRun.getId()).willReturn(3L);
        given(runRepository.findNotStarted(1L)).willReturn(Arrays.asList(secondRun));
        given(jobFileRepository.findByJobId(1L)).willReturn(Arrays.asList(jobFile1, jobFile2, jobFile3, jobFile4));

        List<KubeJob> kubeJobs = nextRunFinder.find(firstRun, "L001", pipeline);

        assertThat(kubeJobs.size()).isEqualTo(1);

        KubeJob actual = kubeJobs.get(0);
        KubeJob expect = createKubeJob2(secondRun);
        assertThat(actual.getCommand()).containsExactlyInAnyOrderElementsOf(expect.getCommand());
        assertThat(actual.getKubeEnvs()).containsExactlyInAnyOrderElementsOf(expect.getKubeEnvs());
        assertThat(actual.getImageName()).isEqualTo(expect.getImageName());
    }

    private KubeJob createKubeJob2(Run run) {
        List<V1EnvVar> envs = new ArrayList<>();
        V1EnvVar env1 = KubeUtil.createKubeEnv("input_read_1", "L001_R1.fastq.gz");
        V1EnvVar env2 = KubeUtil.createKubeEnv("input_read_2", "L001_R2.fastq.gz");
        V1EnvVar env3 = KubeUtil.createKubeEnv("reference_fasta", "human_g1k_v37_decoy.fasta");
        V1EnvVar env4 = KubeUtil.createKubeEnv("sample", "L001");
        envs.add(env1);
        envs.add(env2);
        envs.add(env3);
        envs.add(env4);

        List<String> commands = new ArrayList<>();
        commands.add("aws s3 cp s3://genetica/L001_R1.fastq.gz .");
        commands.add("aws s3 cp s3://genetica/L001_R2.fastq.gz .");
        commands.add("aws s3 cp s3://genetica/human_g1k_v37_decoy.fasta .");
        commands.add("bwa mem -R '@RG\\tID:1\\tPL:Illumina\\tSM:dnk_sample' -t 8 human_g1k_v37_decoy.fasta L001_R1.fastq.gz L001_R2.fastq.gz | samtools view -bS - > L001.bam && samtools sort L001.bam > L001_sorted.bam && samtools index L001_sorted.bam");
        commands.add("aws s3 cp L001_sorted.bam s3://genetica/");

        String imageName = "338282184009.dkr.ecr.ap-northeast-2.amazonaws.com/myrepo:bwa_0.7.17samtools_1.10";

        return KubeJob.createJob(job.getId(), run.getId(), envs, imageName, commands );
    }

    @Test
    public void Initializer끝났을때_다음잡찾기_테스트() {
        Run initializerRun = mock(Run.class);
        given(initializerRun.getJob()).willReturn(job);
        Run firstRun = mock(Run.class);
        given(firstRun.getStepId()).willReturn("untar_fasta");
        given(firstRun.getId()).willReturn(2L);
        Run secondRun = mock(Run.class);
        given(secondRun.getStepId()).willReturn("bwa_mem_bundle_0_7_17");
        given(secondRun.getId()).willReturn(3L);
        given(runRepository.findNotStarted(1L)).willReturn(Arrays.asList(firstRun, secondRun));
        given(jobFileRepository.findByJobId(1L)).willReturn(Arrays.asList(jobFile1, jobFile2, jobFile3));

        List<KubeJob> kubeJobs = nextRunFinder.find(initializerRun, "L001", pipeline);

        assertThat(kubeJobs.size()).isEqualTo(1);

        KubeJob actual = kubeJobs.get(0);
        KubeJob expect = createKubeJob(firstRun);
        assertThat(actual.getCommand()).containsExactlyInAnyOrderElementsOf(expect.getCommand());
        assertThat(actual.getKubeEnvs()).containsExactlyInAnyOrderElementsOf(expect.getKubeEnvs());
        assertThat(actual.getImageName()).isEqualTo(expect.getImageName());
    }

    private KubeJob createKubeJob(Run run) {
        List<V1EnvVar> envs = new ArrayList<>();
        V1EnvVar env1 = KubeUtil.createKubeEnv("input_tar_with_reference", "human_g1k_v37_decoy.fasta.tar");
        V1EnvVar env2 = KubeUtil.createKubeEnv("sample", "L001");
        envs.add(env1);
        envs.add(env2);

        List<String> commands = new ArrayList<>();
        commands.add("aws s3 cp s3://genetica/human_g1k_v37_decoy.fasta.tar .");
        commands.add("tar -xf human_g1k_v37_decoy.fasta.tar");

        String imageName = "338282184009.dkr.ecr.ap-northeast-2.amazonaws.com/myrepo:genetica_base";

        return KubeJob.createJob(job.getId(), run.getId(), envs, imageName, commands );
    }
}
