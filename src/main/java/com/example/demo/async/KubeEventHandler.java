package com.example.demo.async;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mysql.Job;
import com.example.demo.domain.mysql.JobEnv;
import com.example.demo.domain.mysql.JobFile;
import com.example.demo.domain.mysql.Run;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.JobEnvRepository;
import com.example.demo.repository.mysql.JobFileRepository;
import com.example.demo.repository.mysql.JobRepository;
import com.example.demo.repository.mysql.RunRepository;
import com.example.demo.service.CommandLineService;
import com.example.demo.service.KubeClientService;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;
import org.apache.commons.collections4.ListUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KubeEventHandler implements Runnable {
    private Map<String, String> labels;
    private String resultStatus;
    private String nodeName;
    private KubeClientService kubeClientService;
    private JobRepository jobRepository;
    private PipelineRepository pipelineRepository;
    private JobEnvRepository jobEnvRepository;
    private RunRepository runRepository;
    private JobFileRepository jobFileRepository;
    private CommandLineService commandLineService;

    public KubeEventHandler(Map<String, String> labels,
                            String resultStatus,
                            String nodeName,
                            KubeClientService kubeClientService,
                            PipelineRepository pipelineRepository,
                            JobEnvRepository jobEnvRepository,
                            RunRepository runRepository,
                            CommandLineService commandLineService,
                            JobFileRepository jobFileRepository,
                            JobRepository jobRepository) {
        this.labels = labels;
        this.resultStatus = resultStatus;
        this.nodeName = nodeName;
        this.kubeClientService = kubeClientService;
        this.pipelineRepository = pipelineRepository;
        this.jobEnvRepository = jobEnvRepository;
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.jobFileRepository = jobFileRepository;
        this.commandLineService = commandLineService;
    }
    @Override
    public void run() {
        String type = labels.get("type");
        Long jobId = Long.valueOf(labels.get("jobId"));
        Long runId = Long.valueOf(labels.get("runId"));

        if(type.equals("initializer")) {
            handleInitializer(jobId, this.resultStatus, nodeName);
        }

        if(type.equals("job")) {
            handleJobResult(jobId, runId, this.resultStatus);
        }

        //System.out.println(job.object.getMetadata().getLabels().get("job-name") + job.object.getStatus());*/
    }

    public void handleInitializer(Long jobId, String resultState, String nodeName) {
        //Todo: job state 변경
        if(resultState.equals("Succeeded")) {
            kubeClientService.addLabelToNode(nodeName, jobId);
            runNextRun(jobId);
        }
    }

    public void handleJobResult(Long jobId, Long runId, String resultState) {
        Run run = runRepository.findById(runId).get();
        run.setStatus(resultState);
        jobEnvRepository.updateJobEnvRelatedtoRun(jobId, run.getId());

        if(resultState.equals("Succeeded")) {
            runNextRun(jobId);
        }
    }

    private Boolean containsAllEnv(List<JobEnv> envs, List<StepIO> stepInputs) {
        for(StepIO stepInput: stepInputs) {
            boolean check = false;
            for(JobEnv env: envs) {
                if(stepInput.getSource().equals(env.getEnvKey())) {
                    check = true;
                }
            }
            if(!check) return false;
        }
        return true;
    }

    private List<JobEnv> getInputEnvs(List<JobEnv> envs, List<StepIO> stepInputs, Job job) {
        List<JobEnv> newJobEnvs = new ArrayList<>();
        for(StepIO stepInput: stepInputs) {
            for(JobEnv env: envs) {
                if(stepInput.getSource().equals(env.getEnvKey())) {
                    JobEnv jobEnv = new JobEnv();
                    jobEnv.setJob(job);
                    jobEnv.setEnvKey(stepInput.getId());
                    jobEnv.setEnvVal(env.getEnvVal());
                    newJobEnvs.add(jobEnv);
                }
            }
        }
        return newJobEnvs;
    }

    private Step findNextStep(Pipeline pipeline, List<JobEnv> envs) {
        for(Step step : pipeline.getSteps()) {
            if(containsAllEnv(envs, step.getIn())) {
                return step;
            }
        }
        return null;
    }

    private List<String> getS3CopyInCommand(List<JobFile> jobFiles, Step step) {
        List<JobFile> inputJobFiles = jobFiles.stream().filter(jf -> jf.getIoType().equals("input")).collect(Collectors.toList());
        List<String> commands = new ArrayList<>();
        for(StepIO stepIO: step.getIn()) {
            for(JobFile jobFile: inputJobFiles) {
                if(stepIO.getSource().equals(jobFile.getTargetId())) {
                    commands.add(String.format("aws s3 cp s3://genetica/%s .", jobFile.getFile().getName()));
                }
            }
        }
        return commands;
    }

    private List<String> getS3CopyOutCommand(List<JobFile> jobFiles, Step step) {
        List<JobFile> outputJobFiles = jobFiles.stream().filter(jf -> jf.getIoType().equals("output")).collect(Collectors.toList());
        List<String> commands = new ArrayList<>();
        for(StepIO stepIO: step.getOut()) {
            for(JobFile jobFile: outputJobFiles) {
                if(stepIO.getSource().equals(jobFile.getTargetId())) {
                    commands.add(String.format("aws s3 cp %s s3://genetica/", jobFile.getFile().getName()));
                }
            }
        }
        return commands;
    }

    private List<JobEnv> createOutputEnvs(List<JobEnv> inputEnvs, Step step) {
        List<JobEnv> outputEnvs = new ArrayList<>();
        for(StepIO stepIO: step.getOut()) {
            JobEnv jobEnv = new JobEnv();
            jobEnv.setEnvVal(stepIO.getId());
            jobEnv.setEnvKey(commandLineService.getEchoString(inputEnvs, stepIO.getScript()));
        }
        return outputEnvs;
    }

    @Transactional
    public void runNextRun(Long jobId) {
        //Job, Pipeline, JobEnv  FETCH
        //Step 을 돌면서 다음 Step 을 찾아
        //Step 에 해당하는 Run 찾아서
        //다음 Step 에 들어갈 환경변수 Map 을 리포지토리에 저장
        Job job = jobRepository.findById(jobId).get();
        Pipeline pipeline = pipelineRepository.findById(job.getTask().getPipelineId()).get();
        List<JobEnv> validEnvList = jobEnvRepository.findAllValidEnvsInJob(jobId);

        Step nextStep = findNextStep(pipeline, validEnvList);
        if(nextStep == null) throw new RuntimeException();

        Run nextRun = runRepository.findRun(jobId, nextStep.getId());

        List<JobEnv> inputEnvs = getInputEnvs(validEnvList, nextStep.getIn(), job);
        List<JobEnv> outputEnvs = createOutputEnvs(inputEnvs, nextStep);

        jobEnvRepository.saveAll(outputEnvs);

        List<JobFile> jobFiles = jobFileRepository.findJobFilesInJob(jobId);

        List<String> command = new ArrayList<>();
        command = ListUtils.union(command, getS3CopyInCommand(jobFiles, nextStep));
        command.add(commandLineService.getEchoString(inputEnvs, nextStep.getRun().getCommand()));
        command = ListUtils.union(command, getS3CopyOutCommand(jobFiles, nextStep));

        System.out.println("command: " + command);

        kubeClientService.runJob(jobId, nextRun.getId(), "job", inputEnvs, nextStep.getRun().getImage(), command);
    }
}