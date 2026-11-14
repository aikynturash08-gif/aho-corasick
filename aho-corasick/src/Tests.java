import java.util.*;

public class Tests {
    private static int passed = 0, failed = 0;

    private static void check(String name, boolean ok) {
        if (ok) { passed++; System.out.println("[PASS] " + name); }
        else    { failed++; System.out.println("[FAIL] " + name); }
    }

    private static Set<String> naive(String text, String[] pats) {
        Set<String> s = new TreeSet<>();
        for (int id = 0; id < pats.length; id++) {
            if (pats[id].isEmpty()) continue;
            for (int i = 0; i + pats[id].length() <= text.length(); i++)
                if (text.startsWith(pats[id], i)) s.add(id + "@" + i);
        }
        return s;
    }

    private static Set<String> ac(String text, String[] pats) {
        Set<String> s = new TreeSet<>();
        for (int[] m : new AhoCorasick(pats).findAll(text)) s.add(m[0] + "@" + m[1]);
        return s;
    }

    public static void main(String[] args) {
        String[] classic = {"he", "she", "his", "hers"};
        check("classic: 'ushers' -> she@1, he@2, hers@2",
                ac("ushers", classic).equals(new TreeSet<>(List.of("1@1", "0@2", "3@2"))));

        check("output links: {a, aa, aaa} in 'aaaa' = 4+3+2 = 9 matches",
                new AhoCorasick(new String[]{"a", "aa", "aaa"}).count("aaaa") == 9);

        check("overlap: 'aba' in 'ababababa' = 4",
                new AhoCorasick(new String[]{"aba"}).count("ababababa") == 4);

        check("duplicates: {ab, ab} in 'abab' = 4",
                new AhoCorasick(new String[]{"ab", "ab"}).count("abab") == 4);

        check("no match", new AhoCorasick(new String[]{"xyz", "qq"}).count("abcabcabc") == 0);
        check("empty text", new AhoCorasick(new String[]{"a"}).count("") == 0);
        check("single chars: {a,b} in 'abba' = 4", new AhoCorasick(new String[]{"a", "b"}).count("abba") == 4);

        check("pattern == text", new AhoCorasick(new String[]{"hello"}).count("hello") == 1);
        check("pattern longer than text", new AhoCorasick(new String[]{"hello!"}).count("hello") == 0);

        check("fail link: 'abcd' in 'abcabcd' at 3",
                ac("abcabcd", new String[]{"abcd"}).equals(Set.of("0@3")));

        check("KMP: 'aba' in 'ababababa' = 4", KMP.count("ababababa", "aba") == 4);
        check("KMP positions", KMP.findAll("ababababa", "aba").equals(List.of(0, 2, 4, 6)));

        Random rnd = new Random(12345);
        boolean allOk = true;
        for (int t = 0; t < 300 && allOk; t++) {
            int alpha = 2 + rnd.nextInt(3);
            int k = 1 + rnd.nextInt(15);
            String[] pats = new String[k];
            for (int i = 0; i < k; i++) pats[i] = rand(rnd, 1 + rnd.nextInt(6), alpha);
            String text = rand(rnd, rnd.nextInt(200), alpha);

            Set<String> expected = naive(text, pats);
            if (!ac(text, pats).equals(expected)) allOk = false;
            if (new AhoCorasick(pats).count(text) != KMP.countAll(text, pats)) allOk = false;
            if (KMP.countAll(text, pats) != expected.size()) allOk = false;
        }
        check("300 random tests: AC == brute force == KMP", allOk);

        System.out.println("\nPassed: " + passed + ", Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    private static String rand(Random r, int len, int alpha) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append((char) ('a' + r.nextInt(alpha)));
        return sb.toString();
    }
}
