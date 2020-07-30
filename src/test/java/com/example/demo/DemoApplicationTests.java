package com.example.demo;

import com.example.demo.async.KubeEventHandler;
import com.example.demo.domain.mongo.*;
import com.example.demo.domain.mysql.File;
import com.example.demo.domain.mysql.JobEnv;
import com.example.demo.dto.request.InputFileInfo;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import com.example.demo.service.CommandLineService;
import com.example.demo.service.KubeClientService;
import com.example.demo.service.MonitorService;
import com.example.demo.service.PipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

	@Autowired
	private MonitorService monitorService;



	@BeforeEach
	void insertPipeline() {
		pipelineRepository.deleteAll();

		ToolIO tio6 = ToolIO.builder().id("input_tar_with_reference").label("Input archive file with fasta").type("file").build();
		ToolIO tio7 = ToolIO.builder().id("output_fasta").label("Unpacked fasta file").type("file").script("${input_tar_with_reference::${#input_tar_with_reference}-4}").build();
		Tool unTarTool = Tool.builder()
				.nameId("untar_fasta")
				.label("untar fasta")
				.image("338282184009.dkr.ecr.ap-northeast-2.amazonaws.com/myrepo:genetica_base")
				.command("tar -xf ${input_tar_with_reference}")
				.inputs(Arrays.asList(tio6))
				.outputs(Arrays.asList(tio7)).build();

		ToolIO tio1 = ToolIO.builder().id("reference_fasta").label("reference fasta file").type("file").build();
		ToolIO tio2 = ToolIO.builder().id("input_read_1").label("input read 1").type("fileList").build();
		ToolIO tio3 = ToolIO.builder().id("input_read_2").label("input read 2").type("fileList").build();

		ToolIO tio4 = ToolIO.builder().id("aligned_bam").label("aligned BAM").source("bwa_mem_bundle_0_7_17.aligned_bam").type("fileList").script("${sample}_sorted.bam").build();
		ToolIO tio5 = ToolIO.builder().id("aligned_bam_bai").label("aligned BAM BAI").source("bwa_mem_bundle_0_7_17.aligned_bam_bai").type("fileList").script("${sample}_sorted.bam.bai").build();

		Tool bwaTool = Tool.builder()
				.nameId("bwa_mem_bundle_0_7_17")
				.label("BWA MEM Bundle 0.7.17")
				.image("338282184009.dkr.ecr.ap-northeast-2.amazonaws.com/myrepo:bwa_0.7.17samtools_1.10")
				.command("bwa mem -R \\'@RG\\\\\\tID:1\\\\\\tPL:Illumina\\\\\\tSM:dnk_sample\\' -t 8 ${reference_fasta} ${input_read_1} ${input_read_2} \\| samtools view -bS - \\> ${sample}.bam \\&\\& samtools sort ${sample}.bam \\> ${sample}_sorted.bam \\&\\& samtools index ${sample}_sorted.bam")
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
				.out(Arrays.asList(new StepIO("aligned_bam", "", "${sample}_sorted.bam"), new StepIO("aligned_bam_bai", "", "${sample}_sorted.bam.bai")))
				.run(bwaTool).build();


		Pipeline pipe = Pipeline.builder()
				.nameId("GATK4 WES PIPELINE")
				.inputs(Arrays.asList(tio6, tio2, tio3))
				.outputs(Arrays.asList(tio4))
				.steps(Arrays.asList(step1, step2)).build();

		pipelineRepository.save(pipe);

	}

	@BeforeEach
	void insertFiles() {
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

		/*
		File file4 = new File();
		file4.setName("TESTX_H7YRLADXX_S1_L002_R1_001.fastq.gz");
		file4.setSize(1000L);
		file4.setSampleId("TESTX_H7YRLADXX_S1_L002");

		File file5 = new File();
		file5.setName("TESTX_H7YRLADXX_S1_L002_R2_001.fastq.gz");
		file5.setSize(1000L);
		file5.setSampleId("TESTX_H7YRLADXX_S1_L002");*/

		fileRepository.save(file1);
		fileRepository.save(file2);
		fileRepository.save(file3);
		//fileRepository.save(file4);
		//fileRepository.save(file5);
	}

	@Test
	void hello() {
		System.out.println("@@");
	}

	@Test
	void runPipelineTest() {
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

		/*
		File file4 = new File();
		file4.setName("TESTX_H7YRLADXX_S1_L002_R1_001.fastq.gz");
		file4.setSize(1000L);
		file4.setSampleId("TESTX_H7YRLADXX_S1_L002");

		File file5 = new File();
		file5.setName("TESTX_H7YRLADXX_S1_L002_R2_001.fastq.gz");
		file5.setSize(1000L);
		file5.setSampleId("TESTX_H7YRLADXX_S1_L002");*/

		fileRepository.save(file1);
		fileRepository.save(file2);
		fileRepository.save(file3);
		//fileRepository.save(file4);
		//fileRepository.save(file5);

		RunPipelineRequest request = new RunPipelineRequest();


		InputFileInfo insertFileInfo1 = new InputFileInfo();
		insertFileInfo1.setId("input_tar_with_reference");
		insertFileInfo1.setFileIds(Arrays.asList(1L));

		InputFileInfo insertFileInfo2 = new InputFileInfo();
		insertFileInfo2.setId("input_read_1");
		insertFileInfo2.setFileIds(Arrays.asList(2L));

		InputFileInfo insertFileInfo3 = new InputFileInfo();
		insertFileInfo3.setId("input_read_2");
		insertFileInfo3.setFileIds(Arrays.asList(3L));

		request.setPipelineId("5f0dd336a0a22c74ccbc6df0");
		request.setData(Arrays.asList(insertFileInfo1, insertFileInfo2, insertFileInfo3));

		pipelineService.runPipeline(request);
	}

	@Test
	void InitializerPending() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "initializer");
		labels.put("taskId", "4");
		labels.put("jobId", "5");
		labels.put("runId", "0");
		String resultStatus = "Pending";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
		eventHandler.run();
	}

	@Test
	void InitializeSucceeded() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "initializer");
		labels.put("taskId", "4");
		labels.put("jobId", "5");
		labels.put("runId", "6");
		String resultStatus = "Succeeded";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
		eventHandler.run();
	}

	@Test
	void Run1Running() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "job");
		labels.put("taskId", "4");
		labels.put("jobId", "5");
		labels.put("runId", "6");
		String resultStatus = "Running";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
		eventHandler.run();
	}

	@Test
	void Run1Success() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "job");
		labels.put("taskId", "4");
		labels.put("jobId", "5");
		labels.put("runId", "6");
		String resultStatus = "Succeeded";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
		eventHandler.run();
	}

	@Test
	void Run2Running() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "job");
		labels.put("taskId", "4");
		labels.put("jobId", "5");
		labels.put("runId", "6");
		String resultStatus = "Running";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
		eventHandler.run();
	}

	@Test
	void Run2Succeeded() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "job");
		labels.put("taskId", "6");
		labels.put("jobId", "7");
		labels.put("runId", "9");
		String resultStatus = "Succeeded";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
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
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
		eventHandler.run();
	}

	@Test
	void firstRunFinishTest() {
		Map<String, String> labels = new HashMap<>();
		labels.put("type", "job");
		labels.put("jobId", "7");
		labels.put("runId", "8");
		String resultStatus = "Succeeded";
		String nodeName = "minikube";
		KubeEventHandler eventHandler = new KubeEventHandler(labels, resultStatus, nodeName, monitorService);
		eventHandler.run();
	}

	@Test
	void test2() {
		Pipeline pipeline = pipelineRepository.findById("5f0b1e904ba103285d4ddbcf").get();
		List<JobEnv> validEnvList = jobEnvRepository.findAllValidEnvsInJob(7L);
//		System.out.println(monitorService.findNextStep(pipeline, validEnvList, 7L));
	}

	@Test
	void commandLineServiceEchoTest() {
		String script = "bwa mem -R '@RG\\tID:1\\tPL:Illumina\\tSM:dnk_sample' -t 8 ${reference_fasta} ${input_read_1} ${input_read_2} \\| samtools view -bS - \\> ${sample}.bam \\&\\& samtools sort ${sample}.bam \\> ${sample}_sorted.bam && samtools index ${sample}_sorted.bam";
		JobEnv jobEnv = new JobEnv();
		jobEnv.setEnvKey("reference_fasta");
		jobEnv.setEnvVal("hg38.fasta");
		List<JobEnv> list = new ArrayList<>();
		list.add(jobEnv);
		System.out.println(commandLineService.getEchoString(list, script));
	}

	@Test
	void kubeClientServiceAddLabelTest() {
		kubeClientService.addLabelToNode("minikube", 100L);
	}


}
