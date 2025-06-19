package org.bigdata.rag_legal.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Reference {
    private String content;

    public Reference(String content) {
        this.content = content;
    }
}
