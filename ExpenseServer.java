import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 (then open http://localhost:8080)
 */
public class ExpenseServer {

    // ---------- Model ----------
    static class Expense {
        final String date, category, type, note;
        final double amount;
        Expense(String date, String category, String type, double amount, String note) {
            this.date = date; this.category = category; this.type = type;
            this.amount = amount; this.note = note;
        }
        String toLine() {
            return date + "|" + category + "|" + type + "|" + amount + "|"
                    + note.replace("\\", "\\\\").replace("|", "\\p");
        }
        static Expense fromLine(String line) {
            String[] p = line.split("\\|", 5);
            if (p.length < 5) throw new IllegalArgumentException("Bad line: " + line);
            return new Expense(p[0], p[1], p[2], Double.parseDouble(p[3]),
                    p[4].replace("\\p", "|").replace("\\\\", "\\"));
        }
    }

    // ---------- State ----------
    static final List<Expense> expenses = new ArrayList<>();
    static double budget = 0;
    static final File DATA = new File(System.getProperty("user.dir"), "expenses.txt");
    static final File PAGE = new File(System.getProperty("user.dir"), "index.html");

    // ---------- File handling (I/O streams) ----------
    static void saveFile() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("BUDGET|" + budget);
        for (Expense x : expenses) lines.add(x.toLine());
        Files.write(DATA.toPath(), lines, StandardCharsets.UTF_8);
    }

    /** Parses file text. Throws IllegalArgumentException on bad content. */
    static List<Expense> parse(String text, double[] budgetOut) {
        List<Expense> out = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            if (line.startsWith("BUDGET|")) budgetOut[0] = Double.parseDouble(line.substring(7));
            else out.add(Expense.fromLine(line));
        }
        return out;
    }

    static void loadFile() {
        if (!DATA.exists()) return;
        try {
            double[] b = {0};
            List<Expense> loaded = parse(new String(Files.readAllBytes(DATA.toPath()), StandardCharsets.UTF_8), b);
            expenses.clear(); expenses.addAll(loaded); budget = b[0];
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Could not read " + DATA + ": " + e.getMessage());
        }
    }

    // ---------- HTTP helpers ----------
    static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    static void sendText(HttpExchange ex, int code, String text) throws IOException {
        send(ex, code, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
    }

    static String body(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    static Map<String, String> form(String s) {
        Map<String, String> m = new HashMap<>();
        for (String pair : s.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                  kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }

    static String esc(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"') b.append("\\\"");
            else if (c == '\\') b.append("\\\\");
            else if (c < 32) b.append(' ');
            else b.append(c);
        }
        return b.toString();
    }

    static String dataJson() {
        StringBuilder b = new StringBuilder("{\"budget\":" + budget + ",\"file\":\""
                + esc(DATA.getAbsolutePath()) + "\",\"expenses\":[");
        for (int i = 0; i < expenses.size(); i++) {
            Expense x = expenses.get(i);
            if (i > 0) b.append(',');
            b.append("{\"id\":").append(i)
             .append(",\"date\":\"").append(esc(x.date))
             .append("\",\"category\":\"").append(esc(x.category))
             .append("\",\"type\":\"").append(esc(x.type))
             .append("\",\"amount\":").append(x.amount)
             .append(",\"note\":\"").append(esc(x.note)).append("\"}");
        }
        return b.append("]}").toString();
    }

    // ---------- Request routing ----------
    static synchronized void handle(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath(), method = ex.getRequestMethod();

            if (method.equals("GET") && path.equals("/")) {
                if (!PAGE.exists()) { sendText(ex, 404, "index.html not found next to the program."); return; }
                send(ex, 200, "text/html; charset=utf-8", Files.readAllBytes(PAGE.toPath()));

            } else if (method.equals("GET") && path.equals("/api/data")) {
                send(ex, 200, "application/json; charset=utf-8", dataJson().getBytes(StandardCharsets.UTF_8));

            } else if (method.equals("GET") && path.equals("/api/export")) {
                byte[] bytes = DATA.exists() ? Files.readAllBytes(DATA.toPath()) : new byte[0];
                ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"expenses.txt\"");
                send(ex, 200, "text/plain; charset=utf-8", bytes);

            } else if (method.equals("POST") && path.equals("/api/add")) {
                Map<String, String> f = form(body(ex));
                double amount = Double.parseDouble(f.getOrDefault("amount", "").trim());
                if (!(amount > 0)) throw new IllegalArgumentException("Amount must be greater than 0.");
                String date = LocalDate.parse(f.getOrDefault("date", "").trim()).toString();
                String type = f.getOrDefault("type", "");
                if (!type.equals("necessary") && !type.equals("extra"))
                    throw new IllegalArgumentException("Type must be necessary or extra.");
                String cat = f.getOrDefault("category", "Other").trim();
                if (cat.isEmpty() || cat.length() > 30) throw new IllegalArgumentException("Invalid category.");
                String note = f.getOrDefault("note", "").replaceAll("[\\r\\n]+", " ").trim();
                if (note.length() > 200) note = note.substring(0, 200);
                expenses.add(new Expense(date, cat, type, amount, note));
                saveFile();
                sendText(ex, 200, "ok");

            } else if (method.equals("POST") && path.equals("/api/delete")) {
                int id = Integer.parseInt(form(body(ex)).getOrDefault("id", "-1"));
                if (id < 0 || id >= expenses.size()) throw new IllegalArgumentException("No such expense.");
                expenses.remove(id);
                saveFile();
                sendText(ex, 200, "ok");

            } else if (method.equals("POST") && path.equals("/api/budget")) {
                double b = Double.parseDouble(form(body(ex)).getOrDefault("value", "").trim());
                if (b < 0) throw new IllegalArgumentException("Budget cannot be negative.");
                budget = b;
                saveFile();
                sendText(ex, 200, "ok");

            } else if (method.equals("POST") && path.equals("/api/import")) {
                double[] b = {0};
                List<Expense> loaded = parse(body(ex), b);
                expenses.clear(); expenses.addAll(loaded); budget = b[0];
                saveFile();
                sendText(ex, 200, "Imported " + loaded.size() + " expenses.");

            } else {
                sendText(ex, 404, "Not found");
            }
        } catch (IllegalArgumentException | DateTimeException e) {   // bad user input
            sendText(ex, 400, "Invalid input: " + e.getMessage());
        } catch (Exception e) {                                      // I/O or unexpected
            sendText(ex, 500, "Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        loadFile();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", ExpenseServer::handle);
        server.start();
        String url = "http://localhost:" + port;
        System.out.println("Expense Tracker running at " + url);
        System.out.println("Data file: " + DATA.getAbsolutePath());
        System.out.println("Press Ctrl+C to stop.");
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) { }
    }
}
