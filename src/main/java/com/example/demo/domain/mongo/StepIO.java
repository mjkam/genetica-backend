package com.example.demo.domain.mongo;

import com.example.demo.domain.mysql.JobFile;
import com.example.demo.domain.mysql.Run;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class StepIO {
    private String id;
    private String source;
    private String script;

    public boolean isReady(List<JobFile> usableJobFiles) {
        if(source.contains(".")) return usableJobFiles.stream().anyMatch(jf -> jf.getTargetId().equals(source));
        return true;
    }
}
