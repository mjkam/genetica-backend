package com.example.demo.service;

import com.example.demo.domain.mysql.Job;
import com.example.demo.dto.KubeJob;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class KubeClientService {
    public void addLabelToNode(String nodeName, Long jobId) {
        try {
            ApiClient strategicMergePatchClient =
                    ClientBuilder.standard()
                            .setOverridePatchFormat(V1Patch.PATCH_FORMAT_STRATEGIC_MERGE_PATCH)
                            .build();
            strategicMergePatchClient.setDebugging(true);
            OkHttpClient httpClient =
                    strategicMergePatchClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
            strategicMergePatchClient.setHttpClient(httpClient);
            Configuration.setDefaultApiClient(strategicMergePatchClient);

            CoreV1Api api = new CoreV1Api();

            V1Patch patch = new V1Patch("{\"metadata\":{\"labels\":{\"jobId\":\"" + jobId + "\"}}}");
            api.patchNode(nodeName, patch, null, null, V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH, null);
        } catch(ApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public V1Job runJob(KubeJob kubeJob) {
        //Todo: KubeJob 실행이 실패 했을 시 처리
        V1Job v1Job = getV1Job(kubeJob);

        try {
            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            System.out.println(Yaml.dump(v1Job));

            BatchV1Api api = new BatchV1Api();
            return api.createNamespacedJob("genetica-job", v1Job, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private V1Job getV1Job(KubeJob kubeJob) {
        V1EnvFromSource es = new V1EnvFromSource();
        V1ConfigMapEnvSource source = new V1ConfigMapEnvSource();
        source.setName("config-dev");
        es.setConfigMapRef(source);

        return
                new V1JobBuilder()
                        .withApiVersion("batch/v1")
                        .withKind("Job")
                        .withNewMetadata()
                        .withName(kubeJob.getJobName())
                        .withLabels(kubeJob.getLabels())
                        .endMetadata()
                        .withNewSpec()
                        .withNewTemplate()
                        .withNewSpec()
                        .addNewContainer()
                        .withName("job-container")
                        .withImage(kubeJob.getImageName())
                        .withEnvFrom(es)
                        .withNewResources()
                        .withLimits(kubeJob.getResourceLimit())
                        .and()
                        .addNewVolumeMount()
                        .withMountPath("/genetica")
                        .withName("genetica-volume")
                        .endVolumeMount()
                        .withCommand(Arrays.asList("/bin/bash", "-c"))
                        .withArgs(kubeJob.getCommandStr())
                        .withEnv(kubeJob.getEnvVars())
                        .endContainer()
                        .withNodeSelector(kubeJob.getNodeSelector())
                        .addNewVolume()
                        .withName("genetica-volume")
                        .withNewHostPath()
                        .withPath("/home/ec2-user")
                        .withType("Directory")
                        .endHostPath()
                        .endVolume()
                        .withRestartPolicy("Never")
                        .endSpec()
                        .endTemplate()
                        .withBackoffLimit(0)
                        .endSpec()
                        .build();

    }
}
