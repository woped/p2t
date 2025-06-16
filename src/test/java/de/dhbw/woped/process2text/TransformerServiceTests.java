package de.dhbw.woped.process2text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dhbw.woped.process2text.service.TransformerService;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class TransformerServiceTest {

  private final TransformerService transformerService = new TransformerService();

  @Test
  void testCheckForBPMNorPNML_BPMN()
      throws ParserConfigurationException, IOException, SAXException {
    String bpmnXml = "<definitions></definitions>";
    String result = transformerService.checkForBPMNorPNML(bpmnXml);
    assertEquals("BPMN", result);
  }

  @Test
  void testCheckForBPMNorPNML_PNML()
      throws ParserConfigurationException, IOException, SAXException {
    String pnmlXml = "<pnml></pnml>";
    String result = transformerService.checkForBPMNorPNML(pnmlXml);
    assertEquals("PNML", result);
  }

  @Test
  void testCheckForBPMNorPNML_Unknown()
      throws ParserConfigurationException, IOException, SAXException {
    String unknownXml = "<unknown></unknown>";
    String result = transformerService.checkForBPMNorPNML(unknownXml);
    assertEquals("Unknown", result);
  }

  @Test
  void testTransform() {
    String direction = "pnmltobpmn";
    String pnmlXml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<pnml>\n"
            + "  <net type=\"http://www.pnml.org/version-2009/grammar/pnml\" id=\"net1\">\n"
            + "    <page id=\"page1\">\n"
            + "      <place id=\"p1\">\n"
            + "        <name><text>Start</text></name>\n"
            + "      </place>\n"
            + "      <transition id=\"t1\">\n"
            + "        <name><text>Process</text></name>\n"
            + "      </transition>\n"
            + "      <place id=\"p2\">\n"
            + "        <name><text>End</text></name>\n"
            + "      </place>\n"
            + "    </page>\n"
            + "  </net>\n"
            + "</pnml>";
    String result = transformerService.transform(direction, pnmlXml);

    assertTrue(
        result.contains("<?xml") || result.contains("<definitions"),
        "Die Antwort sollte gültiges XML sein");
  }
}
