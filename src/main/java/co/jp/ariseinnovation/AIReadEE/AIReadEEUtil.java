package co.jp.ariseinnovation.AIReadEE;

/**
 * AIReadEEのユーティリティクラス
 */
public class AIReadEEUtil {
    
    /**
     * 文字列がnullまたは空文字列かどうかをチェック
     * @param str チェックする文字列
     * @return nullまたは空文字列の場合true
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    /**
     * 文字列がnull、空文字列、または空白文字のみかどうかをチェック
     * @param str チェックする文字列
     * @return null、空文字列、または空白文字のみの場合true
     */
    public static boolean isNullOrWhiteSpace(String str) {
        return str == null || str.trim().isEmpty();
    }
}
