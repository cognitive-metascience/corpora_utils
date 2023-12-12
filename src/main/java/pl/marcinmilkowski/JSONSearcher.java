package pl.marcinmilkowski;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class JSONSearcher {
  private static final String INDEX_DIR = "C:/marcin/LuceneIndex-mdpi";
  private static final String CSV_FILE = "C:/marcin/mdpi_papers.csv";
  private static final String FIELD_NAME = "doi";
  private static final String CSV_FIELD_NAME = "DOI";

  public static void main(String[] args) {
    try {
      Directory dir = FSDirectory.open(Paths.get(INDEX_DIR));
      IndexReader reader = DirectoryReader.open(dir);
      IndexSearcher searcher = new IndexSearcher(reader);

      BufferedReader csvReader = new BufferedReader(new FileReader(CSV_FILE));
      Iterable<CSVRecord> records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(csvReader);
      int count = 0;
      int recordsCount = 0;
      Set<String> uniqueFound = new HashSet<>();

      for (CSVRecord record : records) {
        String doi = record.get(CSV_FIELD_NAME);
        recordsCount++;
        Query query = new MatchAllDocsQuery();
        TopDocs docs = searcher.search(query, 100);
        for (ScoreDoc scoreDoc : docs.scoreDocs) {
          String indexedDoi = reader.document(scoreDoc.doc).get(FIELD_NAME);
          if (indexedDoi!= null && indexedDoi.equals(doi) && !uniqueFound.contains(doi)) {
            count++;
            uniqueFound.add(doi);
          }
        }
      }
      csvReader.close();

      System.out.println("Number of matching DOIs: " + count + " out of " + recordsCount);

      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  }
