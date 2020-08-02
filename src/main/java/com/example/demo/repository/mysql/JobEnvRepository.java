//package com.example.demo.repository.mysql;
//
//import com.example.demo.domain.mysql.JobEnv;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Modifying;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//public interface JobEnvRepository extends JpaRepository<JobEnv, Long> {
//    @Query("SELECT j FROM JobEnv j WHERE j.job.id = :jobId AND j.isValid = true")
//    public List<JobEnv> findAllValidEnvsInJob(Long jobId);
//
//    @Transactional
//    @Modifying
//    @Query("UPDATE JobEnv j SET j.isValid = true WHERE j.job.id = :jobId AND j.runId = :runId")
//    public void updateJobEnvRelatedtoRun(Long jobId, Long runId);
//
//    @Query("SELECT j FROM JobEnv j WHERE j.job.id = :jobId AND j.runId = :runId")
//    public List<JobEnv> findNewOutput(Long jobId, Long runId);
//
//    @Query("SELECT j FROM JobEnv j WHERE j.envKey = 'sample' AND j.job.id = :jobId AND j.runId = :runId")
//    public List<JobEnv> findSampleInRun(Long jobId, Long runId);
//}
