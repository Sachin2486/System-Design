import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

interface RateLimiter {
    boolean allowRequest();
}

class FixedWindowRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowSizeMillis;
    
    private long windowStart;
    private long requestCount;
    
    FixedWindowRateLimiter(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
        this.windowStart = System.currentTimeMillis();
        this.requestCount = 0;
    }
    
    @Override
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        if (now - windowStart >= windowSizeMillis) {
            windowStart = now;
            requestCount = 0;
        }

        requestCount++;
        return requestCount <= maxRequests;
    }
}

class SlidingWindowRateLimiter implements RateLimiter {
    private final int maxRequests;
    private final long windowSizeMillis;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    SlidingWindowRateLimiter(int maxRequests, long windowSizeMillis) {
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }

    @Override
    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        while (!timestamps.isEmpty() &&
               now - timestamps.peekFirst() >= windowSizeMillis) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }
}

class RateLimiterFactory {
    enum Algorithm {
        FIXED_WINDOW,
        SLIDING_WINDOW
    }
    
    static RateLimiter create(
            Algorithm algorithm,
            int maxRequests,
            long windowSizeMillis
    ) {
        switch (algorithm) {
            case SLIDING_WINDOW:
                return new SlidingWindowRateLimiter(maxRequests, windowSizeMillis);
            case FIXED_WINDOW:
            default:
                return new FixedWindowRateLimiter(maxRequests, windowSizeMillis);
        }
    }
}

class RateLimiterManager {

    private final Map<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();
    private final RateLimiterFactory.Algorithm algorithm;
    private final int maxRequests;
    private final long windowSizeMillis;

    RateLimiterManager(
            RateLimiterFactory.Algorithm algorithm,
            int maxRequests,
            long windowSizeMillis
    ) {
        this.algorithm = algorithm;
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeMillis;
    }

    boolean allowRequest(String userId) {
        RateLimiter limiter = userLimiters.computeIfAbsent(
                userId,
                id -> RateLimiterFactory.create(
                        algorithm,
                        maxRequests,
                        windowSizeMillis
                )
        );
        return limiter.allowRequest();
    }
}

public class Main
{
    public static void main(String[] args) {
        
        RateLimiterManager manager =
                new RateLimiterManager(
                        RateLimiterFactory.Algorithm.FIXED_WINDOW,
                        5,
                        10_000
                );

        String user = "user1";

        for (int i = 1; i <= 7; i++) {
            System.out.println(
                    "Request " + i + ": " +
                    (manager.allowRequest(user) ? "ALLOWED" : "BLOCKED")
            );
        }
    }
}
