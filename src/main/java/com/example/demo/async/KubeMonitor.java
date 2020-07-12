package com.example.demo.async;

import com.example.demo.repository.mongo.PipelineRepository;
import com.example.demo.repository.mysql.JobEnvRepository;
import com.example.demo.repository.mysql.JobRepository;
import com.example.demo.repository.mysql.RunRepository;
import com.example.demo.service.CommandLineService;
import com.example.demo.service.KubeClientService;
import com.google.common.reflect.TypeToken;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.omg.SendingContext.RunTime;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class KubeMonitor  {
    private final ServiceExecutor serviceExecutor;
    private final JobRepository jobRepository;
    private final RunRepository runRepository;
    private final JobEnvRepository jobEnvRepository;
    private final PipelineRepository pipelineRepository;
    private final KubeClientService kubeClientService;
    private final CommandLineService commandLineService;

    public void run() {
        try {
            ApiClient client = Config.defaultClient();
            // infinite timeout
            OkHttpClient httpClient =
                    client.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
            client.setHttpClient(httpClient);
            Configuration.setDefaultApiClient(client);

            //BatchV1Api api = new BatchV1Api();
            CoreV1Api api = new CoreV1Api();

            Watch<V1Pod> watch =
                    Watch.createWatch(
                            client,
                            api.listNamespacedPodCall("default", null, null, null, null, null, null, null, null, Boolean.TRUE, null),
                            new TypeToken<Watch.Response<V1Pod>>() {}.getType());
            for (Watch.Response<V1Pod> item : watch) {
                //System.out.println(item.object.getStatus().getPhase());
                String jobName = item.object.getMetadata().getLabels().get("job-name");
                if(jobName.contains("genetica-job")) {
                    serviceExecutor.runExecutor(new KubeEventHandler(item, kubeClientService, pipelineRepository, jobEnvRepository, runRepository, commandLineService, jobRepository));
                }
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
