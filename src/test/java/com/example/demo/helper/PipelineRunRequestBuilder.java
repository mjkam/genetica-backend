package com.example.demo.helper;

import com.example.demo.domain.mongo.Pipeline;
import com.example.demo.dto.KubeJob;
import com.example.demo.dto.request.InputFileInfo;
import com.example.demo.dto.request.RunPipelineRequest;

import java.util.Arrays;
import java.util.List;

public class PipelineRunRequestBuilder {
    public static RunPipelineRequest createRunPipelineRequest(Pipeline pipeline) {
        RunPipelineRequest request = new RunPipelineRequest();

        InputFileInfo insertFileInfo1 = new InputFileInfo();
        insertFileInfo1.setId("input_tar_with_reference");
        insertFileInfo1.setFileIds(Arrays.asList(1L));

        InputFileInfo insertFileInfo2 = new InputFileInfo();
        insertFileInfo2.setId("input_read_1");
        insertFileInfo2.setFileIds(Arrays.asList(2L, 4L));

        InputFileInfo insertFileInfo3 = new InputFileInfo();
        insertFileInfo3.setId("input_read_2");
        insertFileInfo3.setFileIds(Arrays.asList(3L, 5L));

        request.setPipelineId("");
        request.setData(Arrays.asList(insertFileInfo1, insertFileInfo2, insertFileInfo3));

        return request;
    }
}
