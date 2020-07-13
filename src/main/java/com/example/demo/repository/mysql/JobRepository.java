package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    @Query("SELECT j FROM Job j WHERE j.task.id = :taskId")
    List<Job> getJobsInTask(Long taskId);
}
