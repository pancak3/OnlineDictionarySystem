package Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

import org.json.simple.JSONObject;


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

    Word(int idx, String wordName, String wordType, String meaning) {
        this.idx = idx;
        this.wordName = wordName;
        this.wordType = wordType;
        this.meaning = meaning;
    }
}

public class Database {
    private Connection conn = connect();
    private final static Logger logger = Logger.getLogger("virtualDB");
    private final static String SQL_FILE_PATH = "SQLite";


    private Connection connect() {

        Connection conn = null;
        try {
            String url = "jdbc:sqlite:" + SQL_FILE_PATH;
            conn = DriverManager.getConnection(url);
            logger.info("Connection to SQLite has been established.");
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            System.exit(0);
        }
        return conn;
    }

    private void closeConn(Connection conn) throws SQLException {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            throw e;
        }
        logger.info("[*] connection closed");
    }

    public JSONObject queryWord(String wordName) throws SQLException {
        List<Word> wordList = new ArrayList<Word>();
        String sql = "select * from words where wordName=\"" + wordName + '"';
        JSONObject resJson = (JSONObject) new JSONObject();
        try {
            Statement stmt = this.conn.createStatement();
            ResultSet res = stmt.executeQuery(sql);
            int idx = 0;
            while (res.next()) {
                // JSONObject actually put address in, so need new word every time
                JSONObject word = (JSONObject) new JSONObject();
                word.put("idx", res.getString("idx"));
                word.put("wordName", res.getString("wordName"));
                word.put("wordType", res.getString("wordType"));
                word.put("meaning", res.getString("meaning"));
                resJson.put(idx, word);
                idx++;
            }
            logger.info("[-] queried word: " + wordName + ": " + resJson.toJSONString());
            return resJson;
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            throw e;
        }
    }


    public JSONObject addWord(String wordName, String wordType, String meaning) throws SQLException {
        String sql = "insert into words (wordName,wordType,meaning) values(?,?,?)";
        JSONObject resJson = (JSONObject) new JSONObject();

        try (
                PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            pstmt.setString(1, wordName);
            pstmt.setString(2, wordType);
            pstmt.setString(3, meaning);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            throw e;
        }
        logger.info("[-] added word: " + wordName + ". ");
        return resJson;
    }

    public JSONObject editWord(int idx, String wordName, String wordType, String meaning) throws SQLException {
        JSONObject resJson = (JSONObject) new JSONObject();

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
            throw e;
        }
        logger.info("[-] edited word: " + wordName + ". ");
        return resJson;
    }

    public JSONObject removeWord(int idx) throws SQLException {
        JSONObject resJson = (JSONObject) new JSONObject();

        String sql = "delete from words where idx = ?";

        try (
                PreparedStatement pstmt = this.conn.prepareStatement(sql)) {
            pstmt.setInt(1, idx);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.warning(e.getMessage());
            throw e;
        }
        logger.info("[-] removed word: " + idx + ". ");
        return resJson;

    }

    public static void main(String[] args) {
        // write your code here
//        Database db = new Database();
//        db.queryWord("apple");
//        db.addWord("banana", "noun", "another kind of fruit");
//        db.editWord(3, "banana", "noun", "very different from apple");
//        db.removeWord(3);
//        db.closeConn(db.conn);
    }
}
