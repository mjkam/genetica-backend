package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

@Service
public class CommandLineService {

    public String getEchoString(Map<String, String> envMap, String script) {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "echo " + script);
        Map<String, String> env = pb.environment();
        for(String k: envMap.keySet()) {
            env.put(k, envMap.get(k));
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
            return output;
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
