package cx.telosa.urlshortener;

import java.util.*;




public class Utils {
    public static Map<String, String> parseJson(String json) {
        Map<String, String> map = new HashMap<>();


        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);


        boolean inQuotes = false;
        StringBuilder keyValue = new StringBuilder();
        List<String> entries = new ArrayList<>();

        for (char c : json.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            }
            if (c == ',' && !inQuotes) {
                entries.add(keyValue.toString());
                keyValue.setLength(0);
            } else {
                keyValue.append(c);
            }
        }
        entries.add(keyValue.toString()); // Add last entry

        for (String entry : entries) {
            String[] pair = entry.split(":", 2); // Only split at the first colon
            if (pair.length == 2) {
                String key = pair[0].trim().replaceAll("^\"|\"$", "");
                String value = pair[1].trim().replaceAll("^\"|\"$", "");
                map.put(key, value);
            }
        }

        return map;
    }
}
