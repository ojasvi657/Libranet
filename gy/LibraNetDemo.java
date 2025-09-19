import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LibraryException extends Exception {
    public LibraryException(String msg) { super(msg); }
}

class ItemNotAvailableException extends LibraryException {
    public ItemNotAvailableException(String msg) { super(msg); }
}

class InvalidDurationException extends LibraryException {
    public InvalidDurationException(String msg) { super(msg); }
}

final class DurationParser {
    private static final Pattern TOKEN = Pattern.compile("(\\d+)\\s*(d|day|days|h|hr|hrs|hour|hours|m|min|mins|minute|minutes|s|sec|secs|second|seconds)", Pattern.CASE_INSENSITIVE);
    public static Duration parse(String text) throws InvalidDurationException {
        if (text == null || text.trim().isEmpty()) throw new InvalidDurationException("Empty duration");
        Matcher m = TOKEN.matcher(text);
        Duration result = Duration.ZERO;
        boolean found = false;
        while (m.find()) {
            found = true;
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase();
            switch (unit) {
                case "d":
                case "day":
                case "days":
                    result = result.plus(Duration.ofDays(value));
                    break;
                case "h":
                case "hr":
                case "hrs":
                case "hour":
                case "hours":
                    result = result.plus(Duration.ofHours(value));
                    break;
                case "m":
                case "min":
                case "mins":
                case "minute":
                case "minutes":
                    result = result.plus(Duration.ofMinutes(value));
                    break;
                case "s":
                case "sec":
                case "secs":
                case "second":
                case "seconds":
                    result = result.plus(Duration.ofSeconds(value));
                    break;
                default:
                    throw new InvalidDurationException("Unknown duration unit: " + unit);
            }
        }
        if (!found) throw new InvalidDurationException("Could not parse duration: " + text);
        return result;
    }
}

class FineLedger {
    private final Map<Integer, BigDecimal> userFines = new ConcurrentHashMap<>();
    private final BigDecimal finePerDay;
    public FineLedger(BigDecimal finePerDay) { this.finePerDay = finePerDay; }
    public void addFine(int userId, long overdueDays) {
        if (overdueDays <= 0) return;
        BigDecimal add = finePerDay.multiply(BigDecimal.valueOf(overdueDays));
        userFines.merge(userId, add, BigDecimal::add);
    }
    public BigDecimal getFine(int userId) {
        return userFines.getOrDefault(userId, BigDecimal.ZERO);
    }
    public void payFine(int userId, BigDecimal amount) {
        userFines.computeIfPresent(userId, (k, v) -> {
            BigDecimal remaining = v.subtract(amount);
            return remaining.compareTo(BigDecimal.ZERO) <= 0 ? null : remaining;
        });
    }
}

interface Playable {
    void play();
    void pause();
    Duration getPlaybackDuration();
}

abstract class LibraryItem {
    private final int id;
    private final String title;
    private final String author;
    private boolean available = true;
    private Integer borrowedByUserId = null;
    private Instant borrowedAt = null;
    private Instant dueAt = null;

