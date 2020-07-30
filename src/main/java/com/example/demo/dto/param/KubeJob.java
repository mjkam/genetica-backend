package com.example.demo.dto.param;

import com.example.demo.domain.mysql.JobEnv;

import java.util.List;

public class KubeJob {
    Long taskId;
    Long jobId;
    Long runId;
    String jobType;
    List<JobEnv> jobEnvs;
    String imageName;
    List<String> command;
    public KubeJob(Long taskId, Long jobId, Long runId, )
}
