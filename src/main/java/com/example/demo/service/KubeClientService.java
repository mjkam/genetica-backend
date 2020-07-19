package com.example.demo.service;

import com.example.demo.domain.mysql.JobEnv;
import com.google.gson.JsonObject;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NodeApi;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.proto.Meta;
import io.kubernetes.client.proto.V1;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Yaml;
import okhttp3.OkHttpClient;
import org.omg.SendingContext.RunTime;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

            ArrayList<JsonObject> arr = new ArrayList<>();

            V1Patch patch = new V1Patch("{\"metadata\":{\"labels\":{\"jobId\":\"" + jobId + "\"}}}");
            api.patchNode(nodeName, patch, null, null, V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH, null);
        } catch(ApiException e) {
            System.out.println(e.getResponseBody());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runJob(Long taskId, Long jobId, Long runId, String type, List<JobEnv> jobEnvs, String imageName, List<String> command) {
        String x = command.stream().collect(Collectors.joining(" && "));
        System.out.println(x);
        List<V1EnvVar> envs = jobEnvs.stream().map(e -> {
            V1EnvVar env = new V1EnvVar();
            env.setName(e.getEnvKey());
            env.setValue(e.getEnvVal());
            return env;
        }).collect(Collectors.toList());

        Map<String, String> labels = new HashMap<>();

        labels.put("type", type);
        labels.put("taskId", String.valueOf(taskId));
        labels.put("jobId", String.valueOf(jobId));
        labels.put("runId", String.valueOf(runId));

        String timeName = String.valueOf(System.nanoTime());
        Map<String, String> nodeSelector = new HashMap<>();
        if(!type.equals("initializer")) {
            nodeSelector.put("jobId", String.valueOf(jobId));
        }

        Map<String, Quantity> limit = new HashMap<>();
        if(type.equals("initializer")) {
            limit.put("cpu", new Quantity("3"));
            limit.put("memory", new Quantity("5000000Ki"));
        }

        V1EnvFromSource es = new V1EnvFromSource();
        V1ConfigMapEnvSource source = new V1ConfigMapEnvSource();
        source.setName("config-dev");
        es.setConfigMapRef(source);

        try {
            V1Job pod =
                    new V1JobBuilder()
                            .withApiVersion("batch/v1")
                            .withKind("Job")
                            .withNewMetadata()
                            .withName(timeName)
                            .withLabels(labels)
                            .endMetadata()
                            .withNewSpec()
                            .withNewTemplate()
                            .withNewSpec()
                            .addNewContainer()
                            .withName("job-container")
                            .withImage(imageName)
                            .withEnvFrom(es)
                            .withNewResources()
                            .withLimits(limit)
                            .and()
                            .addNewVolumeMount()
                            .withMountPath("/genetica")
                            .withName("genetica-volume")
                            .endVolumeMount()
                            .withCommand(Arrays.asList("/bin/bash", "-c"))
                            .withArgs(x)
                            .withEnv(envs)
                            .endContainer()
                            .withNodeSelector(nodeSelector)
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

            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            System.out.println(Yaml.dump(pod));

            //CoreV1Api api = new CoreV1Api();
            BatchV1Api api = new BatchV1Api();
            api.createNamespacedJob("genetica-job", pod, null, null, null);
            System.out.println("Job started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
