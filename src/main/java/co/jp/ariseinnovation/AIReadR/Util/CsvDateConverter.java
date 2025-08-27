package co.jp.ariseinnovation.AIReadR.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * CSVファイルの和暦データを西暦に変換するユーティリティクラス
 */
public class CsvDateConverter {
    
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormat.forPattern("yyyyMM");
    
    /**
     * CSVファイルの指定された列(名称)の和暦データを西暦に変換する
     * @param inputFilePath 入力CSVファイルのパス
     * @param outputFilePath 出力CSVファイルのパス
     * @param targetColumns 変換対象の列名のリスト (例: 取得, 使用, 事業共用日 など)
     * @throws Exception 変換処理でエラーが発生した場合
     */
    public static void convertCsvDates(String inputFilePath, String outputFilePath, List<String> targetColumns) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath), StandardCharsets.UTF_8))) {
            
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException("CSVファイルが空です");
            }
            
            // ヘッダー行を解析して列のインデックスを取得
            String[] headers = parseCsvLine(line);
            int itemNameIndex = -1;
            int keyWordIndex = -1;
            int valueIndex = -1;
            
            for (int i = 0; i < headers.length; i++) {
                if ("ItemName".equals(headers[i])) {
                    itemNameIndex = i;
                } else if ("KeyWord".equals(headers[i])) {
                    keyWordIndex = i;
                } else if ("Value".equals(headers[i])) {
                    valueIndex = i;
                }
            }
            
            if (valueIndex == -1) {
                throw new IllegalArgumentException("Value列が見つかりません");
            }
            if (itemNameIndex == -1 && keyWordIndex == -1) {
                throw new IllegalArgumentException("ItemName列またはKeyWord列が見つかりません");
            }
            
            // ヘッダー行を出力
            writer.write(line);
            writer.newLine();
            
            // データ行を処理
            while ((line = reader.readLine()) != null) {
                String[] fields = parseCsvLine(line);
                String[] convertedFields = Arrays.copyOf(fields, headers.length);
                
                String label = null;
                if (keyWordIndex != -1 && keyWordIndex < fields.length) {
                    label = fields[keyWordIndex];
                }
                // "null"(文字列)や空なら無効とみなす
                if (label == null || label.trim().isEmpty() || label.equalsIgnoreCase("null")) {
                    if (itemNameIndex != -1 && itemNameIndex < fields.length) {
                        label = fields[itemNameIndex];
                    }
                }
                
                // ラベル正規化
                String normalizedLabel = normalizeLabel(label);
                
                // ターゲット名と正規化・厳密一致で判定
                boolean isTarget = false;
                for (String t : targetColumns) {
                    String nt = normalizeLabel(t);
                    if (normalizedLabel.equals(nt)) {
                        isTarget = true;
                        break;
                    }
                }
                
                if (isTarget && valueIndex < fields.length) {
                    String original = fields[valueIndex] == null ? "" : fields[valueIndex];
                    String normalized = normalizeOcrDateString(original);
                    if (!normalized.isEmpty()) {
                        try {
                            String convertedDate = convertWarekiToSeireki(normalized);
                            if (convertedDate != null) {
                                convertedFields[valueIndex] = convertedDate;
                                System.out.println("変換: " + normalizedLabel + " = '" + original + "' → '" + convertedDate + "'");
                            }
                        } catch (Exception e) {
                            System.err.println("警告: '" + original + "' を変換できませんでした: " + e.getMessage());
                        }
                    }
                }
                
                writer.write(convertToCsvLine(convertedFields));
                writer.newLine();
            }
            
            System.out.println("変換が完了しました。出力ファイル: " + outputFilePath);
            
        } catch (IOException e) {
            throw new Exception("CSVファイルの読み書きでエラーが発生しました", e);
        }
    }
    
    /**
     * 和暦の日付文字列を西暦のYYYYMM形式に変換する
     */
    public static String convertWarekiToSeireki(String warekiDate) {
        if (warekiDate == null || warekiDate.trim().isEmpty()) {
            return null;
        }
        try {
            DateTime parsedDate = DateParser.Parse(warekiDate.trim());
            return parsedDate.toString(OUTPUT_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * OCR表記ゆれの正規化
     * 例: "H10 5" -> "H10.5", "H106" -> "H10.6", 全角/デリミタ混在を統一
     */
    private static String normalizeOcrDateString(String src) {
        if (src == null) return "";
        String s = src.trim();
        if (s.isEmpty()) return s;
        // 全角→半角などは DateParser 内でも処理するが、ここでも軽く正規化
        s = s.replace('\u3000', ' ');
        s = s.replace(" ", "");
        s = s.replace("年", ".");
        s = s.replace("月", ".");
        s = s.replace("日", "");
        s = s.replace("/", ".");
        s = s.replace("-", ".");
        s = s.replace("..", ".");
        // H106 -> H10.6 のように、元号+2桁年+1〜2桁月/日 っぽい場合に区切り挿入
        // まず元号プレフィックス
        if (s.matches("^[RrHhSs]\\d{3,4}$")) {
            // 例: H106, H1006
            String era = s.substring(0,1);
            String nums = s.substring(1);
            if (nums.length() == 3) {
                // yyM -> yy.M
                s = era + nums.substring(0,2) + "." + nums.substring(2);
            } else if (nums.length() == 4) {
                // yyMd -> yy.MM? ここでは yy.Md として DateParser に渡す
                s = era + nums.substring(0,2) + "." + nums.substring(2);
            }
        }
        // H10 5 / H10. 5 などの空白や余計なピリオドを整理
        s = s.replaceAll("\\.\\.", ".");
        s = s.replaceAll("\\.(?=\\.)", ".");
        s = s.replaceAll("\\.$", "");
        return s;
    }
    
    /**
     * ラベルの正規化
     * 例: "供給", "供給 " -> "共用", "目標", "目標 " -> "目標", 空白除去
     */
    private static String normalizeLabel(String s) {
        if (s == null) return "";
        String x = s.trim();
        if (x.equalsIgnoreCase("null")) return "";
        x = x.replace(" ", "");
        // 全角スペース除去のみ
        x = x.replace('　', ' ');
        x = x.replace("　", "");
        return x;
    }
    
    private static String[] parseCsvLine(String csvLine) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < csvLine.length(); i++) {
            char c = csvLine.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField.setLength(0);
            } else {
                currentField.append(c);
            }
        }
        fields.add(currentField.toString());
        return fields.toArray(new String[0]);
    }
    
    private static String convertToCsvLine(String[] fields) {
        StringBuilder csvLine = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) csvLine.append(',');
            String field = (fields[i] == null) ? "" : fields[i];
            if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
                csvLine.append('"').append(field.replace("\"", "\"\"")).append('"');
            } else {
                csvLine.append(field);
            }
        }
        return csvLine.toString();
    }
    
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("使用方法: java CsvDateConverter <入力ファイル> <出力ファイル> <列名1> [列名2] ...");
            System.out.println("例: java CsvDateConverter input.csv output.csv 取得 使用 事業共用日 共用日 契約開始日 事業共用");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];
        List<String> targetColumns = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            targetColumns.add(args[i]);
        }
        try {
            convertCsvDates(inputFile, outputFile, targetColumns);
        } catch (Exception e) {
            System.err.println("エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
