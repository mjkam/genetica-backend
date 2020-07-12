package com.example.demo.domain.mongo;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolIO {
    String id;
    String label;
    String type;
    String script;
}
