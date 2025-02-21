package com.example;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class LlmHelper {

    public static ChatLanguageModel ollamaAiModel() {
        String modelName = "llama3.1:8b-instruct-q4_K_S";
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .timeout(Duration.ofMinutes(20))
                .modelName(modelName)
                .temperature(0.0)
                .numCtx(8096)
                .build();
    }

    public static void main(String[] args) throws IOException {
        ChatLanguageModel model = ollamaAiModel();
        File directory = new File("<PATH TO DIR WITH HTTP TESTS>");
        
        try (Stream<File> files = Files.walk(directory.toPath())
                .map(java.nio.file.Path::toFile)
                .filter(file -> file.isFile() && "http".equals(fileExtension(file)))) {
            List<File> testCases = files.collect(Collectors.toList());

            for (File testcase : testCases) {
                System.out.println("Processing " + testcase.getName());
                String originalFile = new String(Files.readAllBytes(testcase.toPath()));

                String response = model.generate(
                        new UserMessage("You are an expert Java developer with deep IntelliJ HTTP client test knowledge"),
                        new UserMessage("Your task is to add failure messages for IntelliJ HTTP client tests assertions"),
                        new UserMessage("""
                                Hint: Original test block:
                                > {%
                                    client.assert(response.status === 200);
                                    client.assert(response.body.action === 'TPP_CREATE_ACCESS');
                                    client.assert(response.body.stage === 'AUTH_PSU_ACK');
                                    client.assert(response.body.aspsp.name === 'Test bank (redirect, detailed, PSU ID required)');
                                    client.assert(response.body.fintech.name === 'GDNext GmbH TestTPP');
                                    client.assert(response.body.links != undefined);
                                %}
                                Correct test block with added assertions:
                                > {%
                                    client.assert(response.status === 200, "Response status is not 200");
                                    client.assert(response.body.action === 'TPP_CREATE_ACCESS', `Wrong action: ${response.body.action}`);
                                    client.assert(response.body.stage === 'AUTH_PSU_ACK', `Unexpected stage: ${response.body.stage}`);
                                    client.assert(response.body.aspsp.name === 'Test bank (redirect, detailed, PSU ID required)', `Wrong ASPSP: ${response.body.aspsp.name}`);
                                    client.assert(response.body.fintech.name === 'GDNext GmbH TestTPP', `Wrong Fintech: ${response.body.fintech.name}`);
                                    client.assert(response.body.links != undefined, 'Missing links');
                                %}
                        """),
                        new UserMessage("Add messages to this test (Provide only the updated file without quotes, references or markdown): \n\n " + originalFile)
                ).content().text();

                Files.write(testcase.toPath(), response.getBytes());
            }
        }
    }

    private static String fileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        return (lastIndex == -1) ? "" : name.substring(lastIndex + 1);
    }
}