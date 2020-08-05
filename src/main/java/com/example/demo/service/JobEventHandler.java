package com.example.demo.service;

import com.example.demo.domain.mysql.JobStatus;
import com.example.demo.domain.mysql.Run;
import com.example.demo.dto.KubeJobType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobEventHandler {
    private final KubeClient kubeClient;

    public void handle(Run run, KubeJobType kubeJobType, JobStatus jobStatus, String nodeName) {
        run.changeStatus(jobStatus);
        if(run.isSucceeded()) {
            if(kubeJobType.equals(KubeJobType.INITIALIZER)) {
                //Todo: 컨테이너 런타임에서 라벨 붙여야할듯
                kubeClient.addLabelToNode(nodeName, run.getJob().getId());
                return;
            }


        }
        /*
        잡의 상태변경
        잡의 아웃풋이 최종 아웃풋이면 파일생성, 잡파일생성
         */
    }
}
