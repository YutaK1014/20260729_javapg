import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

class Todo {
    private final int id;
    private final String title;
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
    private static final List<Todo> todos = new ArrayList<>();
    private static int nextId = 1;

    public static void main(String[] args) throws IOException {
        todos.add(new Todo(nextId++, "買い物をする", false));
        todos.add(new Todo(nextId++, "本を読む", true));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", App::handleRequest);
        server.start();
        System.out.println("サーバーを起動しました: http://localhost:8080");
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        if (path.equals("/") && method.equals("GET")) {
            sendHtml(exchange, renderTodoList());
            return;
        }
        if (path.equals("/add") && method.equals("POST")) {
            String title = queryValue(readBody(exchange), "todo");
            if (!title.isBlank())
                todos.add(new Todo(nextId++, title, false));
            redirect(exchange);
            return;
        }
        if (path.equals("/toggle") && method.equals("POST")) {
            String body = readBody(exchange);
            int id = parseInt(queryValue(body, "id"));
            boolean done = queryValue(body, "done").equals("true");
            for (Todo todo : todos) {
                if (todo.getId() == id) {
                    todo.setDone(done);
                    break;
                }
            }
            redirect(exchange);
            return;
        }
        if (path.equals("/delete") && method.equals("GET")) {
            int id = parseInt(queryValue(exchange.getRequestURI().getQuery(), "id"));
            todos.removeIf(todo -> todo.getId() == id);
            redirect(exchange);
            return;
        }

        byte[] response = "Not Found".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String renderTodoList() {
        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang='ja'><meta charset='UTF-8'>");
        html.append("<title>ToDoリスト</title>");
        html.append("<style>li{margin:8px 0}.done{text-decoration:line-through;color:gray}</style>");
        html.append("<h1>ToDoリスト</h1>");
        html.append("<form method='post' action='/add'>");
        html.append("<input name='todo' placeholder='やることを入力' required>");
        html.append("<button type='submit'>追加</button></form><ul>");

        for (Todo todo : todos) {
            html.append("<li class='").append(todo.isDone() ? "done" : "").append("'>");
            html.append("<form method='post' action='/toggle' style='display:inline'>");
            html.append("<input type='hidden' name='id' value='").append(todo.getId()).append("'>");
            html.append("<input type='hidden' name='done' value='false'>");
            html.append("<input type='checkbox' name='done' value='true'");
            if (todo.isDone())
                html.append(" checked");
            html.append(" onchange='this.form.submit()'> ");
            html.append(escapeHtml(todo.getTitle()));
            html.append("</form> <a href='/delete?id=").append(todo.getId()).append("'>削除</a></li>");
        }
        html.append("</ul></html>");
        return html.toString();
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String queryValue(String query, String key) {
        if (query == null)
            return "";
        String value = "";
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return value;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void redirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
