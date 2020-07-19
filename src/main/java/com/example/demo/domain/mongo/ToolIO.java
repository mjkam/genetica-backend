package com.example.demo.domain.mongo;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolIO {
    String id;
    String source;
    String label;
    String type;
    String script;
}
