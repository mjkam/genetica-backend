package com.example.demo.service;

import com.example.demo.domain.mysql.JobEnv;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

@Service
public class CommandLineService {

    public String getEchoString(List<JobEnv> jobEnvs, String script) {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "echo " + script);
        Map<String, String> envMap = pb.environment();
        for(JobEnv env: jobEnvs) {
            envMap.put(env.getEnvKey(), env.getEnvVal());
        }

        try {
            Process p = pb.start();
            String output = loadStream(p.getInputStream());
            String error = loadStream(p.getErrorStream());
            int rc = p.waitFor();
            //System.out.println("Process ended with rc=" + rc);
            //System.out.println("\nStandard Output:\n");
            //System.out.println(output);
            //System.out.println("\nStandard Error:\n");
            //System.out.println(error);
            return output.replace("\n", "").replace("\r", "");
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private static String loadStream(InputStream s) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(s));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null)
            sb.append(line).append("\n");
        return sb.toString();
    }
}
