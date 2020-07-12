package com.example.demo.async;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mysql.Job;
import com.example.demo.domain.mysql.JobEnv;
import com.example.demo.domain.mysql.Run;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.JobEnvRepository;
import com.example.demo.repository.mysql.JobRepository;
import com.example.demo.repository.mysql.RunRepository;
import com.example.demo.service.CommandLineService;
import com.example.demo.service.KubeClientService;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Watch;

import java.util.*;

public class KubeEventHandler implements Runnable {
    private Watch.Response<V1Pod> pod;
    private KubeClientService kubeClientService;
    private JobRepository jobRepository;
    private PipelineRepository pipelineRepository;
    private JobEnvRepository jobEnvRepository;
    private RunRepository runRepository;
    private CommandLineService commandLineService;

    public KubeEventHandler(Watch.Response<V1Pod> pod,
                            KubeClientService kubeClientService,
                            PipelineRepository pipelineRepository,
                            JobEnvRepository jobEnvRepository,
                            RunRepository runRepository,
                            CommandLineService commandLineService,
                            JobRepository jobRepository) {
        this.pod = pod;
        this.kubeClientService = kubeClientService;
        this.pipelineRepository = pipelineRepository;
        this.jobEnvRepository = jobEnvRepository;
        this.jobRepository = jobRepository;
        this.runRepository = runRepository;
        this.commandLineService = commandLineService;
    }
    @Override
    public void run() {
        Map<String, String> labels = pod.object.getMetadata().getLabels();
        String type = labels.get("type");
        Long jobId = Long.valueOf(labels.get("jobId"));
        Long runId = Long.valueOf(labels.get("runId"));
        String resultState = pod.object.getStatus().getPhase();
        String nodeName = pod.object.getSpec().getNodeName();

        if(type.equals("initializer")) {
            handleInitializer(jobId, resultState, nodeName);
        }

        if(type.equals("job")) {
            handleJobResult(jobId, runId, resultState);
        }

        //System.out.println(job.object.getMetadata().getLabels().get("job-name") + job.object.getStatus());*/
    }

    public void handleInitializer(Long jobId, String resultState, String nodeName) {
        //job state 변경
        if(resultState.equals("Succeeded")) {
            kubeClientService.addLabelToNode(nodeName, String.valueOf(jobId));
            runNextRun(jobId);
        }
    }

    public void handleJobResult(Long jobId, Long runId, String resultState) {
        Run run = runRepository.findById(runId).get();
        run.setStatus(resultState);

        if(resultState.equals("Succeeded")) {
            runNextRun(jobId);
        }
    }

    public void runNextRun(Long jobId) {
        //Job, Pipeline, JobEnv  FETCH
        //Step 을 돌면서 다음 Step 을 찾아
        //Step 에 해당하는 Run 찾아서
        //다음 Step 에 들어갈 환경변수 Map 을 리포지토리에 저장





        Job job = jobRepository.findById(jobId).get();
        Pipeline pipeline = pipelineRepository.findById(job.getTask().getPipelineId()).get();

        for(Step step : pipeline.getSteps()) {
            Map<String, String> envMap = new HashMap<>();
            Boolean check = true;
            String stepId = "";
            for(StepIO stepIO: step.getIn()) {
                JobEnv jobEnv = jobEnvRepository.find(jobId, stepIO.getSource());
                stepId = stepIO.getId();
                if(jobEnv == null) {
                    check = false;
                    break;
                }
                else {
                    envMap.put(stepIO.getId(), jobEnv.getEnvVal());
                }
            }
            if(check) {
                List<JobEnv> jobEnvs = new ArrayList<>();
                for(String k : envMap.keySet()) {
                    JobEnv je = new JobEnv();
                    je.setEnvKey(k);
                    je.setEnvVal(envMap.get(k));
                    je.setJob(job);
                    jobEnvs.add(je);
                    //jobEnvRepository.save(je);
                }
                Run run = runRepository.findRun(jobId, stepId);
                run.setStatus("Pending");
                String command = commandLineService.getEchoString(envMap, step.getRun().getCommand());
                System.out.println("job command: " + command);
                kubeClientService.runJob(jobId, run.getId(), "job", envMap, step.getRun().getImage(), command);
                break;
            }
        }
    }
}