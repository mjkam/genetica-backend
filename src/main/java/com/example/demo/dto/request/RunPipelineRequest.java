package com.example.demo.dto.request;

import com.example.demo.domain.mysql.File;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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



