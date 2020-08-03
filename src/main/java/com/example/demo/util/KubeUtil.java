package com.example.demo.util;

import io.kubernetes.client.openapi.models.V1EnvVar;

public class KubeUtil {
    public static V1EnvVar createKubeEnv(String name, String value) {
        V1EnvVar env = new V1EnvVar();
        env.setName(name);
        env.setValue(value);
        return env;
    }
}
