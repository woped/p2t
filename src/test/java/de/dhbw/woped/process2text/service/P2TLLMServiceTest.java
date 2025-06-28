package de.dhbw.woped.process2text.service;

import static org.junit.jupiter.api.Assertions.*;

import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

public class P2TLLMServiceTest {

  private final P2TLLMService service = new P2TLLMService();
  private final String pathString = "src\\test\\java\\de\\dhbw\\woped\\process2text\\service";

  @Test
  void testCallLLM2WithValidModelAndWriteToFile() {
    OpenAiApiDTO dto = new OpenAiApiDTO("apiKey", "", "", "", false);
    dto.setProvider("llmStudio");
    dto.setGptModel("llama-3.2-1b-instruct"); // Modellname ggf. anpassen
    dto.setPrompt("Sag mir etwas über KI.");

    String body = "Dies ist ein Test.";

    try {
      String result = service.callLLM(body, dto);
      assertNotNull(result, "Antwort vom LLM war null.");
      assertFalse(result.isBlank(), "Antwort vom LLM war leer.");

      // Schreibe Ergebnis in Datei
      writeToFile(pathString, result);

    } catch (ResponseStatusException e) {
      writeToFile("target/llm-output.txt", "ResponseStatusException: " + e.getReason());
      fail("ResponseStatusException: " + e.getReason());
    } catch (Exception e) {
      writeToFile("target/llm-output.txt", "Fehler beim Aufruf von callLLM: " + e.getMessage());
      fail("Fehler beim Aufruf von callLLM: " + e.getMessage());
    }
  }

  @Test
  void testLmStudioUrlCleaning() {
    assertEquals("http://localhost:1234", cleanUrl("http://localhost:1234/v1/chat/completions"));
    assertEquals("http://localhost:1234", cleanUrl("http://localhost:1234"));
    assertEquals("https://example.com:8080", cleanUrl("https://example.com:8080/v1/chat/completions"));
    assertEquals("http://localhost:1234/api", cleanUrl("http://localhost:1234/api"));
  }

  private String cleanUrl(String url) {
    if (url.endsWith("/v1/chat/completions")) {
      return url.replace("/v1/chat/completions", "");
    }
    return url;
  }

  private void writeToFile(String path, String content) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
      writer.write(content);
    } catch (IOException e) {
      e.printStackTrace(); // nur für den Fall, dass Datei nicht geschrieben werden kann
    }
  }
}
