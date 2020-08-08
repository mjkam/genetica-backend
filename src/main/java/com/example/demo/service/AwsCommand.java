package com.example.demo.service;

import java.util.List;

public class AwsCommand {
    public static String createFileDownloadCmd(String fileName) {
        return String.format("aws s3 cp s3://genetica/%s .", fileName);
    }

    public static String createFileUploadCmd(String fileName) {
        return String.format("aws s3 %s s3://genetica/", fileName);
    }
}


//    public List<String> createInputCommands(Map<String, String> envs) {
//        List<String> commands = new ArrayList<>();
//        for(Map.Entry<String, String> e: envs.entrySet()) {
//            commands.add(String.format("aws s3 cp s3://genetica/%s .", e.getValue()));
//        }
//        return commands;
//    }
//
//    public List<String> createOutputCommands(List<V1EnvVar> outputKubeEnvs) {
//        return outputKubeEnvs.stream().map(e -> String.format("aws s3 cp %s s3://genetica/", e.getValue())).collect(Collectors.toList());
//    }