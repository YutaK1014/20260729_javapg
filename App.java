import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
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
    private static final Path DATA_FILE = Path.of("todos.dat");
    private static int nextId = 1;

    public static void main(String[] args) throws IOException {
        loadTodos();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                saveTodos();
            } catch (IOException e) {
                System.err.println("Could not save todos: " + e.getMessage());
            }
        }));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", App::handleRequest);
        server.start();
        System.out.println("Server started: http://localhost:8080");
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
            if (!title.isBlank()) {
                todos.add(new Todo(nextId++, title, false));
                saveTodos();
            }
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
                    saveTodos();
                    break;
                }
            }
            redirect(exchange);
            return;
        }
        if (path.equals("/delete") && method.equals("GET")) {
            int id = parseInt(queryValue(exchange.getRequestURI().getQuery(), "id"));
            todos.removeIf(todo -> todo.getId() == id);
            saveTodos();
            redirect(exchange);
            return;
        }

        byte[] response = "Not Found".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private static String renderTodoList() {
        long completed = todos.stream().filter(Todo::isDone).count();
        StringBuilder html = new StringBuilder();
        html.append(
                "<!doctype html><html lang='ja'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1'>");
        html.append("<title>ToDo\u30ea\u30b9\u30c8</title><style>");
        html.append(
                "*{box-sizing:border-box}body{margin:0;min-height:100vh;background:linear-gradient(135deg,#eef2ff,#f8fafc);font-family:Arial,'Noto Sans JP',sans-serif;color:#1e293b;padding:40px 16px;position:relative;overflow-x:hidden}body:before,body:after{content:'';position:fixed;width:280px;height:280px;border-radius:50%;filter:blur(6px);opacity:.45;z-index:-1}body:before{background:#c7d2fe;top:-100px;left:-90px}body:after{background:#bfdbfe;right:-120px;bottom:-100px}");
        html.append(
                ".container{max-width:640px;margin:auto;background:rgba(255,255,255,.94);border:1px solid rgba(255,255,255,.8);border-radius:20px;padding:32px;box-shadow:0 18px 45px rgba(30,41,59,.12);backdrop-filter:blur(10px)}.container:before{content:'';display:block;width:54px;height:5px;border-radius:99px;background:linear-gradient(90deg,#4f46e5,#818cf8);margin-bottom:22px}");
        html.append(
                ".header{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:24px}h1{margin:0;font-size:30px;letter-spacing:-.04em;color:#312e81}.progress{color:#4338ca;background:#eef2ff;border:1px solid #e0e7ff;border-radius:99px;padding:7px 12px;font-size:13px;font-weight:bold;text-align:right;white-space:nowrap}");
        html.append(
                ".add-form{display:flex;gap:10px;margin-bottom:26px}.add-form input{flex:1;border:1px solid #cbd5e1;border-radius:10px;padding:12px 14px;font-size:16px;outline:none}.add-form input:focus{border-color:#6366f1;box-shadow:0 0 0 3px #e0e7ff}");
        html.append(
                "button{border:0;border-radius:10px;background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff;padding:0 20px;font-size:15px;font-weight:bold;cursor:pointer;box-shadow:0 7px 14px rgba(79,70,229,.22);transition:transform .15s,box-shadow .15s}button:hover{background:linear-gradient(135deg,#4338ca,#6d28d9);transform:translateY(-1px);box-shadow:0 9px 18px rgba(79,70,229,.3)}button:active{transform:translateY(0)}");
        html.append(
                ".todo-list{padding:0;margin:0;list-style:none}.todo-item{display:flex;align-items:center;gap:12px;padding:15px 16px;margin:10px 0;border:1px solid #e2e8f0;border-radius:12px;background:#f8fafc;transition:transform .15s,box-shadow .15s,border-color .15s}.todo-item:hover{transform:translateY(-2px);border-color:#c7d2fe;box-shadow:0 8px 18px rgba(99,102,241,.1)}.todo-item form{display:flex;align-items:center;gap:12px;flex:1}.todo-item input[type=checkbox]{width:19px;height:19px;accent-color:#4f46e5;cursor:pointer}.todo-item a{color:#ef4444;text-decoration:none;font-size:13px;padding:4px 7px;border-radius:6px;transition:background .15s}.todo-item a:hover{text-decoration:none;background:#fee2e2}.done{text-decoration:line-through;color:#94a3b8}.empty-message{text-align:center;color:#64748b;padding:30px 0}");
        html.append(
                ".todo-item form{min-width:0}.todo-item form input[type=checkbox]{flex:0 0 auto}.todo-item form{overflow-wrap:anywhere}.todo-item a{flex:0 0 auto}.empty-message{text-align:center;color:#64748b;padding:30px 0}");
        html.append(
                "@media (max-width:600px){body{padding:16px 10px}.container{padding:22px 16px;border-radius:16px}.header{align-items:flex-start;gap:8px;margin-bottom:20px}h1{font-size:25px}.progress{font-size:12px;padding-top:5px}.add-form{gap:8px;margin-bottom:20px}.add-form input{min-width:0;padding:11px 12px}.add-form button{padding:0 14px}.todo-item{align-items:flex-start;gap:9px;padding:12px}.todo-item form{align-items:flex-start;gap:9px;line-height:1.5}.todo-item input[type=checkbox]{margin-top:3px}.todo-item a{font-size:12px;padding-top:3px}}");
        html.append("</style></head><body><main class='container'>");
        html.append("<div class='header'><h1>ToDo\u30ea\u30b9\u30c8</h1><div class='progress'>\u5168")
                .append(todos.size()).append("\u4ef6\u4e2d ").append(completed)
                .append("\u4ef6\u5b8c\u4e86</div></div>");
        html.append(
                "<form class='add-form' method='post' action='/add'><input name='todo' placeholder='\u3084\u308b\u3053\u3068\u3092\u5165\u529b' required><button type='submit'>\u8ffd\u52a0</button></form><ul class='todo-list'>");

        if (todos.isEmpty()) {
            html.append(
                    "<p class='empty-message'>\u4eca\u3084\u308b\u3053\u3068\u306f\u3042\u308a\u307e\u305b\u3093</p>");
        }
        for (Todo todo : todos) {
            html.append("<li class='todo-item ").append(todo.isDone() ? "done" : "").append("'>");
            html.append("<form method='post' action='/toggle'><input type='hidden' name='id' value='")
                    .append(todo.getId()).append("'>");
            html.append(
                    "<input type='hidden' name='done' value='false'><input type='checkbox' name='done' value='true'");
            if (todo.isDone())
                html.append(" checked");
            html.append(" onchange='this.form.requestSubmit()'> ").append(escapeHtml(todo.getTitle()));
            html.append("</form><a href='/delete?id=").append(todo.getId()).append("'>\u524a\u9664</a></li>");
        }
        html.append("</ul></main><script>");
        html.append("const list=document.querySelector('.todo-list');");
        html.append("const progress=document.querySelector('.progress');");
        html.append(
                "function updateProgress(){const items=document.querySelectorAll('.todo-item');const completed=[...items].filter(item=>item.querySelector('input[type=checkbox]').checked).length;progress.textContent='全'+items.length+'件中 '+completed+'件完了';if(!items.length&&!list.querySelector('.empty-message')){list.innerHTML='<p class=\\'empty-message\\'>今やることはありません</p>';}} ");
        html.append(
                "document.querySelectorAll('.todo-item form').forEach(form=>form.addEventListener('submit',async event=>{event.preventDefault();const item=form.closest('.todo-item');const checkbox=form.querySelector('input[type=checkbox]');const done=checkbox.checked;const body=new URLSearchParams(new FormData(form));try{const response=await fetch(form.action,{method:'POST',body});if(!response.ok)throw new Error();item.classList.toggle('done',done);updateProgress();}catch(error){checkbox.checked=!done;alert('更新に失敗しました。');}}));");
        html.append(
                "document.querySelectorAll('.todo-item a').forEach(link=>link.addEventListener('click',async event=>{event.preventDefault();const item=link.closest('.todo-item');try{const response=await fetch(link.href);if(!response.ok)throw new Error();item.remove();updateProgress();}catch(error){alert('削除に失敗しました。');}}));");
        html.append("</script></body></html>");
        return html.toString();
    }

    private static void loadTodos() throws IOException {
        if (!Files.exists(DATA_FILE))
            return;
        for (String line : Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\\t", 3);
            if (parts.length != 3)
                continue;
            try {
                int id = Integer.parseInt(parts[0]);
                boolean done = Boolean.parseBoolean(parts[1]);
                String title = new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8);
                todos.add(new Todo(id, title, done));
                nextId = Math.max(nextId, id + 1);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed records and continue loading the remaining tasks.
            }
        }
    }

    private static void saveTodos() throws IOException {
        List<String> lines = new ArrayList<>();
        for (Todo todo : todos) {
            String title = Base64.getEncoder().encodeToString(todo.getTitle().getBytes(StandardCharsets.UTF_8));
            lines.add(todo.getId() + "\t" + todo.isDone() + "\t" + title);
        }
        Files.write(DATA_FILE, lines, StandardCharsets.UTF_8);
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
            if (parts.length == 2 && parts[0].equals(key))
                value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
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
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
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
