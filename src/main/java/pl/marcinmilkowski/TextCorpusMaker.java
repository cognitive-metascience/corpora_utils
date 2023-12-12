package pl.marcinmilkowski;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import net.loomchild.segment.srx.SrxDocument;
import net.loomchild.segment.TextIterator;
import net.loomchild.segment.srx.SrxParser;
import net.loomchild.segment.srx.SrxTextIterator;
import net.loomchild.segment.srx.io.Srx2SaxParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextCorpusMaker {
  private static final String INDEX_DIR = "C:/marcin/LuceneIndex-plos";
  private static final String JSON_DIR = "C:/marcin/elife/";

  /** code for English segmentation, single end-of-line break **/
  private static final String EN_ONE = "EN_one";

  private static final String SEGMENT_SRX = "C:\\Users\\marcin\\IdeaProjects\\MetadataIndexer\\src\\main\\resources\\segment.srx";

  private static final Pattern ELIFE_REVIEW = Pattern.compile("eLife\\.\\d+\\.[ra](sa)?\\d+\\.xml");
  private static final Pattern XML_TAG_PATTERN = Pattern.compile("<[^>]*>");
  private static final Pattern XML_COMMENT_PATTERN = Pattern.compile("(?s)<!--.*?-->");

  private static final Pattern XML_AMP_ENTITY = Pattern.compile("&amp;");

  private static final Pattern XML_LT_ENTITY = Pattern.compile("&lt;");

  private static final Pattern XML_APOS_ENTITY = Pattern.compile("&apos;");

  private static final Pattern XML_GT_ENTITY = Pattern.compile("&gt;");

  private static final Pattern XML_QUOT_ENTITY = Pattern.compile("&quot;");

  private static final Pattern XML_NUMERICAL_ENTITY = Pattern.compile("&#(\\d+);");

  private static final SrxDocument srxDocument = createSrxDocument();

  //Adapt the file path to your needs.
  public static final Path CORPUS_FILE = Paths.get("c:/marcin/elife-understanding.txt");

  //This is now a regular expression for the term "understanding" (possibly in plural).
  //You can adapt it to your needs to get a different corpus.
  public static final String KEYWORD_FILTER = ".*\\bunderstandings?\\b.*";

  public static boolean isReview(@NotNull Path file) {
    return (ELIFE_REVIEW.matcher(file.getFileName().toString()).matches());
  }

  private static int reviews = 0;


  /**
   * Recursively get all corpus files from the specified directory.
   *
   * @param startingDir The starting directory
   */
  private static void getCorpusFiles(Path startingDir)  throws IOException, ProcessingException {

    Map<String, Integer> wordCounts = new HashMap<>();

    ObjectMapper mapper = new ObjectMapper();
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();

    // Read the byte[] from the file and convert it to a JsonNode
    JsonNode node = mapper.readTree(Files.readAllBytes(Paths.get("C:/marcin/review_schema.json")));
    // Get the JsonSchema from the JsonNode
    JsonSchema schema = schemaFactory.getJsonSchema(node);
    /* JsonSchema schema = schemaFactory.getJsonSchema(new String(Files.readAllBytes(Paths.get("C:/marcin/review_schema.json"))), StandardCharsets.UTF_8); */

    Files.walkFileTree(startingDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (file.toString().endsWith(".json")) {
          JsonNode json = mapper.readTree(new String(Files.readAllBytes(file)));

          try {
            // Validate the JSON node against the schema
            schema.validate(json);
          } catch (ProcessingException e) {
            // Handle the exception
            e.printStackTrace();
          }
          Document doc = new Document();
          doc.add(new StringField("filename", file.toString(), Field.Store.YES));
          doc.add(new StringField("path", file.toString(), Field.Store.YES));
          doc.add(new StringField("modified", Long.toString(attrs.lastModifiedTime().toMillis()), Field.Store.YES));
          doc.add(new StringField("created", Long.toString(attrs.creationTime().toMillis()), Field.Store.YES));
          doc.add(new StringField("accessed", Long.toString(attrs.lastAccessTime().toMillis()), Field.Store.YES));
          doc.add(new StringField("type", "json", Field.Store.YES));

          if (json.isObject()) {
            ObjectNode object = (ObjectNode) json;
            object.fields().forEachRemaining(entry -> {
              String key = entry.getKey();
              JsonNode value = entry.getValue();
              if (value.isTextual()) {
                doc.add(new StringField(key, value.asText(), Field.Store.YES));
              } else if (value.isNumber()) {
                doc.add(new StringField(key, value.asText(), Field.Store.YES));
              } else if (value.isBoolean()) {
                doc.add(new StringField(key, Boolean.toString(value.asBoolean()), Field.Store.YES));
              }
            });
          }

          // writer.addDocument(doc);
        } else if (file.toString().endsWith(".xml")) {
          // process xml files
          String content = Files.readString(file);
          if (isReview(file)) {
            wordCounts.put(file.toString(), getWordCount(content));
            reviews += 1;
          }
        }
        return FileVisitResult.CONTINUE;
      }
    });
    int total = wordCounts.values().stream().mapToInt(Integer::intValue).sum();
    double mean = wordCounts.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    double median = wordCounts.values().stream().sorted().skip(wordCounts.size() / 2).findFirst().orElse(0);

    System.out.println("Total: " + total + " in " + wordCounts.size() + " files.");
    System.out.println("Reviews: " + reviews);
    System.out.println("Mean: " + mean);
    System.out.println("Median: " + median);
  }

  /**
   * Generate a filtered corpus and store it in the specified file.
   *
   * @param startingDir  The starting directory for JSON files
   * @param corpusFile   The path to the output corpus file
   * @param filter  The regular expression for filtering corpus text
   * @throws IOException If an I/O error occurs
   */
  private static void getFilteredCorpusText(Path startingDir, Path corpusFile, Pattern filter)  throws IOException, ProcessingException {

    Map<String, Integer> wordCounts = new HashMap<>();

    reviews = 0;

    Files.walkFileTree(startingDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
     if (file.toString().endsWith(".xml")) {
          // process xml files
          String content = Files.readString(file);
          if (isReview(file)) {
            wordCounts.put(file.toString(), getWordCount(content));
            reviews += 1;
          }
          Files.write(corpusFile,
              getFilteredText(getCleanText(content), filter),
              StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    
    int total = wordCounts.values().stream().mapToInt(Integer::intValue).sum();
    double mean = wordCounts.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
    double median = wordCounts.values().stream().sorted().skip(wordCounts.size() / 2).findFirst().orElse(0);

    System.out.println("Total: " + total + " in " + wordCounts.size() + " files.");
    System.out.println("Reviews: " + reviews);
    System.out.println("Mean: " + mean);
    System.out.println("Median: " + median);
  }


  /**
   * Returns the text content of the given XML code.
   * The text content is obtained by removing any XML comments and tags from the code.
   * The XML comments are matched by the XML_COMMENT_PATTERN regular expression.
   * The XML tags are matched by the XML_TAG_PATTERN regular expression.
   * TODO: convert xml entities to UTF-8 / use proper XSLT conversion.
   * @param xml_code the XML code to be processed
   * @return the text content of the XML code
   */
  public static String getCleanText(String xml_code) {
    // Remove XML comments
    String text = XML_COMMENT_PATTERN.matcher(xml_code).replaceAll("");
    //Remove XML tags
    text = XML_TAG_PATTERN.matcher(text).replaceAll("");
    //Replace standard entities
    text = XML_LT_ENTITY.matcher(text).replaceAll("<");
    text = XML_GT_ENTITY.matcher(text).replaceAll(">");
    text = XML_AMP_ENTITY.matcher(text).replaceAll("&");
    text = XML_QUOT_ENTITY.matcher(text).replaceAll("\"");
    text = XML_APOS_ENTITY.matcher(text).replaceAll("'");

    //Replace numerical entities and return:
    return convertXmlEntityToUtf8(text);
  }

  public static String convertXmlEntityToUtf8(String input) {

    if (input == null) {
      return null;
    }
    
    // create a matcher to find the entities in the input
    Matcher matcher = XML_NUMERICAL_ENTITY.matcher(input);
    // create a string builder to store the output
    StringBuilder output = new StringBuilder();
    // keep track of the last position of the matcher
    int lastPos = 0;
    // loop through the matches
    while (matcher.find()) {
      // append the text before the match to the output
      output.append(input, lastPos, matcher.start());
      // get the decimal value of the entity
      int value = Integer.parseInt(matcher.group(1));
      // convert the value to a UTF-8 character and append it to the output
      output.append((char) value);
      // update the last position of the matcher
      lastPos = matcher.end();
    }
    // append the remaining text after the last match to the output
    output.append(input.substring(lastPos));
    // return the output as a string
    return output.toString();
  }


  static SrxDocument createSrxDocument() {
    try {
      try (
          InputStream inputStream = new FileInputStream(SEGMENT_SRX);
          BufferedReader srxReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
      ) {
        Map<String, Object> parserParameters = new HashMap<>();
        parserParameters.put(Srx2SaxParser.VALIDATE_PARAMETER, true);
        SrxParser srxParser = new Srx2SaxParser(parserParameters);
        return srxParser.parse(srxReader);
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not load SRX rules", e);
    }
  }

  public static List<String> getFilteredText(String text, Pattern filter) {
    List<String> filteredSentences= new ArrayList<String>();
    for (String sentence: sentenceTokenize(text)) {
      if (filter.matcher(sentence).matches()) {
        filteredSentences.add(sentence + System.lineSeparator());
      }
    }
    return filteredSentences;
  }
  
  public static List<String> sentenceTokenize(String text) {
    return tokenize(text, srxDocument, EN_ONE);
  }

  static List<String> tokenize(String text, SrxDocument srxDocument, String code) {
    List<String> segments = new ArrayList<>();
    TextIterator textIterator = new SrxTextIterator(srxDocument, code, text);
    while (textIterator.hasNext()) {
      segments.add(textIterator.next());
    }
    return segments;
  }

  /**
   * Returns the number of words in the given text.
   * A word is defined as a sequence of non-whitespace characters separated by whitespace characters.
   * The text is first cleaned by removing XML tags.
   * @param contents - the XML-formatted text to be analyzed
   * @return the word count of the text
   */
  public static int getWordCount(String contents) {
    // Split the text by whitespace characters and get the length of the resulting array
    return getCleanText(contents).split("\\s+").length;
  }

  //Main method:
  public static void main(String[] args) throws IOException, ProcessingException {
    Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
    IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
    IndexWriter writer = new IndexWriter(dir, config);

    Path startingDir = Paths.get(JSON_DIR);
    //getCorpusFiles(startingDir);

    getFilteredCorpusText(startingDir, CORPUS_FILE, Pattern.compile(KEYWORD_FILTER));

    writer.close();
  }
}
