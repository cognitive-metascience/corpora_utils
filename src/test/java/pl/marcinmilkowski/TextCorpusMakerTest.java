package pl.marcinmilkowski;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TextCorpusMakerTest {

  @Test
  void testGetWordCount() {
    assertEquals(3, TextCorpusMaker.getWordCount("<H1>This is it</H1>"));
    assertEquals(3, TextCorpusMaker.getWordCount("<H1>This <b>is</b> it</H1>"));
    assertEquals(3, TextCorpusMaker.getWordCount("<H1>This <b>is</b> <!-- really!!!--> it</H1>"));
    assertNotEquals(3, TextCorpusMaker.getWordCount("<H1>This is <strong>not</strong> it.</H1>"));
  }

  @Test
  void testIsReview() {
    Path f = Paths.get("eLife.00003.xml");
    assertFalse(TextCorpusMaker.isReview(f));
    f = Paths.get("eLife.00003.a013.xml");
    assertTrue(TextCorpusMaker.isReview(f));
  }

  @Test
  public void testMultipleSentences() {
    TextCorpusMaker textCorpusMaker = new TextCorpusMaker();
    String text = "This is the first sentence. This is the second sentence.";
    List<String> expected = Arrays.asList("This is the first sentence. ", "This is the second sentence.");
    List<String> result = textCorpusMaker.sentenceTokenize(text);
    assertEquals(expected, result);
  }

  @Test
  public void testOneSentence() {
    TextCorpusMaker textCorpusMaker = new TextCorpusMaker();
    String text = "This is the only sentence.";
    List<String> expected = Collections.singletonList("This is the only sentence.");
    List<String> result = textCorpusMaker.sentenceTokenize(text);
    assertEquals(expected, result);
    text = "This is the only sentence.";
    expected = Collections.singletonList("This is the only sentence.");
    result = textCorpusMaker.sentenceTokenize(text);
    assertEquals(expected, result);
    text = "This is the only sentence.\n";
    expected = Collections.singletonList("This is the only sentence.\n");
    result = textCorpusMaker.sentenceTokenize(text);
    assertEquals(expected, result);
    }

  @Test
  public void testOneSentenceTextAndValidFilterPattern() {
    TextCorpusMaker textCorpusMaker = new TextCorpusMaker();
    String text = "This is a valid sentence.";
    Pattern filter = Pattern.compile(".*valid.*");
    List<String> expected = Collections.singletonList("This is a valid sentence." + System.lineSeparator());
    List<String> actual = textCorpusMaker.getFilteredText(text, filter);
    assertEquals(expected, actual);
  }

  @Test
  public void testMultipleSentencesTextAndValidFilterPatternMatchingOneSentence() {
    TextCorpusMaker textCorpusMaker = new TextCorpusMaker();
    String text = "This is a valid sentence. This is another valid sentence.";
    Pattern filter = Pattern.compile(".*\\banother\\b.*");
    List<String> expected = Collections.singletonList("This is another valid sentence." + System.lineSeparator());
    List<String> actual = textCorpusMaker.getFilteredText(text, filter);
    assertEquals(expected, actual);
  }

  @Test
  public void testMultipleSentencesTextAndValidFilterPatternMatchingNoSentences() {
    TextCorpusMaker textCorpusMaker = new TextCorpusMaker();
    String text = "This is a valid sentence. This is another valid sentence.";
    Pattern filter = Pattern.compile("invalid");
    List<String> expected = Collections.emptyList();
    List<String> actual = textCorpusMaker.getFilteredText(text, filter);
    assertEquals(expected, actual);
  }


  @Test
  public void testGetCleanText() {
    // The input string contains one or more XML entities.
    String input = "This is a test string with &amp; and &lt; entities";
    String expected = "This is a test string with & and < entities";

    String result = TextCorpusMaker.getCleanText(input);

    assertEquals(expected, result);
    // The input string contains only one XML entity.
    input = "&amp;";
    expected = "&";
    result = TextCorpusMaker.getCleanText(input);

  }

    @Test
    public void testConvertXmlEntityToUtf8() {
      // The input string contains no XML entities.

      String input = "This is a test string";
      String expected = "This is a test string";

      String result = TextCorpusMaker.convertXmlEntityToUtf8(input);

      assertEquals(expected, result);

      assertEquals(expected, result);
    // The input string is null.
       input = null;
       expected = null;

       result = TextCorpusMaker.convertXmlEntityToUtf8(input);
      input = "";
       expected = "";

       result = TextCorpusMaker.convertXmlEntityToUtf8(input);

      assertEquals(expected, result);
    // Test whitespace  {
       input = "   ";
       expected = "   ";
       result = TextCorpusMaker.convertXmlEntityToUtf8(input);

      assertEquals(expected, result);
    }

  

}