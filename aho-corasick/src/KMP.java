import java.util.ArrayList;
import java.util.List;

public class KMP {

    public static int[] prefixFunction(String p) {
        int[] pi = new int[p.length()];
        for (int i = 1, k = 0; i < p.length(); i++) {
            while (k > 0 && p.charAt(i) != p.charAt(k)) k = pi[k - 1];
            if (p.charAt(i) == p.charAt(k)) k++;
            pi[i] = k;
        }
        return pi;
    }

    public static long count(String text, String p) {
        if (p.isEmpty()) return 0;
        int[] pi = prefixFunction(p);
        long matches = 0;
        for (int i = 0, k = 0; i < text.length(); i++) {
            while (k > 0 && (k == p.length() || text.charAt(i) != p.charAt(k))) k = pi[k - 1];
            if (text.charAt(i) == p.charAt(k)) k++;
            if (k == p.length()) matches++;
        }
        return matches;
    }

    public static List<Integer> findAll(String text, String p) {
        List<Integer> res = new ArrayList<>();
        if (p.isEmpty()) return res;
        int[] pi = prefixFunction(p);
        for (int i = 0, k = 0; i < text.length(); i++) {
            while (k > 0 && (k == p.length() || text.charAt(i) != p.charAt(k))) k = pi[k - 1];
            if (text.charAt(i) == p.charAt(k)) k++;
            if (k == p.length()) res.add(i - p.length() + 1);
        }
        return res;
    }

    public static long countAll(String text, String[] patterns) {
        long total = 0;
        for (String p : patterns) total += count(text, p);
        return total;
    }
}