    protected LibraryItem(int id, String title, String author) {
        if (title == null || author == null) throw new IllegalArgumentException("title/author required");
        this.id = id;
        this.title = title;
        this.author = author;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public boolean isAvailable() { return available; }

    public synchronized void borrow(int userId, String durationText) throws LibraryException {
        if (!available) throw new ItemNotAvailableException("Item " + id + " is not available.");
        Duration dur = DurationParser.parse(durationText);
        this.available = false;
        this.borrowedByUserId = userId;
        this.borrowedAt = Instant.now();
        this.dueAt = borrowedAt.plus(dur);
    }

    public synchronized long returnItem() {
        if (available) return 0L;
        Instant now = Instant.now();
        long overdueDays = 0L;
        if (dueAt != null && now.isAfter(dueAt)) {
            Duration overdue = Duration.between(dueAt, now);
            overdueDays = overdue.toDays();
            if (overdue.toMinutesPart() > 0 || overdue.toSecondsPart() > 0) overdueDays += 1;
        }
        this.available = true;
        this.borrowedByUserId = null;
        this.borrowedAt = null;
        this.dueAt = null;
        return overdueDays;
    }

    public Optional<Integer> getBorrowedByUserId() { return Optional.ofNullable(borrowedByUserId); }
    public Optional<Instant> getDueAt() { return Optional.ofNullable(dueAt); }
}

class Book extends LibraryItem {
    private final int pageCount;
    public Book(int id, String title, String author, int pageCount) {
        super(id, title, author);
        if (pageCount < 0) throw new IllegalArgumentException("pageCount must be >= 0");
        this.pageCount = pageCount;
    }
    public int getPageCount() { return pageCount; }
}

class Audiobook extends LibraryItem implements Playable {
    private final Duration playbackDuration;
    public Audiobook(int id, String title, String author, Duration playbackDuration) {
        super(id, title, author);
        if (playbackDuration == null || playbackDuration.isNegative()) throw new IllegalArgumentException("playbackDuration required");
        this.playbackDuration = playbackDuration;
    }
    @Override
    public void play() { System.out.println("Playing audiobook: " + getTitle()); }
    @Override
    public void pause() { System.out.println("Pausing audiobook: " + getTitle()); }
    @Override
    public Duration getPlaybackDuration() { return playbackDuration; }
}

class EMagazine extends LibraryItem {
    private final String issueNumber;
    private boolean archived = false;
    public EMagazine(int id, String title, String author, String issueNumber) {
        super(id, title, author);
        if (issueNumber == null) throw new IllegalArgumentException("issueNumber required");
        this.issueNumber = issueNumber;
    }
    public String getIssueNumber() { return issueNumber; }
    public synchronized void archiveIssue() {
        this.archived = true;
        System.out.println("Archived e-magazine issue: " + issueNumber);
    }
    public boolean isArchived() { return archived; }
}

class Catalog {
    private final Map<Integer, LibraryItem> items = new ConcurrentHashMap<>();
    public void addItem(LibraryItem item) {
        Objects.requireNonNull(item);
        items.put(item.getId(), item);
    }
    public Optional<LibraryItem> findById(int id) { return Optional.ofNullable(items.get(id)); }
    public List<LibraryItem> searchByTitle(String titleQuery) {
        List<LibraryItem> result = new ArrayList<>();
        String q = titleQuery == null ? "" : titleQuery.toLowerCase();
        for (LibraryItem it : items.values()) {
            if (it.getTitle().toLowerCase().contains(q)) result.add(it);
        }
        return result;
    }
    public <T extends LibraryItem> List<T> searchByType(Class<T> clazz) {
        List<T> out = new ArrayList<>();
        for (LibraryItem it : items.values()) {
            if (clazz.isInstance(it)) out.add(clazz.cast(it));
        }
        return out;
    }
}

public class LibraNetDemo {
    public static void main(String[] args) {
        FineLedger ledger = new FineLedger(BigDecimal.valueOf(10));
        Catalog catalog = new Catalog();
        Book b1 = new Book(1, "Effective Java", "Joshua Bloch", 416);
        Audiobook a1 = new Audiobook(2, "Clean Code (Audio)", "Robert C. Martin", Duration.ofHours(12).plusMinutes(30));
        EMagazine m1 = new EMagazine(3, "Monthly Tech", "Various", "2025-09");
        catalog.addItem(b1);
        catalog.addItem(a1);
        catalog.addItem(m1);
        try {
            b1.borrow(1001, "14 days");
            System.out.println("Borrowed book: " + b1.getTitle() + " due on " + b1.getDueAt().orElse(null));
            try {
                b1.borrow(1002, "7 days");
            } catch (ItemNotAvailableException e) {
                System.out.println("Expected: " + e.getMessage());
            }
            long overdueDays = 3;
            ledger.addFine(1001, overdueDays);
            System.out.println("User 1001 fines: " + ledger.getFine(1001));
            a1.borrow(1002, "3 days");
            ((Playable) a1).play();
            System.out.println("Audio duration: " + ((Playable) a1).getPlaybackDuration());
            m1.archiveIssue();
            Duration d1 = DurationParser.parse("1 day 5 hours 30 minutes");
            System.out.println("Parsed duration: " + d1);
            try {
                DurationParser.parse("two weeks");
            } catch (InvalidDurationException ex) {
                System.out.println("Invalid duration: " + ex.getMessage());
            }
        } catch (LibraryException | IllegalArgumentException e) {
            System.err.println("Library operation failed: " + e.getMessage());
        }
    }
}