package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.request.InsertFileInfo;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.print.attribute.standard.JobStateReason;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final FileRepository fileRepository;
    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final RunRepository runRepository;
    private final JobEnvRepository jobEnvRepository;
    private final CommandLineService commandLineService;
    private final KubeClientService kubeClientService;

    private final TaskRepository taskRepository;

    public void runPipeline(RunPipelineRequest request) {
        Pipeline pipeline = pipelineRepository.findById(request.getPipelineId()).get();
        List<String> stepIds = pipeline.getSteps().stream().map(step -> step.getId()).collect(Collectors.toList());
        Task newTask = new Task();
        newTask.setName(pipeline.getNameId() + LocalDateTime.now());
        newTask.setPipelineId(pipeline.getId());
        newTask.setStartTime(LocalDateTime.now());
        taskRepository.save(newTask);
        //Task 생성

        Map<String, List<File>> inputs = new HashMap<>();

        int maxLen = 0;
        for(int i=0; i<request.getData().size(); i++) {
            InsertFileInfo fileInfo = request.getData().get(i);
            List<File> files = fileRepository.findByIdIn(fileInfo.getFileIds());
            inputs.put(fileInfo.getId(), files);
            maxLen = Math.max(maxLen, fileInfo.getFileIds().size());
        }

        System.out.println("maxLen: " + maxLen);
        for(int i=0; i<maxLen; i++) {
            Map<String, File> inputFile = new HashMap<>();
            for(String ioId: inputs.keySet()) {
                List<File> files = inputs.get(ioId);
                inputFile.put(ioId, files.get(i % files.size()));
            }

            Job job = new Job();
            job.setTask(newTask);
            job.setName(newTask.getName() + "-" + (i+1));
            jobRepository.save(job);

            for(int j=0; j<stepIds.size(); j++) {
                Run run = new Run();
                run.setJob(job);
                run.setStepId(stepIds.get(j));
                run.setStatus("Queued");
                runRepository.save(run);
            }

            Map<String, String> envMap = new HashMap<>();
            for(String ioId: inputFile.keySet()) {
                JobFile jobFile = new JobFile();
                jobFile.setJob(job);
                jobFile.setFile(inputFile.get(ioId));
                jobFile.setIoType("input");
                jobFile.setTargetId(ioId);

                jobFileRepository.save(jobFile);

                JobEnv jobEnv = new JobEnv();
                jobEnv.setJob(job);
                jobEnv.setEnvKey(ioId);
                jobEnv.setEnvVal(inputFile.get(ioId).getName());
                jobEnvRepository.save(jobEnv);
                envMap.put(ioId, inputFile.get(ioId).getName());

                if(inputFile.get(ioId).getSampleId() != null) {
                    JobEnv jobEnv2 = new JobEnv();
                    jobEnv2.setJob(job);
                    jobEnv2.setEnvKey("sample");
                    jobEnv2.setEnvVal(inputFile.get(ioId).getSampleId());
                    envMap.put("sample", inputFile.get(ioId).getSampleId());
                }
            }
            kubeClientService.runJob(job.getId(), 0L, "initializer", envMap, "genetica_base", "rm -rf *");
        }

    }

    /*
    public void runPipeline(RunPipelineRequest request) {
        Pipeline pipeline = pipelineRepository.findById(request.getPipelineId()).get();
        //파이프라인 검색
        Map<String, List<File>> fileMap = new HashMap<>();
        //맵 선언
        List<String> parsedTools = new ArrayList<>();

        for(InsertFileInfo insertFileInfo : request.getData()) {
            fileMap.put(insertFileInfo.getId(), fileRepository.findByIdIn(insertFileInfo.getFileIds()));
        }
        //인풋데이터와 리퀘스트 데이터 매핑해서 맵에 넣음
        for(Step step : pipeline.getSteps()) {
            if(!parsedTools.contains(step.getId())) {
                parseTool(fileMap, pipeline, step.getId(), parsedTools);
            }
        }
        
        //파싱 메서드 들어가
        //아웃풋데이터 맵에 잇는데이터 이용하여 매핑
    }*/


    /*
    public void parseTool(Map<String, List<File>> fileMap, Pipeline pipeline, String stepId, List<String> parsedTools) {
        Step step = null;
        for(Step st : pipeline.getSteps()) {
            if(st.getId().equals(stepId)) {
                step = st;
                parsedTools.add(step.getId());
                break;
            }
        }
        Map<String, List<File>> envMap = new HashMap<>();
        int maxLen = 0;
        for(StepIO in : step.getIn()) {
            if(fileMap.get(in.getSource()) != null) {
                parseTool(fileMap, pipeline, in.getSource().split("/")[0], parsedTools);
            }
            envMap.put(in.getId(), envMap.get(in.getSource()));
            maxLen = Math.max(maxLen, envMap.get(in.getSource()).size());
        }

        for(int i=0; i<maxLen; i++) {
            Map<String, String> realEnvMap = new HashMap<>();
            for(String k: envMap.keySet()) {
                List<File> list = envMap.get(k);
                File f = list.get(i % list.size());
                realEnvMap.put(k, list.get(list.size() % i).getName());
                realEnvMap.put("sample", f.getSampleId());
            }
            String cmd = commandLineService.getEchoString(realEnvMap, step.getRun().getCommand());

            for(ToolIO toolIO : step.getRun().getOutputs()) {
                toolIO.
            }
        }

    }*/
}

///1. 아웃풋 파일 넣기
///2. 인풋파일과 아웃풋 파일 task