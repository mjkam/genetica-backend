package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.*;
import com.example.demo.dto.request.InputFileInfo;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.KubeJobType;
import com.example.demo.dto.request.RunPipelineRequest;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.*;
import com.example.demo.service.helper.TaskData;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PipelineService {

    private final PipelineRepository pipelineRepository;
    private final FileRepository fileRepository;
    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final RunRepository runRepository;
    //private final JobEnvRepository jobEnvRepository;
    private final KubeClientService kubeClientService;
    private final TaskRepository taskRepository;



    public void runPipeline(RunPipelineRequest request) {
        Pipeline pipeline = pipelineRepository.findById(request.getPipelineId()).orElseThrow(() -> new RuntimeException());
        Map<String, List<File>> inputsMap = createInputFileMap(request.getData());
        Integer maxLen = getMaxNumOfInputFile(inputsMap);

        TaskData taskData = createTaskData(inputsMap, pipeline, maxLen);
        insertParsedTask(taskData);

        List<KubeJob> kubeJobs = createKubeJobsFromParsedTask(taskData);
        for(KubeJob job: kubeJobs) {
            kubeClientService.runJob(job);
        }
    }

    public List<KubeJob> createKubeJobsFromParsedTask(TaskData taskData) {
        List<KubeJob> kubeJobs = new ArrayList<>();
        Task task = taskData.getTask();
        for(Job job: taskData.getJobs()) {

        }
        KubeJob kubeJob = new KubeJob(task.getId(), )

        return kubeJobs;
    }

    public void insertParsedTask(TaskData taskData) {
        taskRepository.save(taskData.getTask());
        jobRepository.saveAll(taskData.getJobs());
        runRepository.saveAll(taskData.getRuns());
        jobFileRepository.saveAll(taskData.getJobFiles());
    }

    public TaskData createTaskData(Map<String, List<File>> inputsMap, Pipeline pipeline, int maxLen) {
        TaskData taskData = new TaskData();

        Task newTask = new Task(pipeline);
        taskData.setTask(newTask);

        for(int i=0; i<maxLen; i++) {
            Job job = new Job(newTask, i);
            taskData.addJob(job);
            taskData.addRuns(pipeline.getAllRuns(job));

            Map<String, File> jobInputFileMap = getEachJobInputFileMap(inputsMap, i);
            taskData.addJobFiles(createJobFilesFromInputsMap(jobInputFileMap, job));
            taskData.addV1EnvVars(createJobEnvsFromInputsMap(jobInputFileMap), job);
        }
        return taskData;
    }

    public List<JobFile> createJobFilesFromInputsMap(Map<String, File> jobInputFileMap, Job job) {
        return jobInputFileMap.entrySet().stream().map(e -> new JobFile(job, e.getValue(), e.getKey())).collect(Collectors.toList());
    }

    public List<V1EnvVar> createJobEnvsFromInputsMap(Map<String, File> jobInputFileMap) {
        List<V1EnvVar> jobEnvs = new ArrayList<>();
        for(String ioId: jobInputFileMap.keySet()) {
            jobEnvs.add(createKubeEnv(ioId, jobInputFileMap.get(ioId).getName()));
            if(jobInputFileMap.get(ioId).getSampleId() != null) {
                jobEnvs.add(createKubeEnv("sample", jobInputFileMap.get(ioId).getSampleId()));
            }
        }
        return jobEnvs;
    }

    public V1EnvVar createKubeEnv(String name, String value) {
        V1EnvVar env = new V1EnvVar();
        env.setName(name);
        env.setValue(value);
        return env;
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