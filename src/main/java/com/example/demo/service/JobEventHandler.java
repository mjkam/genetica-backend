package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mysql.File;
import com.example.demo.domain.mysql.JobFile;
import com.example.demo.domain.mysql.JobStatus;
import com.example.demo.domain.mysql.Run;
import com.example.demo.dto.KubeJobType;
import com.example.demo.repository.mysql.FileRepository;
import com.example.demo.repository.mysql.JobFileRepository;
import com.example.demo.repository.mysql.JobRepository;
import com.example.demo.util.Bash;
import io.kubernetes.client.openapi.models.V1EnvVar;
import jdk.jfr.events.FileReadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JobEventHandler {
    private final KubeClient kubeClient;
    private final JobFileRepository jobFileRepository;
    private final FileRepository fileRepository;

    public void handle(Run run, Pipeline pipeline, String sampleId, KubeJobType kubeJobType, JobStatus jobStatus, String nodeName) {
        updateRunStatus(run, jobStatus);
        if(run.isSucceeded()) {
            handleSucceeded(run, pipeline, kubeJobType, sampleId, nodeName);
        }
    }

    private void handleSucceeded(Run run, Pipeline pipeline, KubeJobType kubeJobType, String sampleId, String nodeName) {
        if(kubeJobType.equals(KubeJobType.INITIALIZER)) {
            kubeClient.addLabelToNode(nodeName, run.getJob().getId()); //Todo: 컨테이너 런타임에서 라벨 붙여야할듯
            return;
        }
        Step step = pipeline.getStep(run.getStepId());
        List<JobFile> jobFiles = jobFileRepository.findByJobId(run.getJob().getId());
        Map<String, String> bashEnvs = step.createEnvMap(jobFiles);
        if(sampleId != null) bashEnvs.put("sample", sampleId);

        for(StepIO stepIO: step.getOut()) {
            String fileName = Bash.runEcho(bashEnvs, stepIO.getScript());
            File file = new File(fileName, 1000L, sampleId);
            JobFile jobFile = new JobFile(run.getJob(), file, step.getId() + "." + stepIO.getId());
            fileRepository.save(file);
            jobFileRepository.save(jobFile);
        }
    }

    private void updateRunStatus(Run run, JobStatus jobStatus) {
        run.changeStatus(jobStatus);
    }
}
