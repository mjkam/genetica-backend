package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mongo.ToolIO;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJob;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MonitorService {
    private final KubeClientService kubeClientService;
    private final TaskRepository taskRepository;
    private final JobRepository jobRepository;
    private final PipelineRepository pipelineRepository;
    private final JobEnvRepository jobEnvRepository;
    private final RunRepository runRepository;
    private final JobFileRepository jobFileRepository;
    private final CommandLineService commandLineService;
    private final FileRepository fileRepository;


    public void handleInitializer(Long taskId, Long jobId, JobStatus resultStatus, String nodeName) {
        Optional<Job> optionalJob = jobRepository.findById(jobId);
        if(!optionalJob.isPresent()) throw new RuntimeException();
        Job job = optionalJob.get();

        //job.changeStatus(resultStatus);
        if(resultStatus.equals(JobStatus.Succeeded)) {
            kubeClientService.addLabelToNode(nodeName, job.getId());
            runNextRun(taskId, jobId);
        }
    }

    public void handleAnalysis(Long taskId, Long jobId, Long runId, JobStatus resultStatus) {
        Job job = jobRepository.findById(jobId).get();
        Run run = runRepository.findById(runId).get();
        run.setStatus(resultStatus);

        if(resultStatus.equals(JobStatus.Running)) {
            run.setStartTime(LocalDateTime.now());
        }

        if(resultStatus.equals(JobStatus.Failed)) {
            run.setFinishTime(LocalDateTime.now());
        }

        if(resultStatus.equals(JobStatus.Succeeded)) {
            run.setFinishTime(LocalDateTime.now());
            jobEnvRepository.updateJobEnvRelatedtoRun(jobId, runId);

            List<JobEnv> sampleEnvs = jobEnvRepository.findSampleInRun(jobId, runId);
            List<JobEnv> outputEnvs  = jobEnvRepository.findNewOutput(jobId, runId);
            Pipeline pipeline = pipelineRepository.findById(taskRepository.findById(taskId).get().getPipelineId()).get();

            //List<File> newFiles = new ArrayList<>();
            //List<JobFile> newJobFiles = new ArrayList<>();
            for(JobEnv jobEnv: outputEnvs) {
                for(ToolIO toolIO : pipeline.getOutputs()) {
                    if(toolIO.getSource().equals(jobEnv.getEnvKey())) {
                        File file = new File();
                        file.setName(jobEnv.getEnvVal());
                        if(sampleEnvs.size() > 0) {
                            file.setSampleId(sampleEnvs.get(0).getEnvVal());
                        }
                        file.setSize(1000L);//Todo: S3 에서 가져와야함
                        fileRepository.save(file);

                        JobFile jobFile = new JobFile();
                        jobFile.setIoType("output");
                        jobFile.setJob(job);
                        jobFile.setFile(file);
                        jobFile.setTargetId(toolIO.getId());
                        jobFileRepository.save(jobFile);
                    }
                }
            }

            runNextRun(taskId, jobId);
        }
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

        for(JobEnv j: envs) {
            if(j.getEnvKey().equals("sample")) {
                JobEnv jobEnv = new JobEnv();
                jobEnv.setJob(job);
                jobEnv.setEnvKey(j.getEnvKey());
                jobEnv.setEnvVal(j.getEnvVal());
                newJobEnvs.add(jobEnv);
            }
        }

        return newJobEnvs;
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

    private List<String> getS3CopyOutCommand(List<JobEnv> outputEnvs, Pipeline pipeline) {
        List<String> commands = new ArrayList<>();
        for(JobEnv jobEnv: outputEnvs) {
            for(ToolIO toolIO : pipeline.getOutputs()) {
                if(toolIO.getSource().equals(jobEnv.getEnvKey())) {
                    commands.add(String.format("aws s3 cp %s s3://genetica/", jobEnv.getEnvVal()));
                }
            }
        }
        return commands;
    }

    private List<JobEnv> createOutputEnvs(List<JobEnv> inputEnvs, Step step, Job job, Run run) {
        List<JobEnv> outputEnvs = new ArrayList<>();
        for(StepIO stepIO: step.getOut()) {
            JobEnv jobEnv = new JobEnv();
            jobEnv.setJob(job);
            jobEnv.setIsValid(false);
            jobEnv.setRunId(run.getId());
            jobEnv.setEnvKey(step.getId() + "." + stepIO.getId());
            System.out.println("##########################");
            System.out.println(commandLineService.getEchoString(inputEnvs, stepIO.getScript()));
            System.out.println("##########################");
            jobEnv.setEnvVal(commandLineService.getEchoString(inputEnvs, stepIO.getScript()));
            outputEnvs.add(jobEnv);
        }
        return outputEnvs;
    }

    public JobStatus getStatusOfJob(Long jobId) {
        List<Run> runs = runRepository.findRunsInJob(jobId);
        if(runs.stream().anyMatch(r -> r.isFailed())) return

        for(Run run : runs) {
            if(run.getStatus().equals(JobStatus.Failed)) {
                return JobStatus.Failed;
            }
        }
        return JobStatus.Succeeded;
    }

    public Boolean checkIfTaskFinished(Long taskId) {
        List<Job> taskJobs = jobRepository.getJobsInTask(taskId);
        for(Job j : taskJobs) {
            if(!j.getStatus().equals(JobStatus.Succeeded) && !j.getStatus().equals(JobStatus.Failed)) return false;
        }
        return true;
    }

    public void runNextRun(Long taskId, Long jobId) {
        List<KubeJob> nextKubeJobs = findNextKubeJobs(taskId, jobId);
        for(KubeJob kubeJob: nextKubeJobs) {
            kubeClientService.runJob(kubeJob);
        }
    }

    public List<KubeJob> findNextKubeJobs(Long taskId, Long jobId) {
        List<KubeJob> kubeJobs = new ArrayList<>();

        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException());
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException());
        Pipeline pipeline = pipelineRepository.findById(task.getPipelineId()).orElseThrow(() -> new RuntimeException());
        List<JobEnv> validEnvList = jobEnvRepository.findAllValidEnvsInJob(jobId);

        List<Step> nextSteps = pipeline.getNextSteps(validEnvList);
//        if(nextSteps.size() == 0) {
//            job.changeStatus();
//            job.setStatus(getResultStatusOfJob(jobId));
//            job.setFinishTime(LocalDateTime.now());
//
//            if(checkIfTaskFinished(taskId)) {
//                task.setFinishTime(LocalDateTime.now());
//            }
//            return;
//        }

        List<Run> nextRuns = job.getNextRuns(nextSteps);
        for(Run nextRun: nextRuns) {
            List<JobEnv> inputEnvs = nextRun.getInputEnvs(validEnvList);
            List<JobEnv> outputEnvs = nextRun.getOutputEnvs(validEnvList);
//            List<JobEnv> inputEnvs = getInputEnvs(validEnvList, nextStep.getIn(), job);
//            List<JobEnv> outputEnvs = createOutputEnvs(inputEnvs, nextStep, job, nextRun);

            jobEnvRepository.saveAll(outputEnvs);

            List<JobFile> jobFiles = jobFileRepository.findJobFilesInJob(jobId);

            List<String> command = new ArrayList<>();
            command = ListUtils.union(command, getS3CopyInCommand(jobFiles, nextStep));
            command.add(commandLineService.getEchoString(inputEnvs, nextStep.getRun().getCommand()));
            command = ListUtils.union(command, getS3CopyOutCommand(outputEnvs, pipeline));
        }

       // for(Step nextStep: nextSteps) {
//            job.getRuns()
//            Run nextRun = runRepository.findRun(jobId, nextStep.getId());
//            nextRun.setStatus(JobStatus.Pending);





            //kubeClientService.runJob(taskId, jobId, nextRun.getId(), "job", inputEnvs, nextStep.getRun().getImage(), command);
        }
    }

}
