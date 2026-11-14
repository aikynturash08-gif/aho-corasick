import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


public class AhoCorasick {

    private static final int ALPHABET = 128;

    private static final class Node {
        final Node[] next = new Node[ALPHABET];
        Node fail;
        Node dict;
        List<Integer> ids;
        boolean isTerminal() { return ids != null; }
    }


    public interface MatchListener {
        /** @param patternId index of pattern in the input array; @param endIndex index of last matched char in text */
        void onMatch(int patternId, int endIndex);
    }

    private final Node root = new Node();
    private final String[] patterns;
    private int nodeCount = 1;


    public AhoCorasick(String[] patterns) {
        this.patterns = patterns.clone();
        buildTrie();
        buildLinks();
    }

    private void buildTrie() {
        for (int id = 0; id < patterns.length; id++) {
            String p = patterns[id];
            if (p.isEmpty()) continue;
            Node cur = root;
            for (int i = 0; i < p.length(); i++) {
                int c = p.charAt(i);
                if (c >= ALPHABET) throw new IllegalArgumentException("Only ASCII supported: " + p);
                if (cur.next[c] == null) {
                    cur.next[c] = new Node();
                    nodeCount++;
                }
                cur = cur.next[c];
            }
            if (cur.ids == null) cur.ids = new ArrayList<>(1);
            cur.ids.add(id);
        }
    }


    private void buildLinks() {
        root.fail = root;
        root.dict = null;
        Queue<Node> queue = new ArrayDeque<>();


        for (int c = 0; c < ALPHABET; c++) {
            Node v = root.next[c];
            if (v != null) {
                v.fail = root;
                v.dict = null;
                queue.add(v);
            }
        }

        while (!queue.isEmpty()) {
            Node u = queue.poll();
            for (int c = 0; c < ALPHABET; c++) {
                Node v = u.next[c];
                if (v == null) continue;


                Node f = u.fail;
                while (f != root && f.next[c] == null) f = f.fail;
                v.fail = (f.next[c] != null) ? f.next[c] : root;


                v.dict = v.fail.isTerminal() ? v.fail : v.fail.dict;

                queue.add(v);
            }
        }
    }

    public long search(String text, MatchListener listener) {
        long matches = 0;
        Node state = root;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c >= ALPHABET) { state = root; continue; }

            while (state != root && state.next[c] == null) state = state.fail;
            if (state.next[c] != null) state = state.next[c];

            Node out = state.isTerminal() ? state : state.dict;
            while (out != null) {
                for (int id : out.ids) {
                    matches++;
                    if (listener != null) listener.onMatch(id, i);
                }
                out = out.dict;
            }
        }
        return matches;
    }


    public long count(String text) {
        return search(text, null);
    }

    public List<int[]> findAll(String text) {
        List<int[]> res = new ArrayList<>();
        search(text, (id, end) -> res.add(new int[]{id, end - patterns[id].length() + 1}));
        return res;
    }

    public int nodeCount() { return nodeCount; }
}
