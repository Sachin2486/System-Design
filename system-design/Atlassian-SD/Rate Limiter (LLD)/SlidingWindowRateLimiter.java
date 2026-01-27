import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Date;

public class SlidingWindowRateLimiter {

    // A class to represent the Rate Limiter that tracks requests per user using a sliding window approach

    // Number of requests allowed (X) in a given time window (Y seconds)
    private final int maxRequests;
    private final long windowSizeInMillis;

    // A map to track user request timestamps. Each user has a queue of timestamps.
    private final Map<String, Queue<Long>> userRequestMap;

    // Constructor to initialize the rate limiter with maxRequests and windowSize
    public SlidingWindowRateLimiter(int maxRequests, int windowSizeInSeconds) {
        this.maxRequests = maxRequests;
        this.windowSizeInMillis = windowSizeInSeconds * 1000L;  // Convert seconds to milliseconds
        this.userRequestMap = new HashMap<>();
    }

    // Method to check if a user is allowed to make a request at the given currentTime
    public boolean allowRequest(String userId, Date currentTime) {
        long currentTimeMillis = currentTime.getTime();  // Convert current time to milliseconds

        // If this user has never made a request before, create a queue for them
        userRequestMap.putIfAbsent(userId, new LinkedList<>());

        // Get the user's queue of request timestamps
        Queue<Long> requestQueue = userRequestMap.get(userId);

        // Remove outdated requests that are outside the sliding window
        while (!requestQueue.isEmpty() && requestQueue.peek() <= currentTimeMillis - windowSizeInMillis) {
            requestQueue.poll();  // Remove the oldest request if it's outside the time window
        }

        // Check if the number of requests in the current window exceeds the max allowed
        if (requestQueue.size() < maxRequests) {
            // If the user is within the rate limit, allow the request and track the current request time
            requestQueue.offer(currentTimeMillis);
            return true;
        } else {
            // If the user has exceeded the rate limit, deny the request
            return false;
        }
    }

    // Helper method to simulate a user making a request
    public void handleRequest(String userId) {
        Date currentTime = new Date();  // Get the current time
        if (allowRequest(userId, currentTime)) {
            System.out.println("Request allowed for user: " + userId + " at " + currentTime);
        } else {
            System.out.println("Request denied for user: " + userId + " at " + currentTime);
        }
    }

    // Main method to test the SlidingWindowRateLimiter
    public static void main(String[] args) throws InterruptedException {
        // Create a new SlidingWindowRateLimiter allowing 3 requests every 10 seconds
        SlidingWindowRateLimiter rateLimiter = new SlidingWindowRateLimiter(3, 10);

        // Simulate requests from a user "user123"
        String userId = "user123";

        // First 3 requests should be allowed
        rateLimiter.handleRequest(userId);  // Request 1
        rateLimiter.handleRequest(userId);  // Request 2
        rateLimiter.handleRequest(userId);  // Request 3

        // The 4th request should be denied (exceeds 3 requests in 10 seconds)
        rateLimiter.handleRequest(userId);  // Request 4 (should be denied)

        // Wait for 10 seconds (simulate passage of time)
        Thread.sleep(10000);

        // Now the rate limit window has reset, the next request should be allowed again
        rateLimiter.handleRequest(userId);  // Request 5 (should be allowed)
    }
}
