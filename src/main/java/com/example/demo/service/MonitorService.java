package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mongo.ToolIO;
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
    private final KubeClient kubeClientService;
    private final PipelineRepository pipelineRepository;
    private final RunRepository runRepository;
    private final JobFileRepository jobFileRepository;
    private final CommandLineService commandLineService;
    private final FileRepository fileRepository;

    public void handleJobEvent(Long taskId, Long jobId, Long runId, List<V1EnvVar> kubeEnvs, JobStatus resultStatus, String nodeName, KubeJobType kubeJobType) {
        //해당 잡 찾아서 상태 업데이트
        //끝난 잡의 아웃풋 파일이 최종 아웃풋 파일이면 파일 생성 및 잡파일 추가
        //
        //다음 런 찾아서 실행
        //환경변수 input, output 용 등록해줘야함



        Run run = runRepository.findRunWithJoinById(runId);
        Job job = run.getJob();
        Task task = job.getTask();
        Pipeline pipeline = pipelineRepository.findById(task.getPipelineId()).orElseThrow(() -> new RuntimeException());

        job.changeStatus(resultStatus);

        if(resultStatus.equals(JobStatus.Succeeded)) {
            if(kubeJobType.equals(KubeJobType.INITIALIZER)) kubeClientService.addLabelToNode(nodeName, jobId);
            else {
                OutputData outputData = createOutputData(pipeline, run.getStepId(), kubeEnvs, getSampleId(kubeEnvs), job);
                fileRepository.saveAll(outputData.getFiles());
                jobFileRepository.saveAll(outputData.getJobFiles());
            }

            List<KubeJob> nextKubeJobs = createNextKubeJobs(pipeline, kubeEnvs, taskId, job);
            runNextKubeJobs(nextKubeJobs);
        }
    }

    public void runNextKubeJobs(List<KubeJob> kubeJobs) {
        for(KubeJob kubeJob: kubeJobs) {
            kubeClientService.runJob(kubeJob);
        }
    }

    public OutputData createOutputData(Pipeline pipeline, String stepId, List<V1EnvVar> kubeEnvs, String sampleId, Job job) {
        Step step = pipeline.getStep(stepId);
        Map<String, String> envs = step.createEnvMap(kubeEnvs);
        List<V1EnvVar> outputKubeEnvs = getStepOutputFilesKubeEnvs(envs, step);
        OutputData outputData = new OutputData();
        for(V1EnvVar env: outputKubeEnvs) {
            File file = new File(env.getValue(), 1000L, sampleId);
            outputData.addFile(file);
            outputData.addJobFile(createJobFiles(pipeline, env, job, file));
        }
        return outputData;
    }

    public List<JobFile> createJobFiles(Pipeline pipeline, V1EnvVar env, Job job, File file) {
        List<JobFile> list = new ArrayList<>();
        for(ToolIO io: pipeline.getOutputs()) {
            if(io.getSource().equals(env.getName())) {
                JobFile jobFile = new JobFile(job, file, env.getName());
                list.add(jobFile);
            }
        }
        return list;
    }

    public String getSampleId(List<V1EnvVar> kubeEnvs) {
        return kubeEnvs.stream().filter(e -> e.getName().equals("sample")).findFirst().orElseThrow(() -> new RuntimeException()).getValue();
    }

    public List<V1EnvVar> getStepOutputFilesKubeEnvs(Map<String, String> envs, Step step) {
        List<V1EnvVar> kubeEnvs = new ArrayList<>();
        for(StepIO out: step.getOut()) {
            kubeEnvs.add(KubeUtil.createKubeEnv(step.getId() + "." + out.getId(), commandLineService.getEchoString(envs, out.getScript())));
        }
        return kubeEnvs;
    }

    public List<KubeJob> createNextKubeJobs(Pipeline pipeline, List<V1EnvVar> kubeEnvs, Long taskId, Job job) {
        List<Step> nextRunnableSteps = pipeline.getNextSteps(job.getFinishedRuns());
        List<KubeJob> kubeJobs = new ArrayList<>();
        for(Step step: nextRunnableSteps) {
            Run nextRun = runRepository.findRun(job.getId(), step.getId());

            Map<String, String> envs = step.createEnvMap(kubeEnvs);
            List<V1EnvVar> outputKubeEnvs = getStepOutputFilesKubeEnvs(envs, step);
            kubeEnvs.addAll(outputKubeEnvs);

            List<String> commands = new ArrayList<>();
            commands.addAll(createInputCommands(envs));
            commands.add(createMainCommand(envs, step));
            commands.addAll(createOutputCommands(outputKubeEnvs));

            kubeJobs.add(new KubeJob(taskId, job.getId(), nextRun.getId(), KubeJobType.ANALYSIS, kubeEnvs, step.getTool().getImage(), commands));
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
