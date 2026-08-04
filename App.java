
// Webサーバーを作るための HttpServer を読み込みます。
import com.sun.net.httpserver.HttpServer;

// 文字をバイトデータに変換するときに使う UTF-8 を読み込みます。
import java.nio.charset.StandardCharsets;

// ポート番号を指定するための InetSocketAddress を読み込みます。
import java.net.InetSocketAddress;

// プログラム本体となる App クラスを作ります。
public class App {

    // Javaプログラムを実行したとき、最初に動く main メソッドです。
    public static void main(String[] args) throws Exception {

        // 8080番ポートでWebサーバーを作ります。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // ブラウザから「/」にアクセスされたときの処理を登録します。
        server.createContext("/", exchange -> {

            System.out.println("ハンドラが動いた");

            // ブラウザに返す文字を message という変数に入れます。
            String message = "超電磁砲 \n心理掌握 \n一方通行";

            // messageをUTF-8形式のバイトデータに変換します。
            byte[] response = message.getBytes(StandardCharsets.UTF_8);

            // 返すデータがUTF-8の文字であることをブラウザに伝えます。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

            // 正常に処理できたことを表す200と、返すデータの大きさを送ります。
            exchange.sendResponseHeaders(200, response.length);

            // ブラウザに返すデータを書き込みます。
            exchange.getResponseBody().write(response);

            // データの送信を終了します。
            exchange.getResponseBody().close();

            // 「/」にアクセスされたときの処理をここまでにします。
        });

        // Webサーバーを起動します。
        server.start();

        // 起動したことと、止め方をターミナルに表示します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");

        // mainメソッドをここで終了します。
    }

    // Appクラスをここで終了します。
}