package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.request.InputFileInfo;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import com.example.demo.util.KubeUtil;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PipelineRunner {
    private final PipelineRepository pipelineRepository;
    private final FileRepository fileRepository;
    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final RunRepository runRepository;
    private final TaskRepository taskRepository;


    public List<KubeJob> parseRunRequest(RunPipelineRequest request) {
        List<KubeJob> kubeJobs = new ArrayList<>();

        Map<String, List<File>> inputsMap = createInputFileMap(request.getData());
        int maxLen = getMaxNumOfInputFile(inputsMap);

        Pipeline pipeline = pipelineRepository.findById(request.getPipelineId()).orElseThrow(() -> new RuntimeException());
        Task newTask = new Task(pipeline);
        taskRepository.save(newTask);

        for(int i=0; i<maxLen; i++) {
            Job job = new Job(newTask, i);
            jobRepository.save(job);

            List<Run> runs = pipeline.getAllRuns(job);
            Run initializeRun = new Run(job, "");
            runs.add(initializeRun);
            runRepository.saveAll(runs);

            Map<String, File> jobInputFileMap = getEachJobInputFileMap(inputsMap, i);
            jobFileRepository.saveAll(createJobFilesFromInputsMap(jobInputFileMap, job));

            List<V1EnvVar> kubeEnvs = createSampleIdKubeEnv(jobInputFileMap);

            kubeJobs.add(KubeJob.createInitializer(job.getId(), initializeRun.getId(), kubeEnvs));
        }
        return kubeJobs;
    }



    public List<JobFile> createJobFilesFromInputsMap(Map<String, File> jobInputFileMap, Job job) {
        return jobInputFileMap.entrySet().stream().map(e -> new JobFile(job, e.getValue(), e.getKey())).collect(Collectors.toList());
    }

    public List<V1EnvVar> createSampleIdKubeEnv(Map<String, File> jobInputFileMap) {
        //Todo: 임시방편
        List<V1EnvVar> result = new ArrayList<>();
        for(Map.Entry<String, File> e: jobInputFileMap.entrySet()) {
            String sampleId = e.getValue().getSampleId();
            if(sampleId != null) {
                result.add(KubeUtil.createKubeEnv("sample", sampleId));
            }
        }
        return result;
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

    private Map<String, File> getEachJobInputFileMap(Map<String, List<File>> inputsMap, int idx) {
        Map<String, File> toolInputFileMap = new HashMap<>();
        for(String ioId: inputsMap.keySet()) {
            List<File> files = inputsMap.get(ioId);
            toolInputFileMap.put(ioId, files.get(idx % files.size()));
        }
        return toolInputFileMap;
    }
}
