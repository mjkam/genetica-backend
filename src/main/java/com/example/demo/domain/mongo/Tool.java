package com.example.demo.domain.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tool {
    @Id
    private String id;

    private String nameId;

    private String label;

    private String command;

    private String image;

    private List<ToolIO> inputs;

    private List<ToolIO> outputs;
}
