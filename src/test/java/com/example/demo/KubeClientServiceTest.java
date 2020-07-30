package com.example.demo;

import com.example.demo.service.KubeClientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class KubeClientServiceTest {
    @Autowired
    KubeClientService kubeClientService;
    @Test
    public void test1() {
        kubeClientService.addLabelToNode("hello", 1000L);
    }
}
