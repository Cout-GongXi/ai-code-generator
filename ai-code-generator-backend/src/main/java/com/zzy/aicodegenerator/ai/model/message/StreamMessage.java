package com.zzy.aicodegenerator.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式消息响应基类
 *
 * @author zzy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamMessage {
    private String type;
}
