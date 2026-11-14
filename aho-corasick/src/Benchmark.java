import java.io.PrintWriter;
import java.util.Random;


public class Benchmark {
    static final int[] PATTERN_COUNTS = {10, 100, 1000};
    static final int[] TEXT_LENGTHS = {1_000, 10_000, 100_000};
    static final int RUNS = 5;
    static final int ALPHABET = 26;
    static final int MIN_LEN = 3, MAX_LEN = 8;

    static volatile long sink;

    static String randomString(Random r, int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append((char) ('a' + r.nextInt(ALPHABET)));
        return sb.toString();
    }

    static String[] randomPatterns(Random r, int k) {
        String[] p = new String[k];
        for (int i = 0; i < k; i++) p[i] = randomString(r, MIN_LEN + r.nextInt(MAX_LEN - MIN_LEN + 1));
        return p;
    }

    public static void main(String[] args) throws Exception {
        Random rnd = new Random(2026);

        {
            String[] wp = randomPatterns(rnd, 100);
            String wt = randomString(rnd, 50_000);
            for (int i = 0; i < 20; i++) {
                sink += new AhoCorasick(wp).count(wt);
                sink += KMP.countAll(wt, wp);
            }
        }

        StringBuilder md = new StringBuilder();
        md.append("| Patterns | Text length | Matches | AC build (ms) | AC search (ms) | AC total (ms) | KMP total (ms) | KMP / AC | Trie nodes |\n");
        md.append("|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");

        try (PrintWriter csv = new PrintWriter("results/results.csv")) {
            csv.println("patterns,text_length,matches,ac_build_ms,ac_search_ms,ac_total_ms,kmp_total_ms,speedup,trie_nodes");

            for (int k : PATTERN_COUNTS) {
                for (int n : TEXT_LENGTHS) {
                    String[] pats = randomPatterns(rnd, k);
                    String text = randomString(rnd, n);

                    long buildSum = 0, searchSum = 0, kmpSum = 0;
                    long matchesAC = 0, matchesKMP = 0;
                    int nodes = 0;

                    for (int run = 0; run < RUNS; run++) {
                        long t0 = System.nanoTime();
                        AhoCorasick ac = new AhoCorasick(pats);
                        long t1 = System.nanoTime();
                        matchesAC = ac.count(text);
                        long t2 = System.nanoTime();
                        matchesKMP = KMP.countAll(text, pats);
                        long t3 = System.nanoTime();

                        buildSum += (t1 - t0);
                        searchSum += (t2 - t1);
                        kmpSum += (t3 - t2);
                        nodes = ac.nodeCount();
                        sink += matchesAC + matchesKMP;
                    }
                    if (matchesAC != matchesKMP)
                        throw new IllegalStateException("MISMATCH AC=" + matchesAC + " KMP=" + matchesKMP);

                    double build = buildSum / (double) RUNS / 1e6;
                    double search = searchSum / (double) RUNS / 1e6;
                    double ac = build + search;
                    double kmp = kmpSum / (double) RUNS / 1e6;

                    csv.printf(java.util.Locale.US, "%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.2f,%d%n",
                            k, n, matchesAC, build, search, ac, kmp, kmp / ac, nodes);
                    md.append(String.format(java.util.Locale.US,
                            "| %d | %d | %d | %.3f | %.3f | %.3f | %.3f | %.2fx | %d |%n",
                            k, n, matchesAC, build, search, ac, kmp, kmp / ac, nodes));
                    System.out.printf(java.util.Locale.US,
                            "patterns=%4d text=%6d matches=%6d | AC total=%8.3f ms (build %.3f + search %.3f) | KMP=%8.3f ms | KMP/AC=%.2fx%n",
                            k, n, matchesAC, ac, build, search, kmp, kmp / ac);
                }
            }
        }
        try (PrintWriter w = new PrintWriter("results/results.md")) { w.print(md); }
        System.out.println("\nSaved results/results.csv and results/results.md");
    }
}
