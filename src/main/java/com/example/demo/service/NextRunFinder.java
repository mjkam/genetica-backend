package com.example.demo.service;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mongo.Step;
import com.example.demo.domain.mongo.StepIO;
import com.example.demo.domain.mysql.JobFile;
import com.example.demo.domain.mysql.JobStatus;
import com.example.demo.domain.mysql.Run;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.KubeJobType;
import com.example.demo.repository.mysql.JobFileRepository;
import com.example.demo.repository.mysql.RunRepository;
import com.example.demo.util.Bash;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.proto.V1;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NextRunFinder {
    private final JobFileRepository jobFileRepository;
    private final RunRepository runRepository;

    public List<KubeJob> find(Run run, String sampleId, Pipeline pipeline) {
        Long jobId = run.getJob().getId();
        Long runId = run.getId();
        List<KubeJob> kubeJobs = new ArrayList<>();

        List<Run> succeededRuns = runRepository.findInJobByStatus(jobId, JobStatus.Succeeded);
        List<JobFile> usableJobFiles = jobFileRepository.findByJobId(jobId);

        List<Step> nextSteps = pipeline.getNextSteps(succeededRuns);
        for(Step step: nextSteps) {
            List<String> commands = new ArrayList<>();
            Map<String, String> bashEnvs = step.createEnvMap(usableJobFiles);
            if(sampleId != null) bashEnvs.put("sample", sampleId);

            commands.addAll(step.createCopyInputCmds(bashEnvs));
            commands.add(step.getCmd(bashEnvs));
            commands.addAll(step.createCopyOutputCmds(bashEnvs));

            List<V1EnvVar> kubeEnvs = createKubeEnvVar(bashEnvs);

            kubeJobs.add(KubeJob.createJob(jobId, runId, kubeEnvs, step.getImageName(), commands));
        }

        return null;
    }

    private List<V1EnvVar> createKubeEnvVar(Map<String, String> bashEnvs) {
        List<V1EnvVar> list = new ArrayList<>();
        for(Map.Entry<String, String> e: bashEnvs.entrySet()) {
            V1EnvVar envVar = new V1EnvVar();
            envVar.setName(e.getKey());
            envVar.setValue(e.getValue());
            list.add(envVar);
        }
        return list;
    }


}
