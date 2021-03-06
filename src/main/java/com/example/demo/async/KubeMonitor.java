package com.example.demo.async;

import com.example.demo.service.MonitorService;
import com.google.common.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1OwnerReference;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class KubeMonitor  {
    private final ServiceExecutor serviceExecutor;
    private final MonitorService monitorService;

    public void run() {
        try {
            System.out.println("Monitor start");
            ApiClient client = Config.defaultClient();
            // infinite timeout
            OkHttpClient httpClient =
                    client.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
            client.setHttpClient(httpClient);
            Configuration.setDefaultApiClient(client);

            BatchV1Api batchV1Api = new BatchV1Api();
            CoreV1Api coreV1Api = new CoreV1Api();

            Watch<V1Pod> watch =
                    Watch.createWatch(
                            client,
                            coreV1Api.listNamespacedPodCall("genetica-job", null, null, null, null, null, null, null, null, Boolean.TRUE, null),
                            new TypeToken<Watch.Response<V1Pod>>() {}.getType());
            for (Watch.Response<V1Pod> item : watch) {
                V1Pod pod = item.object;
                List<V1EnvVar> kubeEnvs = pod.getSpec().getContainers().get(0).getEnv();
                V1OwnerReference jobInfo = pod.getMetadata().getOwnerReferences().get(0);
                V1Job job = batchV1Api.readNamespacedJob(jobInfo.getName(), "genetica-job", null, null, null);

                serviceExecutor.runExecutor(new KubeEventHandler(job.getMetadata().getLabels(), pod.getStatus().getPhase(), pod.getSpec().getNodeName(), kubeEnvs, monitorService));
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
