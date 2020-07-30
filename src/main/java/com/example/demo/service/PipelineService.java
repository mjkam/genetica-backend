package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.request.InputFileInfo;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final FileRepository fileRepository;
    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final RunRepository runRepository;
    private final JobEnvRepository jobEnvRepository;
    private final KubeClientService kubeClientService;

    private final TaskRepository taskRepository;

    private Pipeline findPipelineById(String pipelineId) {
        Optional<Pipeline> pipelineOptional = pipelineRepository.findById(pipelineId);
        if(!pipelineOptional.isPresent()) throw new RuntimeException();
        return pipelineOptional.get();
    }

    private Map<String, List<File>> createInputFileMap(List<InputFileInfo> inputFileInfos) {
        Map<String, List<File>> inputs = new HashMap<>();

        for(InputFileInfo inputFileInfo: inputFileInfos) {
            List<File> files = fileRepository.findByIdIn(inputFileInfo.getFileIds());
            inputs.put(inputFileInfo.getId(), files);
        }

        return inputs;
    }

    private Integer getMaxNumOfInputFile(Map<String, List<File>> inputsMap) {
        int maxNum = 0;
        for(String inputTargetId: inputsMap.keySet()) {
            maxNum = Math.max(inputsMap.get(inputTargetId).size(), maxNum);
        }
        return maxNum;
    }

    private Map<String, File> getToolInputFileMap(Map<String, List<File>> inputsMap, int idx) {
        Map<String, File> toolInputFileMap = new HashMap<>();
        for(String ioId: inputsMap.keySet()) {
            List<File> files = inputsMap.get(ioId);
            toolInputFileMap.put(ioId, files.get(idx % files.size()));
        }
        return toolInputFileMap;
    }

    public void runPipeline(RunPipelineRequest request) {
        Pipeline pipeline = findPipelineById(request.getPipelineId());
        Map<String, List<File>> inputsMap = createInputFileMap(request.getData());
        Integer maxLen = getMaxNumOfInputFile(inputsMap);
        List<String> stepIds = pipeline.getStepIds();

        Task newTask = new Task(pipeline);
        taskRepository.save(newTask);

        for(int i=0; i<maxLen; i++) {
            Map<String, File> toolInputFileMap = getToolInputFileMap(inputsMap, i);

            Job job = new Job(newTask, i);
            jobRepository.save(job);

            for(int j=0; j<stepIds.size(); j++) {
                Run run = new Run(job, stepIds.get(j));
                runRepository.save(run);
            }

            List<JobEnv> jobEnvs = new ArrayList<>();
            for(String ioId: toolInputFileMap.keySet()) {
                JobFile jobFile = new JobFile(job, toolInputFileMap, ioId);
                jobFileRepository.save(jobFile);

                JobEnv jobEnv = new JobEnv(job, ioId, toolInputFileMap.get(ioId).getSampleId(), true);
                jobEnvRepository.save(jobEnv);
                jobEnvs.add(jobEnv);

                if(toolInputFileMap.get(ioId).getSampleId() != null) {
                    JobEnv jobEnv2 = new JobEnv(job, "sample", toolInputFileMap.get(ioId).getSampleId(), true);
                    jobEnvs.add(jobEnv2);
                    jobEnvRepository.save(jobEnv2);
                }

            }
            kubeClientService.runJob(newTask.getId(), job.getId(), 0L, "initializer", jobEnvs, "338282184009.dkr.ecr.ap-northeast-2.amazonaws.com/myrepo:genetica_base", Arrays.asList("rm -rf *"));
        }
    }
}

///1. 아웃풋 파일 넣기
///2. 인풋파일과 아웃풋 파일 task