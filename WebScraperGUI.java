import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.util.List;

public class WebScraperGUI {

    private static int totalChunkCount = 0;
    private static JTextArea textArea;
    private static JTextField urlField;
    private static JTextField fileNameField;
    private static JButton runButton;

    public static void main(String[] args) {
        // Run the Swing GUI creation on the Event Dispatch Thread
        SwingUtilities.invokeLater(WebScraperGUI::createAndShowGUI);
    }

    // Method to create and show the GUI
    private static void createAndShowGUI() {
        // Create the frame
        JFrame frame = new JFrame("Web Scraper");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Set layout for the frame
        frame.setLayout(new BorderLayout());

        // Panel for input fields and button
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(3, 2)); // Grid with 3 rows and 2 columns

        // Add the Start URL input field
        JLabel urlLabel = new JLabel("Enter Start URL:");
        urlField = new JTextField();
        panel.add(urlLabel);
        panel.add(urlField);

        // Add the File Name input field
        JLabel fileNameLabel = new JLabel("Enter Output File Name:");
        fileNameField = new JTextField();
        panel.add(fileNameLabel);
        panel.add(fileNameField);

        // Add the Run button
        runButton = new JButton("Run");
        panel.add(new JLabel());  // Empty label for spacing
        panel.add(runButton);

        // Add the input panel to the frame
        frame.add(panel, BorderLayout.NORTH);

        // Text area to display output
        textArea = new JTextArea();
        textArea.setEditable(false); // Disable editing in the text area
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Add the text area to the frame inside a scroll pane
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Action listener for the "Run" button
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Disable the button to prevent multiple clicks
                runButton.setEnabled(false);
                
                // Get the user inputs
                String startUrl = urlField.getText();
                String fileName = fileNameField.getText();

                // Start the scraping process
                textArea.append("Starting scraping...\n");
                new Thread(() -> processChapters(startUrl, fileName)).start();
            }
        });

        // Make the frame visible
        frame.setVisible(true);
    }

    // Method to process chapters
    private static void processChapters(String startUrl, String fileName) {
        String currentUrl = startUrl;

        while (true) {
            try {
                String text = fetchTextFromUrl(currentUrl);

                // Tokenize and chunk text
                List<String> chunks = splitIntoChunks(text, 2048);

                // Save chunks to a file
                saveChunksToFile(chunks, fileName);

                // Display chunks in the GUI
                for (String chunk : chunks) {
                    totalChunkCount++;
                    textArea.append("Chunk " + totalChunkCount + ":\n" + chunk + "\n\n");
                }

                // Get the next chapter URL
                currentUrl = findNextChapterUrl(currentUrl);
                if (currentUrl == null){
                    textArea.append("No next chapter found. Exiting.\n");
                    break;
                }
            } catch (IOException e) {
                textArea.append("Error processing URL: " + e.getMessage() + "\n");
                break;
            }
        }
        
        // Re-enable the "Run" button once done
        SwingUtilities.invokeLater(() -> runButton.setEnabled(true));
    }

    // Fetch text from a URL
    private static String fetchTextFromUrl(String url) throws IOException {
        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "gzip, deflate, br, ztsd")
            .header("Connection", "keep-alive")
            .header("Origin", "https://www.deqixs.com")
            .header("X-Screen-Size", "375x667")
            .header("X-Requested-With", "XMLHttpRequest")
            .get();
        Element content = doc.selectFirst("div.con");
        return content != null ? content.text() : "";
    }

    // Split the text into chunks of max tokens (simulated here as word chunks)
    private static List<String> splitIntoChunks(String text, int maxTokens) {
        List<String> chunks = new java.util.ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentChunk = new StringBuilder();

        for (String word : words) {
            if ((currentChunk + " " + word).split("\\s+").length > maxTokens) {
                chunks.add(currentChunk.toString().trim());
                currentChunk.setLength(0); // Reset the chunk
            }
            currentChunk.append(" ").append(word);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }

    private static void saveChunksToFile(List<String> chunks, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName, true), StandardCharsets.UTF_8)))  {
            for (String chunk : chunks) {
                writer.write("Chunk " + (totalChunkCount + 1) + ":\n" + chunk + "\n---\n\n");
            }
        } catch (IOException e) {
            textArea.append("Error writing to file: " + e.getMessage() + "\n");
        }
    }

    // Find the next chapter URL
    private static String findNextChapterUrl(String currentUrl) throws IOException {
        Document doc = Jsoup.connect(currentUrl)
            .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept-Encoding", "gzip, deflate, br")
            .header("Connection", "keep-alive")
            .get();
    
        // Select the prenext div
        Element prenextDiv = doc.selectFirst("div.prenext");
    
        if (prenextDiv != null) {
            // Debugging: Print the prenext div content
            //textArea.append("Prenext div:\n" + prenextDiv.html() + "\n");
    
            for (Element link : prenextDiv.select("a")) {
                // Debugging: Print each link's text and href
                textArea.append("Found link: " + link.text() + " -> " + link.attr("href") + "\n");
    
                // Check if the link text matches "下一页" or "下一章"
                if ((link.text()).equals("下一页") || link.text().equals("下一章")) {
                    // Construct the absolute URL
                    String nextChapterUrl = new URL(new URL(currentUrl), link.attr("href")).toString();
                    textArea.append("\nNext Chapter URL: " + nextChapterUrl + "\n");
                    return nextChapterUrl;
                }
            }
        }
        // If no next chapter link is found
        return null;
    }    
}
