package com.example.demo;

import com.example.demo.async.KubeEventHandler;
import com.example.demo.domain.mongo.*;
import com.example.demo.domain.mysql.File;
import com.example.demo.domain.mysql.JobEnv;
import com.example.demo.dto.request.InsertFileInfo;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import com.example.demo.service.CommandLineService;
import com.example.demo.service.KubeClientService;
import com.example.demo.service.PipelineService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.*;

@SpringBootTest
@ContextConfiguration(classes = DemoApplication.class,
		initializers = ConfigFileApplicationContextInitializer.class)
class DemoApplicationTests {

	@Autowired
	PipelineRepository pipelineRepository;

	@Autowired
	FileRepository fileRepository;

	@Autowired
	PipelineService pipelineService;

	@Autowired
	private KubeClientService kubeClientService;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private JobEnvRepository jobEnvRepository;

	@Autowired
	private RunRepository runRepository;

	@Autowired
	private JobFileRepository jobFileRepository;

	@Autowired
	private CommandLineService commandLineService;



	@Test
	void insertPipeline() {
		pipelineRepository.deleteAll();

		ToolIO tio6 = ToolIO.builder().id("input_tar_with_reference").label("Input archive file with fasta").type("file").build();
		ToolIO tio7 = ToolIO.builder().id("output_fasta").label("Unpacked fasta file").type("file").script("${input_tar_with_reference::${#input_tar_with_reference}-4}").build();
		Tool unTarTool = Tool.builder()
				.nameId("untar_fasta")
				.label("untar fasta")
				.image("genetica_base")
				.command("tar -xf ${input_tar_with_reference}")
				.inputs(Arrays.asList(tio6))
				.outputs(Arrays.asList(tio7)).build();

		ToolIO tio1 = ToolIO.builder().id("reference_fasta").label("reference fasta file").type("file").build();
		ToolIO tio2 = ToolIO.builder().id("input_read_1").label("input read 1").type("fileList").build();
		ToolIO tio3 = ToolIO.builder().id("input_read_2").label("input read 2").type("fileList").build();

		ToolIO tio4 = ToolIO.builder().id("aligned_bam").label("aligned BAM").type("fileList").script("${sample}_sorted.bam").build();
		ToolIO tio5 = ToolIO.builder().id("aligned_bam_bai").label("aligned BAM BAI").type("fileList").script("${sample}_sorted.bam.bai").build();

		Tool bwaTool = Tool.builder()
				.nameId("bwa_mem_bundle_0_7_17")
				.label("BWA MEM Bundle 0.7.17")
				.image("bwa_0.7.17")
				.command("bwa mem -R '@RG\\\\tID:1\\\\tPL:Illumina\\\\tSM:dnk_sample' -t 8 ${reference_fasta} ${input_read_1} ${input_read_2} | samtools view -bS - > ${sample}.bam && samtools sort ${sample}.bam > ${sample}_sorted.bam && samtools index ${sample}_sorted.bam")
				.inputs(Arrays.asList(tio1, tio2, tio3))
				.outputs(Arrays.asList(tio4, tio5))
				.build();

		Step step1 = Step.builder()
				.id("untar_fasta")
				.label("Untar fasta")
				.in(Arrays.asList(StepIO.builder().id("input_tar_with_reference").source("input_tar_with_reference").build()))
				.out(Arrays.asList(StepIO.builder().id("output_fasta").script("${input_tar_with_reference::${#input_tar_with_reference}-4}").build()))
				.run(unTarTool).build();

		Step step2 = Step.builder()
				.id("bwa_mem_bundle_0_7_17")
				.label("BWA MEM Bundle 0.7.17")
				.in(Arrays.asList(new StepIO("reference_fasta", "untar_fasta.output_fasta", ""), new StepIO("input_read_1", "input_read_1", ""), new StepIO("input_read_2", "input_read_2", "")))
				.out(Arrays.asList(new StepIO("aligned_bam", "", "")))
				.run(bwaTool).build();


		Pipeline pipe = Pipeline.builder()
				.nameId("GATK4 WES PIPELINE")
				.inputs(Arrays.asList(tio6, tio2, tio3))
				.outputs(Arrays.asList(tio4))
				.steps(Arrays.asList(step1, step2)).build();

		pipelineRepository.save(pipe);


		/*
		File file1 = new File();
		file1.setName("TESTX_H7YRLADXX_S1_L001_R1_001.fastq.gz");
		file1.setSampleId("TESTX_H7YRLADXX_S1_L001");
		file1.setSize(10000L);

		File file2 = new File();
		file2.setName("TESTX_H7YRLADXX_S1_L001_R2_001.fastq.gz");
		file2.setSampleId("TESTX_H7YRLADXX_S1_L001");
		file2.setSize(10000L);

		File file3 = new File();
		file3.setName("human_g1k_v37_decoy.fasta.tar");
		file3.setSize(100000000L);

		fileRepository.save(file1);
		fileRepository.save(file2);
		fileRepository.save(file3);*/
	}

	@Test
	void findPipe() {
		//pipelineRepository.
	}

	@Test
	void runPipelineTest() {
		File file1 = new File();
		file1.setName("hg38.fasta.tar");
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

		fileRepository.save(file1);
		fileRepository.save(file2);
		fileRepository.save(file3);
		fileRepository.save(file4);
		fileRepository.save(file5);

		RunPipelineRequest request = new RunPipelineRequest();


		InsertFileInfo insertFileInfo1 = new InsertFileInfo();
		insertFileInfo1.setId("input_tar_with_reference");
		insertFileInfo1.setFileIds(Arrays.asList(1L));

		InsertFileInfo insertFileInfo2 = new InsertFileInfo();
		insertFileInfo2.setId("input_read_1");
		insertFileInfo2.setFileIds(Arrays.asList(2L, 4L));

		InsertFileInfo insertFileInfo3 = new InsertFileInfo();
		insertFileInfo3.setId("input_read_2");
		insertFileInfo3.setFileIds(Arrays.asList(3L, 5L));

		request.setPipelineId("5f0b1e904ba103285d4ddbcf");
		request.setData(Arrays.asList(insertFileInfo1, insertFileInfo2, insertFileInfo3));

		pipelineService.runPipeline(request);

		Map<String, String> labels = new HashMap<>();
		labels.put("type", "initializer");
		labels.put("jobId", "7");
		labels.put("runId", "0");
		String resultStatus = "Succeeded";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, kubeClientService, pipelineRepository, jobEnvRepository, runRepository, commandLineService, jobFileRepository, jobRepository);
		eventHandler.run();

	}

	@Test
	void firstInitializerFinishTest() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "initializer");
		labels.put("jobId", "7");
		labels.put("runId", "0");
		String resultStatus = "Succeeded";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, kubeClientService, pipelineRepository, jobEnvRepository, runRepository, commandLineService, jobFileRepository, jobRepository);
		eventHandler.run();
	}

	@Test
	void commandLineServiceEchoTest() {
		JobEnv jobEnv = new JobEnv();
		jobEnv.setEnvKey("input_tar_with_reference");
		jobEnv.setEnvVal("hg38.fasta.tar");
		List<JobEnv> list = new ArrayList<>();
		list.add(jobEnv);
		System.out.println(commandLineService.getEchoString(list, "tar -xf ${input_tar_with_reference}"));
	}

	@Test
	void kubeClientServiceAddLabelTest() {
		kubeClientService.addLabelToNode("minikube", 100L);
	}

}
