package com.example.demo.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class InsertFileInfo {
    private String id;
    private List<Long> fileIds = new ArrayList<>();
}
