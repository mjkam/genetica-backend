package com.example.demo.service.helper;

import com.example.demo.domain.mysql.Job;
import com.example.demo.domain.mysql.JobFile;
import com.example.demo.domain.mysql.Run;
import com.example.demo.domain.mysql.Task;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.KubeJobType;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class TaskData {
    Task task;
    List<Job> jobs = new ArrayList<>();
    List<Run> runs = new ArrayList<>();
    List<JobFile> jobFiles = new ArrayList<>();
    Map<Job, List<V1EnvVar>> envs = new HashMap<>();

    public void addJob(Job job) {
        this.jobs.add(job);
    }

    public void addRuns(List<Run> runs) {
        this.runs.addAll(runs);
    }

    public void addJobFiles(List<JobFile> jobFiles) {
        this.jobFiles.addAll(jobFiles);
    }

    public void addV1EnvVars(List<V1EnvVar> envs, Job job) {
        this.envs.put(job, envs);
    }

    public List<KubeJob> createInitializerKubeJobs() {
        List<KubeJob> kubeJobs = new ArrayList<>();
        for(Job job: jobs) {
            List<Run> filteredRuns = runs.stream().filter(r -> r.getJob().equals(job)).collect(Collectors.toList());
            for(Run filteredRun: filteredRuns) {
                List<V1EnvVar> jobEnvs = this.envs.get(job);
                kubeJobs.add(new KubeJob(this.task.getId(), job.getId(), filteredRun.getId(), KubeJobType.INITIALIZER, jobEnvs));
            }
        }
        return kubeJobs;
    }
}