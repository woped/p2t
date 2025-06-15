package de.dhbw.woped.process2text.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Service class to handle the transformer call for the transformation from PNML to BPMN and detect
 * which format was provided.
 */
@Service
public class TransformerService {

  private final WebClient webClient;
  private static final Logger logger = LoggerFactory.getLogger(TransformerService.class);

  /** Constructor to create a new WebClient instance containing the URL of the transformer API. */
  public TransformerService() {
    this.webClient =
        WebClient.builder().baseUrl("https://europe-west3-woped-422510.cloudfunctions.net").build();
  }

  /**
   * Calls the transformer API with the provided direction and PNML content using the webClient, and
   * retrieves the transformed BPMN content.
   *
   * @param direction The direction of the transformation (e.g., "pnmltobpmn").
   * @param pnmlXml The PNML content to be transformed.
   * @return The transformed content.
   */
  public String transform(String direction, String pnmlXml) {
    try {
      // Validiere XML-Format
      Document document = Jsoup.parse(pnmlXml, "", Parser.xmlParser());
      logger.info("XML-Format ist gültig. Root-Element: {}", document.child(0).tagName());

      String endpoint =
          UriComponentsBuilder.fromUriString("/transform")
              .queryParam("direction", direction)
              .toUriString();

      logger.info("Sende Anfrage an Transformations-Service: {}", endpoint);

      // Sende die Anfrage als XML
      String result =
          this.webClient
              .post()
              .uri(endpoint)
              .contentType(MediaType.APPLICATION_XML)
              .bodyValue(pnmlXml)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      if (result == null || result.trim().isEmpty()) {
        logger.warn("Transformations-Service hat leere Antwort zurückgegeben");
        return pnmlXml;
      }

      logger.info("Transformation erfolgreich. Ergebnis-Länge: {}", result.length());
      return result;

    } catch (Exception e) {
      logger.error("Fehler bei der Transformation: {}", e.getMessage());
      // Bei einem Fehler geben wir das Original zurück
      return pnmlXml;
    }
  }

  /**
   * Checks the root element of the provided diagram file (XML String) to determine if it is PNML or
   * BPMN.
   *
   * @param file The diagram file to be checked.
   * @return The type of the diagram ("PNML", "BPMN", or "Unknown").
   */
  public String checkForBPMNorPNML(String file) {
    // Parse the HTML content
    Document document = Jsoup.parse(file, "", Parser.xmlParser());

    // Get the root element
    Element rootElement = document.child(0);
    String rootTag = rootElement.tagName();

    // Check the root element tag name
    if ("pnml".equalsIgnoreCase(rootTag)) {
      return "PNML";
    } else if ("definitions".equalsIgnoreCase(rootTag)) {
      return "BPMN";
    } else {
      return "Unknown";
    }
  }
}
