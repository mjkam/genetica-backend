package com.example.demo.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
{
    "pipelineId": xx,
    "data": [
        {
            "id": xx,
            "fileIds": [1,2,3],
        }, ...
    ]
}
 */


@Data
public class RunPipelineRequest {
    private String pipelineId;
    private List<InputFileInfo> data = new ArrayList<>();
}



