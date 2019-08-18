import com.sun.source.tree.NewArrayTree;

import java.lang.reflect.Array;
import java.security.cert.TrustAnchor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

class User {
    int uid;
    String username, password, group;

    public User(int uid, String username, String password, String group) {
        this.uid = uid;
        this.username = username;
        this.password = password;
        this.group = group;
    }
}

class Word {
    int idx;
    String wordName, wordType, meaning;

    public Word(int idx, String wordName, String wordType, String meaning) {
        this.idx = idx;
        this.wordName = wordName;
        this.wordType = wordType;
        this.meaning = meaning;
    }
}

public class VirtualDB {
    Connection conn = connect();
    private final static Logger logger = Logger.getLogger("virtualDB");

    private Connection connect() {
        Connection conn = null;
        try {
            String url = "jdbc:sqlite:./resources/SQLite";
            conn = DriverManager.getConnection(url);
            logger.info("Connection to SQLite has been established.");

        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }
        return conn;
    }

    private void closeConn(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            logger.warning(ex.getMessage());
        }
        logger.info("[*] connection closed");
    }

    private List<Word> queryWord(String wordName) {
        List<Word> wordList = new ArrayList<Word>();
        Word word = new Word(0, null, null, null);
        String sql = "select * from words where wordName=\"" + wordName + '"';
        try (
                Statement stmt = this.conn.createStatement();
                ResultSet res = stmt.executeQuery(sql)) {
            while (res.next()) {
                word.idx = res.getInt("idx");
                word.wordName = res.getString("wordName");
                word.wordType = res.getString("wordType");
                word.meaning = res.getString("meaning");
                wordList.add(word);
            }


        } catch (SQLException e) {
            logger.warning(e.getMessage());
        }
        logger.info("[-] queried word: " + wordName + ". ");
        return wordList;
    }


    private Boolean addWord(String wordName, String wordType, String meaning) {
        String sql = "insert into words (wordName,wordType,meaning) values(?,?,?)";
        try (
                PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            pstmt.setString(1, wordName);
            pstmt.setString(2, wordType);
            pstmt.setString(3, meaning);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            return false;
        }
        logger.info("[-] added word: " + wordName + ". ");
        return true;
    }

    private Boolean editWord(int idx, String wordName, String wordType, String meaning) {
        String sql = "update words set wordName = ? , "
                + "wordType = ? ,  "
                + "meaning = ? "
                + "where idx = ?";

        try (
                PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            pstmt.setString(1, wordName);
            pstmt.setString(2, wordType);
            pstmt.setString(3, meaning);
            pstmt.setInt(4, idx);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            return false;
        }
        logger.info("[-] edited word: " + wordName + ". ");
        return true;
    }

    private Boolean removeWord(int idx) {
        String sql = "delete from words where idx = ?";

        try (
                PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            pstmt.setInt(1, idx);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            return false;
        }
        logger.info("[-] removed word: " + idx + ". ");
        return true;
    }

    public static void main(String[] args) {
        // write your code here
        VirtualDB db = new VirtualDB();
        db.queryWord("apple");
        db.addWord("banana","noun","another kind of fruit");
        db.editWord(3,"banana", "noun", "very different from apple");
        db.removeWord(3);
        db.closeConn(db.conn);
    }
}
