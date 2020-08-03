package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.KubeJobType;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import com.example.demo.service.helper.OutputData;
import com.example.demo.util.KubeUtil;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MonitorService {
    private final KubeClientService kubeClientService;
    private final TaskRepository taskRepository;
    private final JobRepository jobRepository;
    private final PipelineRepository pipelineRepository;
    private final RunRepository runRepository;
    private final JobFileRepository jobFileRepository;
    private final CommandLineService commandLineService;
    private final FileRepository fileRepository;

    public void handleJobEvent(Long taskId, Long jobId, Long runId, List<V1EnvVar> kubeEnvs, JobStatus resultStatus, String nodeName, KubeJobType kubeJobType) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException());
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException());
        Run run = runRepository.findById(runId).orElseThrow(() -> new RuntimeException());
        Pipeline pipeline = pipelineRepository.findById(task.getPipelineId()).orElseThrow(() -> new RuntimeException());

        job.changeStatus(resultStatus);

        if(resultStatus.equals(JobStatus.Succeeded)) {
            if(kubeJobType.equals(KubeJobType.INITIALIZER)) kubeClientService.addLabelToNode(nodeName, jobId);
            else {
                Step step = pipeline.getStep(run.getStepId());
                Map<String, String> envs = step.createEnvMap(kubeEnvs);
                List<V1EnvVar> outputKubeEnvs = getStepOutputFilesKubeEnvs(envs, step);
                OutputData outputData = createOutputData(outputKubeEnvs, getSampleId(kubeEnvs), job);
                fileRepository.saveAll(outputData.getFiles());
                jobFileRepository.saveAll(outputData.getJobFiles());
            }

            List<Run> finishedRuns = runRepository.findRunsByStatus(jobId, JobStatus.Succeeded);
            List<Step> nextSteps = pipeline.getNextSteps(finishedRuns);
            List<KubeJob> kubeJobs = createNextKubeJobs(nextSteps, kubeEnvs, taskId, jobId);

            for(KubeJob kubeJob: kubeJobs) {
                kubeClientService.runJob(kubeJob);
            }
        }
    }

    public OutputData createOutputData(List<V1EnvVar> outputKubeEnvs, String sampleId, Job job) {
        OutputData outputData = new OutputData();
        for(V1EnvVar env: outputKubeEnvs) {
            File file = new File(env.getName(), 1000L, sampleId);
            JobFile jobFile = new JobFile(job, file, env.getName());
            outputData.addFile(file);
            outputData.addJobFile(jobFile);
        }
        return outputData;
    }

    public String getSampleId(List<V1EnvVar> kubeEnvs) {
        return kubeEnvs.stream().filter(e -> e.getName().equals("sample")).findFirst().orElseThrow(() -> new RuntimeException()).getValue();
    }

    public List<V1EnvVar> getStepOutputFilesKubeEnvs(Map<String, String> envs, Step step) {
        List<V1EnvVar> kubeEnvs = new ArrayList<>();
        for(StepIO out: step.getOut()) {
            kubeEnvs.add(KubeUtil.createKubeEnv(step.getId() + "." + out.getId(),commandLineService.getEchoString(envs, out.getScript())));
        }
        return kubeEnvs;
    }

    public List<KubeJob> createNextKubeJobs(List<Step> nextSteps, List<V1EnvVar> kubeEnvs, Long taskId, Long jobId) {
        List<KubeJob> kubeJobs = new ArrayList<>();
        for(Step step: nextSteps) {
            Run nextRun = runRepository.findRun(jobId, step.getId());
            nextRun.changeStatus(JobStatus.Pending);//Todo: 밖으로 빼야함..

            Map<String, String> envs = step.createEnvMap(kubeEnvs);
            List<V1EnvVar> outputKubeEnvs = getStepOutputFilesKubeEnvs(envs, step);
            kubeEnvs.addAll(outputKubeEnvs);

            List<String> commands = new ArrayList<>();
            commands.addAll(createInputCommands(envs));
            commands.add(createMainCommand(envs, step));
            commands.addAll(createOutputCommands(outputKubeEnvs));

            kubeJobs.add(new KubeJob(taskId, jobId, nextRun.getId(), KubeJobType.ANALYSIS, kubeEnvs, step.getTool().getImage(), commands));
        }
        return kubeJobs;
    }

    public String createMainCommand(Map<String, String> envs, Step step) {
        return commandLineService.getEchoString(envs, step.getTool().getCommand());
    }

    public List<String> createInputCommands(Map<String, String> envs) {
        List<String> commands = new ArrayList<>();
        for(Map.Entry<String, String> e: envs.entrySet()) {
            commands.add(String.format("aws s3 cp s3://genetica/%s .", e.getValue()));
        }
        return commands;
    }

    public List<String> createOutputCommands(List<V1EnvVar> outputKubeEnvs) {
        return outputKubeEnvs.stream().map(e -> String.format("aws s3 cp %s s3://genetica/", e.getValue())).collect(Collectors.toList());
    }
}
