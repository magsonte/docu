package com.codingkiwi.pdfSearch;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

/**
 * Hello world!
 */

public class App {

    private static boolean exit = false;

    private static void createRepository(Connection conn) {
        StringBuilder sb = new StringBuilder();

        try (Statement stmt = conn.createStatement()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("Connected with driver " + meta.getDriverName());

            sb.append("CREATE VIRTUAL TABLE __FTS__ USING FTS5(md5,file_path,document_body);");
            String table_query = sb.toString();

            stmt.execute(table_query);

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private static void searchRepository(String search, Connection conn) {
        String sql = "SELECT file_path FROM __FTS__ WHERE document_body MATCH ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, search);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.println(search + " found@ " + rs.getString("file_path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    private static void addDocument(File f, @org.jetbrains.annotations.NotNull Connection conn) {

        try {
            var stmt = conn.prepareStatement("SELECT file_path as count FROM __FTS__ WHERE file_path = ? LIMIT 1;");
            stmt.setString(1, f.getAbsolutePath());
            stmt.execute();
            try (ResultSet rs = stmt.getResultSet()) {

                while (rs.next()) {
                    System.out.println("File already in repository " + f.getAbsolutePath());
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String sql = "INSERT INTO __FTS__(file_path,document_body) VALUES (?,?);";
        try (PDDocument doc = PDDocument.load(f);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            PDFTextStripper txtStrip = new PDFTextStripper();
            String text = txtStrip.getText(doc);

            pstmt.setString(1, f.getAbsolutePath());
            pstmt.setString(2, text);
            pstmt.executeUpdate();

            System.out.println("Added " + f.getName());


        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void ingestPdfs(Connection conn){
            System.out.println("Ingesting all PDFs");
            var dir = new File(".");

            File[] files = dir.listFiles((dir1, name) -> name.endsWith(".pdf"));

            for (File file : files) {
                addDocument(file, conn);
            }
    }

    private static void commandParser(Query command,Connection conn) {

        switch (command.getCommand()) {
            case QUIT:
                exit = true;
                System.out.println("Goodbye.");
                break;
            case INGEST:
                ingestPdfs(conn);
                break;
            case LIST:
                listRepository(conn);
                break;
            case CLEAR:
                try {
                    Runtime.getRuntime().exec("clear");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("No Command");


        }
    }

    private static void listRepository(Connection conn) {
        String sql = "SELECT ROWID, file_path FROM __FTS__";
        try(Statement stmt = conn.createStatement()){
            ResultSet rs = stmt.executeQuery(sql);

            while(rs.next()){
                System.out.println(rs.getString("ROWID") + "- " + rs.getString("file_path"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        var connectionString = "jdbc:sqlite:__invoice_repository.db";
        try (Connection conn = DriverManager.getConnection(connectionString)) {
            createRepository(conn);

            Scanner sc = new Scanner(System.in);

            while (!exit) {
                System.out.print("[QUERY|COMMAND] (@!help for help) > ");
                String lineRead = sc.nextLine();
                Query query = new Query(lineRead);


                if (query.isCommand()) {
                    commandParser(query,conn);
                }


            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
