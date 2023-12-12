package pl.marcinmilkowski;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class JSONIndexer {
  private static final String INDEX_DIR = "C:/marcin/LuceneIndex-plos";
  private static final String JSON_DIR = "C:/marcin/plos_review/reviewed_articles";
  //private static final String FIELD_NAME = "contents";

  public static void main(String[] args) throws IOException, ProcessingException {
    Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
    IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
    IndexWriter writer = new IndexWriter(dir, config);

    Path startingDir = Paths.get(JSON_DIR);
    ObjectMapper mapper = new ObjectMapper();
    JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
    
    // Read the byte[] from the file and convert it to a JsonNode
    JsonNode node = mapper.readTree(Files.readAllBytes(Paths.get("C:/marcin/review_schema.json")));
    // Get the JsonSchema from the JsonNode
    JsonSchema schema = schemaFactory.getJsonSchema(node);
    /* JsonSchema schema = schemaFactory.getJsonSchema(new String(Files.readAllBytes(Paths.get("C:/marcin/review_schema.json"))), StandardCharsets.UTF_8); */

    Files.walkFileTree(startingDir, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException{
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

          writer.addDocument(doc);
        }
        return FileVisitResult.CONTINUE;
      }
    });

    writer.close();
  }
}
