// Itemsという名前のクラス（プログラムをまとめる箱）を作ります。
public class Items {

    // mainメソッド（プログラムが最初に動き始める場所）を作ります。
    public static void main(String[] args) {

        // String型の配列（複数の文字列を順番に入れる箱）todosを作ります。
        String[] todos = { "牛乳を買う", "", "パンを買う", "掃除をする" };

        // iを0から始め、配列の件数より小さい間、繰り返します。
        boolean[] done = { true, false, false, false };

        for (int i = 1; i <= todos.length; i++) {

            // todosからi番目の文字列を取り出し、liタグで囲んで出力します。
            if (!todos[i].isEmpty()) {
                String mark = done[i] ? "[済] " : "";
                System.out.println("<li>" + mark + todos[i] + "</li>");
            }
        }
    }
}
