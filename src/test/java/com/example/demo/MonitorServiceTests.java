package com.example.demo;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.domain.mysql.Job;
import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.service.MonitorService;
import com.example.demo.service.helper.OutputData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MonitorServiceTests {
    @Autowired
    MonitorService monitorService;

    @Autowired
    PipelineRepository pipelineRepository;

    @Test
    public void test() {
        Pipeline pipeline = pipelineRepository.findAll().get(0);
        Job job = new Job();
        List<V1EnvVar>
        OutputData outputData = createOutputData(pipeline, "stepId", kubeEnvs, "L001", job);
        monitorService.createOutputData()
    }
}
