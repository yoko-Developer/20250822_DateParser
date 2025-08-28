package co.jp.ariseinnovation.AIReadR.Util;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CSVファイルの和暦を西暦YYYYMM形式に変換するプログラム（外部ライブラリなし）
 */
public class CsvWarekiConverter {

    public static void main(String[] args) throws Exception {
        Arguments params = Arguments.parse(args);
        if (!params.isValid()) {
            printUsage();
            System.exit(2);
        }

        System.out.println("=== CSV和暦変換プログラム ===");
        System.out.println("入力ファイル: " + params.input);
        System.out.println("出力ファイル: " + params.output);
        System.out.println("対象列: " + params.columns);
        System.out.println("文字コード: " + params.charset);
        System.out.println();

        Charset cs = Charset.forName(params.charset);
        List<String> lines = new ArrayList<>();
        
        // ファイルを読み込み
        System.out.println("ファイル読み込み中...");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(params.input), cs))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        
        System.out.println("読み込み完了: " + lines.size() + "行");
        System.out.println();
        
        // 変換処理
        System.out.println("変換処理開始...");
        List<String> convertedLines = new ArrayList<>();
        int convertedCount = 0;
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String convertedLine = convertLine(line, params.columns);
            
            if (!line.equals(convertedLine)) {
                convertedCount++;
            }
            
            convertedLines.add(convertedLine);
            
            // 進捗表示（100行ごと）
            if ((i + 1) % 100 == 0) {
                System.out.println("処理中: " + (i + 1) + "/" + lines.size() + "行");
            }
        }
        
        // ファイルに出力
        System.out.println("ファイル出力中...");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(params.output), cs))) {
            for (String convertedLine : convertedLines) {
                writer.write(convertedLine);
                writer.newLine();
            }
        }
        
        System.out.println();
        System.out.println("=== 変換完了 ===");
        System.out.println("変換された行数: " + convertedCount + "行");
        System.out.println("出力ファイル: " + params.output);
    }

    /**
     * CSVの1行を変換
     */
    private static String convertLine(String line, List<String> targetColumns) {
        // CSVの各行を解析
        String[] fields = parseCSVLine(line);
        if (fields.length < 5) {
            return line; // 列数が足りない場合はそのまま
        }
        
        String itemName = fields[0]; // 1列目：項目名
        String value = fields[4];    // 5列目：値
        
        // 対象項目かどうかをチェック
        boolean isTarget = false;
        for (String target : targetColumns) {
            if (itemName != null && itemName.contains(target)) {
                isTarget = true;
                break;
            }
        }
        
        if (isTarget && value != null && !value.trim().isEmpty()) {
            // 和暦を西暦に変換
            String convertedValue = convertToYearMonth(value.trim());
            if (!value.equals(convertedValue)) {
                fields[4] = convertedValue;
                System.out.println("変換: " + itemName + " = " + value + " → " + convertedValue);
            }
        }
        
        // フィールドを結合してCSV行を再構築（String.joinの代わりに手動で結合）
        return joinFields(fields);
    }

    /**
     * フィールドをカンマで結合（String.joinの代替）
     */
    private static String joinFields(String[] fields) {
        if (fields == null || fields.length == 0) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(fields[i] != null ? fields[i] : "");
        }
        return result.toString();
    }

    /**
     * CSV行をフィールドに分割
     */
    private static String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        
        // 最後のフィールド
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }

    /**
     * 和暦を西暦YYYYMM形式に変換
     */
    private static String convertToYearMonth(String src) {
        String trimmed = src == null ? "" : src.trim();
        if (trimmed.isEmpty()) return trimmed;
        
        try {
            return DateParser.Parse(trimmed);
        } catch (Exception e) {
            // 変換できない場合は元の値を返す
            return trimmed;
        }
    }

    /**
     * 使用方法を表示
     */
    private static void printUsage() {
        System.out.println("使用方法:");
        System.out.println("java -cp . co.jp.ariseinnovation.AIReadR.Util.CsvWarekiConverter --in <input.csv> --out <output.csv> [--cols col1,col2,...] [--charset UTF-8]");
        System.out.println();
        System.out.println("オプション:");
        System.out.println("  --in <file>     入力CSVファイル");
        System.out.println("  --out <file>    出力CSVファイル");
        System.out.println("  --cols <list>   変換対象列（カンマ区切り）");
        System.out.println("  --charset <cs>  文字コード（デフォルト: UTF-8）");
        System.out.println();
        System.out.println("例:");
        System.out.println("java -cp . CsvWarekiConverter --in input.csv --out output.csv --cols 取得,使用,供用");
        System.out.println();
        System.out.println("対象列の例: 取得,使用,供用,契約");
        System.out.println("変換例: H10.5 → 199805, H21.6 → 200906");
    }

    /**
     * コマンドライン引数を解析するクラス
     */
    static class Arguments {
        String input;
        String output;
        String charset = "UTF-8";
        List<String> columns;

        boolean isValid() {
            return input != null && output != null && !input.isEmpty() && !output.isEmpty();
        }

        static Arguments parse(String[] args) {
            Arguments a = new Arguments();
            for (int i = 0; i < args.length; i++) {
                String s = args[i];
                if ("--in".equals(s) && i + 1 < args.length) {
                    a.input = args[++i];
                } else if ("--out".equals(s) && i + 1 < args.length) {
                    a.output = args[++i];
                } else if ("--charset".equals(s) && i + 1 < args.length) {
                    a.charset = args[++i];
                } else if ("--cols".equals(s) && i + 1 < args.length) {
                    String[] cols = args[++i].split(",");
                    a.columns = new ArrayList<>();
                    for (String col : cols) {
                        String trimmed = col.trim();
                        if (!trimmed.isEmpty()) {
                            a.columns.add(trimmed);
                        }
                    }
                }
            }
            
            // デフォルトの対象列を設定
            if (a.columns == null || a.columns.isEmpty()) {
                a.columns = Arrays.asList("取得", "使用", "供用", "供用日", "契約");
            }
            
            return a;
        }
    }
}
