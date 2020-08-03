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
    private final RunRepository runRepository;
    private final JobFileRepository jobFileRepository;
    private final CommandLineService commandLineService;
    private final FileRepository fileRepository;


    public void handleInitializer(Long taskId, Long jobId, Long runId, JobStatus resultStatus, String nodeName) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException());
        Run run = runRepository.findById(runId).orElseThrow(() -> new RuntimeException());
        run.changeStatus(resultStatus);

        if(resultStatus.equals(JobStatus.Succeeded)) {
            kubeClientService.addLabelToNode(nodeName, job.getId());
            //runNextRun(taskId, jobId);
        }
    }

    public void handleAnalysis(Long taskId, Long jobId, Long runId, JobStatus resultStatus) {
//        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException());
//        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException());
//        Run run = runRepository.findById(runId).orElseThrow(() -> new RuntimeException());
//        run.changeStatus(resultStatus);
//
//        if(resultStatus.equals(JobStatus.Succeeded)) {
//            jobEnvRepository.updateJobEnvRelatedtoRun(jobId, runId);
//
//            List<JobEnv> sampleEnvs = jobEnvRepository.findSampleInRun(jobId, runId);
//            List<JobEnv> outputEnvs  = jobEnvRepository.findNewOutput(jobId, runId);
//            Pipeline pipeline = pipelineRepository.findById(task.getPipelineId()).orElseThrow(() -> new RuntimeException());
//
//            for(JobEnv jobEnv: outputEnvs) {
//                for(ToolIO toolIO : pipeline.getOutputs()) {
//                    if(toolIO.getSource().equals(jobEnv.getEnvKey())) {
//                        File file = new File();
//                        file.setName(jobEnv.getEnvVal());
//                        if(sampleEnvs.size() > 0) {
//                            file.setSampleId(sampleEnvs.get(0).getEnvVal());
//                        }
//                        file.setSize(1000L);//Todo: S3 에서 가져와야함
//                        fileRepository.save(file);
//
//                        JobFile jobFile = new JobFile();
//                        jobFile.setIoType("output");
//                        jobFile.setJob(job);
//                        jobFile.setFile(file);
//                        jobFile.setTargetId(toolIO.getId());
//                        jobFileRepository.save(jobFile);
//                    }
//                }
//            }
//
//            runNextRun(taskId, jobId);
//        }
    //}



//    private List<JobEnv> getInputEnvs(List<JobEnv> envs, List<StepIO> stepInputs, Job job) {
//        List<JobEnv> newJobEnvs = new ArrayList<>();
//        for(StepIO stepInput: stepInputs) {
//            for(JobEnv env: envs) {
//                if(stepInput.getSource().equals(env.getEnvKey())) {
//                    JobEnv jobEnv = new JobEnv(job, stepInput.getId(), env.getEnvVal(), true);
//                    newJobEnvs.add(jobEnv);
//                }
//            }
//        }
//
//        for(JobEnv j: envs) {
//            if(j.getEnvKey().equals("sample")) {
//                JobEnv jobEnv = new JobEnv(job, j.getEnvKey(), j.getEnvVal(), true);
//                newJobEnvs.add(jobEnv);
//            }
//        }
//
//        return newJobEnvs;
//    }
//
//    private List<String> getS3CopyInCommand(List<JobFile> jobFiles, Step step) {
//        List<JobFile> inputJobFiles = jobFiles.stream().filter(jf -> jf.getIoType().equals("input")).collect(Collectors.toList());
//        List<String> commands = new ArrayList<>();
//        for(StepIO stepIO: step.getIn()) {
//            for(JobFile jobFile: inputJobFiles) {
//                if(stepIO.getSource().equals(jobFile.getTargetId())) {
//                    commands.add(String.format("aws s3 cp s3://genetica/%s .", jobFile.getFile().getName()));
//                }
//            }
//        }
//        return commands;
//    }

//    private List<String> getS3CopyOutCommand(List<JobEnv> outputEnvs, Pipeline pipeline) {
//        List<String> commands = new ArrayList<>();
//        for(JobEnv jobEnv: outputEnvs) {
//            for(ToolIO toolIO : pipeline.getOutputs()) {
//                if(toolIO.getSource().equals(jobEnv.getEnvKey())) {
//                    commands.add(String.format("aws s3 cp %s s3://genetica/", jobEnv.getEnvVal()));
//                }
//            }
//        }
//        return commands;
    }

//    private List<JobEnv> createOutputEnvs(List<JobEnv> inputEnvs, Step step, Job job) {
//        List<JobEnv> outputEnvs = new ArrayList<>();
//        for(StepIO stepIO: step.getOut()) {
//            JobEnv jobEnv = new JobEnv(job, step.getId() + "." + stepIO.getId(), commandLineService.getEchoString(inputEnvs, stepIO.getScript()), false);
//            outputEnvs.add(jobEnv);
//        }
//        return outputEnvs;
//    }

//    public void runNextRun(Long taskId, Long jobId) {
//        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException());
//        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException());
//        Pipeline pipeline = pipelineRepository.findById(task.getPipelineId()).orElseThrow(() -> new RuntimeException());
//        List<JobEnv> validEnvs = jobEnvRepository.findAllValidEnvsInJob(jobId);
//
//        List<Step> nextSteps = pipeline.getNextSteps(validEnvs);
//
//        List<KubeJob> nextKubeJobs = findNextKubeJobs(task, job, pipeline, validEnvs);
//        for(KubeJob kubeJob: nextKubeJobs) {
//            kubeClientService.runJob(kubeJob);
//        }
//    }

//    public List<KubeJob> findNextKubeJobs(Task task, Job job, Pipeline pipeline) {
//        Long taskId = task.getId();
//        Long jobId = job.getId();
//        List<KubeJob> kubeJobs = new ArrayList<>();
//        //List<Step> nextSteps = pipeline.getNextSteps(validEnvs);
//        //다음 잡을 찾음
//        //그 잡에 대한 명령어를 만들어야함
//        //
//
//
////        for(Step nextStep: nextSteps) {
//////            Run nextRun = runRepository.findRun(jobId, nextStep.getId());
//////            nextRun.setStatus(JobStatus.Pending);
////
////            List<JobEnv> inputEnvs = getInputEnvs(validEnvs, nextStep.getIn(), job);
////            List<JobEnv> outputEnvs = createOutputEnvs(inputEnvs, nextStep, job);
////
////            //Todo: 밖으로 빼고싶음
//////            jobEnvRepository.saveAll(outputEnvs);
////
////            List<JobFile> jobFiles = jobFileRepository.findJobFilesInJob(jobId);
////
////            List<String> command = new ArrayList<>();
////            command = ListUtils.union(command, getS3CopyInCommand(jobFiles, nextStep));
////
////            command.add(commandLineService.getEchoString(inputEnvs, nextStep.getRun().getCommand()));
////            command = ListUtils.union(command, getS3CopyOutCommand(outputEnvs, pipeline));
////
////            //kubeJobs.add(new KubeJob(taskId, jobId, nextRun.getId(), KubeJobType.ANALYSIS, inputEnvs, nextStep.getRun().getImage(), command));
////        }
////        return kubeJobs;
//        return null;
//    }

}
