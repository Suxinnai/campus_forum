package org.example.finishedbackend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testTopTopics() throws Exception {
        // 由于是未登录状态（没有传Token），但是 /api/forum/top-topic 应该能访问，我们验证状态或者看是否拦截
        mockMvc.perform(get("/api/forum/top-topic"))
                .andExpect(status().isOk());
    }

    @Test
    public void testTopicTypes() throws Exception {
        // 验证类型接口
        mockMvc.perform(get("/api/forum/types"))
                .andExpect(status().isOk());
    }
}
