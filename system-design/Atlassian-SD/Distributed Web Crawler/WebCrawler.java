import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebCrawler {
    // Frontier: Queue of URLs to crawl
    private Queue<URLDepthPair> urlQueue = new ConcurrentLinkedQueue<>();
    // Set of visited URLs to avoid duplicates
    private Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    // Maximum depth to crawl
    private final int MAX_DEPTH = 3;

    // Thread pool for concurrent crawling
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    // URL Depth Pair: Stores the URL and its depth
    static class URLDepthPair {
        String url;
        int depth;

        public URLDepthPair(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }

    // Function to start the crawl
    public void startCrawl(String startUrl) {
        // Add the initial URL to the queue with depth 0
        urlQueue.add(new URLDepthPair(startUrl, 0));

        // Process the URLs in the queue
        while (!urlQueue.isEmpty() || !executor.isTerminated()) {
            URLDepthPair pair = urlQueue.poll();
            if (pair != null && !visitedUrls.contains(pair.url) && pair.depth <= MAX_DEPTH) {
                visitedUrls.add(pair.url);
                executor.execute(() -> crawlPage(pair));
            }
            
            // If queue is empty, wait a bit for threads to add more URLs
            if (urlQueue.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Shutdown the executor once crawling is complete
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Function to crawl a single page
    public void crawlPage(URLDepthPair pair) {
        try {
            String content = fetchPageContent(pair.url);

            if (content != null) {
                System.out.println("Crawled: " + pair.url + " at depth " + pair.depth);
                List<String> links = extractLinks(content);

                for (String link : links) {
                    // Add the found links to the queue with incremented depth
                    if (!visitedUrls.contains(link)) {
                        urlQueue.add(new URLDepthPair(link, pair.depth + 1));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to crawl: " + pair.url + " due to " + e.getMessage());
        }
    }

    // Fetch page content using HttpURLConnection
    public String fetchPageContent(String urlString) {
        StringBuilder content = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Error fetching page: " + urlString + " - " + e.getMessage());
            return null;
        }
        return content.toString();
    }

    // Extract links from the HTML content using regex
    public List<String> extractLinks(String content) {
        List<String> links = new ArrayList<>();
        String regex = "href=\\\"(http[s]?://[^\\\"]+)\\\""; // Basic regex for extracting links
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            links.add(matcher.group(1));
        }
        return links;
    }

    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler();
        String startUrl = "https://example.com"; // Starting URL
        crawler.startCrawl(startUrl);
    }
}
