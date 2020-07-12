package com.example.demo.service;

import com.google.gson.JsonObject;
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

@Service
public class KubeClientService {
    public void addLabelToNode(String nodeName, String label) {
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

            V1Patch patch = new V1Patch("{\"metadata\":{\"labels\":{\"jobId\":\"" + label + "\"}}}");
            api.patchNode(nodeName, patch, null, null, V1Patch.PATCH_FORMAT_JSON_MERGE_PATCH, null);
        } catch(ApiException e) {
            System.out.println(e.getResponseBody());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void runJob(Long jobId, Long runId, String type, Map<String, String> envMap, String imageName, String command) {
        List<V1EnvVar> envs = new ArrayList<>();
        for(String k: envMap.keySet()) {
            V1EnvVar v = new V1EnvVar();
            v.setName(k);
            v.setValue(envMap.get(k));
            envs.add(v);
        }

        Map<String, String> labels = new HashMap<>();
        labels.put("type", type);
        labels.put("jobId", String.valueOf(jobId));
        labels.put("runId", String.valueOf(runId));

        try {
            V1Job pod =
                    new V1JobBuilder()
                            .withApiVersion("batch/v1")
                            .withKind("Job")
                            .withNewMetadata()
                            .withName(String.valueOf(System.nanoTime()))
                            .withLabels(labels)
                            .endMetadata()
                            .withNewSpec()
                            .withNewTemplate()
                            .withNewSpec()
                            .addNewContainer()
                            .withName(jobId + imageName)
                            .withImage(imageName)
                            .withCommand(Arrays.asList("/bin/bash", "-c"))
                            .withArgs(command)
                            .withEnv(envs)
                            .endContainer()
                            .withRestartPolicy("Never")
                            .endSpec()
                            .endTemplate()
                            .withBackoffLimit(1)
                            .endSpec()
                            .build();

            ApiClient client = Config.defaultClient();
            Configuration.setDefaultApiClient(client);

            //CoreV1Api api = new CoreV1Api();
            BatchV1Api api = new BatchV1Api();
            api.createNamespacedJob("default", pod, null, null, null);
            System.out.println("Job started");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
