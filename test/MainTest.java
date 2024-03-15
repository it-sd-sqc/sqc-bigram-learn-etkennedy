import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

import edu.cvtc.bigram.*;

@SuppressWarnings({"SpellCheckingInspection"})
class MainTest {
  @Test
  void createConnection() {
    assertDoesNotThrow(
        () -> {
          Connection db = Main.createConnection();
          assertNotNull(db);
          assertFalse(db.isClosed());
          db.close();
          assertTrue(db.isClosed());
        }, "Failed to create and close connection."
    );
  }

  @Test
  void reset() {
    Main.reset();
    assertFalse(Files.exists(Path.of(Main.DATABASE_PATH)));
  }

  @Test
  void mainArgs() {
    assertAll(
        () -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setOut(new PrintStream(out));
          Main.main(new String[]{"--version"});
          String output = out.toString();
          assertTrue(output.startsWith("Version "));
        },
        () -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setOut(new PrintStream(out));
          Main.main(new String[]{"--help"});
          String output = out.toString();
          assertTrue(output.startsWith("Add bigrams"));
        },
        () -> assertDoesNotThrow(() -> {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          System.setErr(new PrintStream(out));
          Main.main(new String[]{"--reset"});
          String output = out.toString();
          assertTrue(output.startsWith("Expected"));
        }),
        () -> assertDoesNotThrow(() -> Main.main(new String[]{"./sample-texts/non-existant-file.txt"})),
        () -> assertDoesNotThrow(() -> Main.main(new String[]{"./sample-texts/empty.txt"}))
    );
  }

  // TODO: Create your test(s) below. /////////////////////////////////////////
  @Test
  void testGetId() {
      // Arrange
      String testWord = "testword";
      Connection db = Main.createConnection();

      // Act
      int wordId = -1;
      try {
          wordId = Main.getId(db, testWord);
      } catch (SQLException e) {
          fail("SQLException occurred during test: " + e.getMessage());
      }

      // Assert
      assertNotEquals(-1, wordId, "Word ID should not be -1");

      // Verify that the inserted word is without additional single quotes
      verifyInsertedWord(db, testWord, wordId);

      // Clean up
      closeDatabaseConnection(db);
  }

    private void verifyInsertedWord(Connection db, String word, int wordId) {
        try (Statement statement = db.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT string FROM words WHERE id = " + wordId);
            assertTrue(resultSet.next(), "Word should be present in the database");
            String retrievedWord = resultSet.getString("string");
            assertEquals(word, retrievedWord, "Retrieved word should match the test word");
            assertFalse(retrievedWord.startsWith("'") || retrievedWord.endsWith("'"),
                    "Retrieved word should not start or end with a single quote");
        } catch (SQLException e) {
            fail("SQLException occurred during verification: " + e.getMessage());
        }
    }

    private void closeDatabaseConnection(Connection db) {
        try {
            db.close();
        } catch (SQLException e) {
            fail("Failed to close database connection: " + e.getMessage());
        }
    }
}