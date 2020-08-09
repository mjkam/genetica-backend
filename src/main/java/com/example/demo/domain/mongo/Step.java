package com.example.demo.domain.mongo;

import com.example.demo.domain.mysql.JobFile;
import com.example.demo.domain.mysql.Run;
import com.example.demo.service.AwsCommand;
import com.example.demo.util.Bash;
import io.kubernetes.client.openapi.models.V1EnvVar;
import lombok.*;

import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Step {
    private String id;
    private String label;
    private List<StepIO> in;
    private List<StepIO> out;
    private Tool tool;

    public Boolean isRunnable(List<JobFile> finishedRuns) {
        return in.stream().allMatch(input -> input.isReady(finishedRuns));
    }

    public Map<String,String> createEnvMap(List<JobFile> jobFiles) {
        Map<String, String> env = new HashMap<>();
        for(StepIO io: in) {
            JobFile inputJobFile = jobFiles.stream().filter(jf -> jf.getTargetId().equals(io.getSource())).findFirst().orElseThrow(() -> new RuntimeException());
            env.put(io.getId(), inputJobFile.getFile().getName());
        }
        return env;
    }

    public List<String> createCopyInputCmds(Map<String, String> bashEnvs) {
        List<String> inputFileNames = in.stream().map(input -> bashEnvs.get(input.getId())).collect(Collectors.toList());
        return inputFileNames.stream().map(name -> AwsCommand.createFileDownloadCmd(name)).collect(Collectors.toList());
    }

    public String getImageName() {
        return tool.getImage();
    }

    public String getCmd(Map<String, String> bashEnvs) {
        return Bash.runEcho(bashEnvs, tool.getCommand());
    }

    public List<String> createCopyOutputCmds(Map<String, String> bashEnvs, Pipeline pipeline) {
        List<String> fileNames = new ArrayList<>();
        for(StepIO stepIO: out) {
            String ioId = id + "." + stepIO.getId();
            if(pipeline.isOutput(ioId)) {
                fileNames.add(Bash.runEcho(bashEnvs, stepIO.getScript()));
            }
        }

        return fileNames.stream().map(name -> AwsCommand.createFileUploadCmd(name)).collect(Collectors.toList());
    }
}
