package com.example.demo.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
public class RunPipelineRequest {
    private String pipelineId;
    private List<InsertFileInfo> data = new ArrayList<>();
}
