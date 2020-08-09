package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.JobStatus;
import com.example.demo.domain.mysql.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RunRepository extends JpaRepository<Run, Long> {
    @Query("SELECT r FROM Run r WHERE r.job.id = :jobId AND r.stepId = :stepId")
    Run findRun(Long jobId, String stepId);

    @Query("SELECT r FROM Run r WHERE r.job.id = :jobId")
    List<Run> findRunsInJob(Long jobId);

    @Query("SELECT r FROM Run r JOIN FETCH r.job j JOIN FETCH j.task WHERE r.id = :runId")
    Run findRunWithJoinById(Long runId);

    @Query("SELECT r FROM Run r WHERE r.job.id = :jobId AND r.status = :status")
    List<Run> findInJobByStatus(Long jobId, JobStatus status);

    @Query("SELECT r FROM Run r WHERE r.job.id = :jobId AND (r.status = 'Queued' OR r.status = 'Pending')")
    List<Run> findNotStarted(Long jobId);
}
