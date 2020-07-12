package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.JobEnv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JobEnvRepository extends JpaRepository<JobEnv, Long> {
    @Query("SELECT j FROM JobEnv j WHERE j.job.id = :jobId AND j.envKey = :envKey")
    public JobEnv find(Long jobId, String envKey);
}
