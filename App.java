import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class Todo {
    private int id;
    private String title;
    private boolean done;

    Todo(int id, String title, boolean done) {
        this.id = id;
        this.title = title;
        this.done = done;
    }

    int getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    boolean isDone() {
        return done;
    }

    void setDone(boolean done) {
        this.done = done;
    }
}

public class App {
    private static List<Todo> todos = new ArrayList<>();
    private static int nextId = 1;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        todos.add(new Todo(nextId++, "牛乳を買う", false));
        todos.add(new Todo(nextId++, "本を読む", true));

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String message;

            if (path.equals("/") && exchange.getRequestMethod().equals("GET")) {
                String html = "<form method='post' action='/add'>"
                        + "<input name='todo'>"
                        + "<button type='submit'>追加</button></form>";
                html += "<ul>";
                for (Todo todo : todos) {
                    String mark = ""; // ★追加
                    if (todo.isDone()) {
                        mark = " ✔";
                    } // ★追加
                    html += "<li>" + todo.getTitle() + mark // ★追加
                            + " <a href='/done?id=" + todo.getId() + "'>完了</a>" // ★追加
                            + " <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                }
                html += "</ul>";
                message = html;
            } else if (path.equals("/add") && exchange.getRequestMethod().equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String todo = "";
                if (body.startsWith("todo=")) {
                    todo = URLDecoder.decode(body.substring(5), StandardCharsets.UTF_8);
                }
                if (!todo.isEmpty()) {
                    todos.add(new Todo(nextId++, todo, false));
                }
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1);
                exchange.close();
                return;
            } else if (path.equals("/done") && exchange.getRequestMethod().equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                Integer id = parseId(query); // ★追加
                if (id != null) { // ★追加
                    for (Todo todo : todos) { // ★追加
                        if (todo.getId() == id) { // ★追加
                            todo.setDone(true); // ★追加
                        } // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/delete") && exchange.getRequestMethod().equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                Integer id = parseId(query); // ★追加
                if (id != null) { // ★追加
                    todos.removeIf(todo -> todo.getId() == id); // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else {
                message = "ページが見つかりません";
            }

            byte[] response = message.getBytes(StandardCharsets.UTF_8);
            String contentType = path.equals("/")
                    ? "text/html; charset=UTF-8"
                    : "text/plain; charset=UTF-8";
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.getResponseBody().close();
        });

        server.start();
        System.out.println("サーバーを起動しました http://localhost:8080");
    }

    private static Integer parseId(String query) { // ★追加
        if (query == null || !query.startsWith("id=")) { // ★追加
            return null; // ★追加
        } // ★追加
        try { // ★追加
            return Integer.parseInt(query.substring(3)); // ★追加
        } catch (NumberFormatException e) { // ★追加
            return null; // ★追加
        } // ★追加
    } // ★追加
}
