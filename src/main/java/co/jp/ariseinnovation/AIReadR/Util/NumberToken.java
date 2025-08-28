package co.jp.ariseinnovation.AIReadR.Util;

/**
 * 日付文字列から抽出された数値トークンを管理するクラス
 */
public class NumberToken {

    public final int token;
    public final String tokenAsString;
    public final String afterWord;

    /**
     * 数値のみのコンストラクタ
     * @param token 数値
     */
    public NumberToken(int token) {
        this.token = token;
        this.tokenAsString = String.valueOf(token);
        this.afterWord = "";
    }

    /**
     * 文字列から数値を抽出するコンストラクタ
     * @param tokenAsString 数値文字列
     */
    public NumberToken(String tokenAsString) {
        this.tokenAsString = tokenAsString;
        this.token = Integer.parseInt(tokenAsString);
        this.afterWord = "";
    }

    /**
     * 数値と後続文字列を指定するコンストラクタ
     * @param tokenAsString 数値文字列
     * @param afterWord 後続文字列
     */
    public NumberToken(String tokenAsString, String afterWord) {
        this.tokenAsString = tokenAsString;
        this.token = Integer.parseInt(tokenAsString);
        this.afterWord = afterWord;
    }

    /**
     * デフォルトコンストラクタ
     */
    public NumberToken() {
        this.token = 0;
        this.tokenAsString = "0";
        this.afterWord = "";
    }

    /**
     * 後続文字列に指定の文字が含まれているかチェック
     * @param word チェックする文字
     * @return 含まれている場合true
     */
    public boolean HasAfter(String word) {
        return afterWord != null && afterWord.contains(word);
    }

    /**
     * 配列を作成するファクトリメソッド
     * @param tokens 数値の配列
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(int... tokens) {
        NumberToken[] result = new NumberToken[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = new NumberToken(tokens[i]);
        }
        return result;
    }

    /**
     * 配列を作成するファクトリメソッド（文字列版）
     * @param tokens 数値文字列の配列
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(String... tokens) {
        NumberToken[] result = new NumberToken[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            result[i] = new NumberToken(tokens[i]);
        }
        return result;
    }

    /**
     * 配列を作成するファクトリメソッド（NumberToken版）
     * @param tokens NumberTokenの配列
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(NumberToken... tokens) {
        return tokens;
    }

    /**
     * 混合型の配列を作成するファクトリメソッド（String, String, int）
     * @param first 最初の文字列
     * @param second 2番目の文字列
     * @param third 3番目の数値
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(String first, String second, int third) {
        return new NumberToken[] {
                new NumberToken(first),
                new NumberToken(second),
                new NumberToken(third)
        };
    }

    /**
     * 混合型の配列を作成するファクトリメソッド（String, String, NumberToken）
     * @param first 最初の文字列
     * @param second 2番目の文字列
     * @param third 3番目のNumberToken
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(String first, String second, NumberToken third) {
        return new NumberToken[] {
                new NumberToken(first),
                new NumberToken(second),
                third
        };
    }

    /**
     * 混合型の配列を作成するファクトリメソッド（NumberToken, NumberToken, int）
     * @param first 最初のNumberToken
     * @param second 2番目のNumberToken
     * @param third 3番目の数値
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(NumberToken first, NumberToken second, int third) {
        return new NumberToken[] {
                first,
                second,
                new NumberToken(third)
        };
    }

    /**
     * 混合型の配列を作成するファクトリメソッド（int, NumberToken, NumberToken）
     * @param first 最初の数値
     * @param second 2番目のNumberToken
     * @param third 3番目のNumberToken
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(int first, NumberToken second, NumberToken third) {
        return new NumberToken[] {
                new NumberToken(first),
                second,
                third
        };
    }

    /**
     * 混合型の配列を作成するファクトリメソッド（NumberToken, String, String）
     * @param first 最初のNumberToken
     * @param second 2番目の文字列
     * @param third 3番目の文字列
     * @return NumberTokenの配列
     */
    public static NumberToken[] CreateArray(NumberToken first, String second, String third) {
        return new NumberToken[] {
                first,
                new NumberToken(second),
                new NumberToken(third)
        };
    }
}
