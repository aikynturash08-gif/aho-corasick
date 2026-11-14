# Aho–Corasick vs. KMP (DAA Bonus Task)

Java implementation of the **Aho–Corasick** multi-pattern string matching algorithm, tests, an
experimental comparison with **KMP run separately for every pattern**, and complexity analysis.

## Project structure

```
src/AhoCorasick.java   trie + failure links + output links + search
src/KMP.java           prefix function + single-pattern search (+ countAll for many patterns)
src/Tests.java         13 tests, incl. 300 random cross-checks (AC == brute force == KMP)
src/Benchmark.java     experiments: 10/100/1000 patterns x 1k/10k/100k text, 5 runs each
results/results.csv    raw averaged results      results/results.md   same as a table
run.sh                 compile + run everything
```

## How to run

```bash
./run.sh                        # or manually:
javac -d out src/*.java
java -cp out Tests              # correctness tests
java -cp out Benchmark          # experiments -> results/results.csv, results.md
```

## Implementation overview

* **Trie construction** – every pattern is inserted character by character; patterns with a common
  prefix share the same path. A node where a pattern ends stores the pattern id(s) (duplicates are supported).
* **Failure links** – `fail(v)` points to the node of the *longest proper suffix of the string of v
  that is also a prefix of some pattern*. Built with BFS (by depth): for a child `v` of `u` reached by
  character `c`, start at `u.fail` and go up the fail-chain until a node with an edge `c` is found; `v.fail`
  is that edge's target (or the root).
* **Output links (dictionary links)** – `dict(v)` = nearest node on the fail-chain of `v` that ends a pattern.
  Without it, after each character we would have to walk the whole fail-chain to find patterns that are
  suffixes of the current text (e.g. `he` inside `she`). With it, we visit only nodes that really produce a match.
* **Search** – one pass over the text. For each character: while there is no edge, follow `fail`; then move
  along the edge; then report the current node (if terminal) and all nodes reached via `dict`.
  All (overlapping) occurrences of all patterns are reported.

## Experimental setup

| Parameter | Value |
|---|---|
| Patterns | 10, 100, 1000 random lowercase strings (a–z), length 3–8 |
| Text | random lowercase string of length 1 000, 10 000, 100 000 |
| Timing | `System.nanoTime()`, each experiment run **5 times**, average reported |
| Warm-up | one unmeasured warm-up phase at program start so the JIT compiler has compiled the hot code |
| Seed | fixed (`2026`) – results are reproducible in terms of data |
| AC time | build (trie + links) + search; both shown separately |
| KMP time | prefix function + search for **each** pattern separately, summed |
| Check | for every case the number of matches of AC and KMP is compared (must be equal, else exception) |

## Results (average of 5 runs, milliseconds)

| Patterns | Text length | Matches | AC build (ms) | AC search (ms) | AC total (ms) | KMP total (ms) | KMP / AC | Trie nodes |
|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 10 | 1000 | 1 | 0.464 | 0.252 | 0.716 | 0.032 | 0.04x | 58 |
| 10 | 10000 | 1 | 0.067 | 0.326 | 0.393 | 1.107 | 2.82x | 56 |
| 10 | 100000 | 23 | 0.087 | 4.640 | 4.727 | 4.106 | 0.87x | 44 |
| 100 | 1000 | 0 | 0.267 | 0.017 | 0.284 | 0.209 | 0.74x | 483 |
| 100 | 10000 | 10 | 0.250 | 0.883 | 1.134 | 2.789 | 2.46x | 493 |
| 100 | 100000 | 113 | 1.986 | 1.441 | 3.426 | 23.059 | 6.73x | 469 |
| 1000 | 1000 | 11 | 3.902 | 0.025 | 3.928 | 1.994 | 0.51x | 4012 |
| 1000 | 10000 | 119 | 2.572 | 0.161 | 2.733 | 20.101 | 7.35x | 3977 |
| 1000 | 100000 | 953 | 3.052 | 2.672 | 5.724 | 216.711 | 37.86x | 4026 |

`KMP / AC` > 1 means Aho–Corasick is faster.
Exact numbers depend on the machine; rerun `Benchmark` to get your own.

### Discussion of the results

* KMP time grows **linearly with the number of patterns**: for the 100 000-char text it goes
  4.1 ms → 23.1 ms → 216.7 ms for 10 → 100 → 1000 patterns (the text is scanned once per pattern).
  Aho–Corasick time stays almost flat (4.7 → 3.4 → 5.7 ms) because the text is scanned only once.
* The gap grows with both parameters; the largest case (1000 patterns, 100 000 chars) shows AC ≈ **38× faster**.
* For **small inputs** KMP can be faster (e.g. 10 patterns × 1 000 chars, 1000 patterns × 1 000 chars): AC must first
  build the trie and links, and this fixed cost is not paid back on a short text.
* Small-size timings (sub-millisecond) are **noisy**: only 5 runs, JIT compilation and GC influence them. For example
  the 10 patterns × 100 000 chars case is not slower for AC "in theory"; with so few and so short patterns the
  difference is within measurement noise/constant factors (AC uses a 128-wide array per node and pointer chasing,
  KMP works on a tiny array). The clear trend appears for 100+ patterns and 10 000+ characters.
* The number of trie nodes is below the total length of all patterns (e.g. 1000 patterns × avg 5.5 chars ≈ 5500 chars
  → ~4000 nodes) because common prefixes are shared.

## Complexity analysis

Notation: `n` = text length, `m` = total length of all patterns, `k` = number of patterns,
`σ` = alphabet size (128 here), `z` = number of reported occurrences.

| Phase | Time | Explanation |
|---|---|---|
| Trie build | O(m) | each pattern character is processed once (array child lookup is O(1)) |
| Failure/output links | O(m·σ) with array children (O(m) amortised with σ treated as constant) | BFS visits each node once; the fail-chain walk is amortised O(1) per node by the same argument as in KMP's prefix function (depth of the fail target grows by ≤ 1 per step and each fail step decreases it). The `σ` factor comes only from scanning the 128 child slots of each node. |
| Search | O(n + z) | each text character moves the state forward by one edge; fail steps can never exceed forward steps (amortised), so ≤ 2n transitions; `z` is the cost of reporting matches via output links |
| **Total** | **O(m + n + z)** | independent of the number of patterns *except through m* |

**Space:** O(m·σ) with child arrays (≈ nodes × 128 references; at most m+1 nodes), or O(m) with
hash-map/sorted-list children. Plus O(k) for pattern ids.

**KMP for each pattern separately:** for pattern `i` of length `mᵢ`: O(mᵢ + n). Total
**O(m + k·n)** time, O(max mᵢ) extra space. The `k·n` term is exactly what Aho–Corasick removes, and
what we observe in the experiment (KMP time ∝ k).

## Roles of the trie and failure links; why AC suits multi-pattern search

* **Trie** merges all patterns into one automaton: the current state always represents the longest
  suffix of the text read so far that is a prefix of *some* pattern. All patterns are tracked simultaneously.
* **Failure links** are the "KMP idea for a trie": when the next character does not extend the current
  match, we do not restart; we jump to the longest shorter suffix that is still a prefix of some pattern.
  This guarantees the text pointer never moves backward → linear time.
* **Output links** make sure that patterns that are suffixes of other matched strings (`he` in `she`) are
  found without scanning the whole fail-chain.
* So the text is read **once** regardless of how many patterns there are – ideal for dictionaries,
  virus signatures, keyword filters, DNA motif search, intrusion detection.

## Advantages and limitations

| | Aho–Corasick | KMP per pattern |
|---|---|---|
| Time | O(n + m + z) | O(k·n + m) |
| Scales with many patterns | Excellent | Poor (linear in k) |
| Preprocessing | Heavier: trie + links, memory O(m·σ) | Very light, O(mᵢ) per pattern |
| Memory | Larger (whole trie in memory) | Tiny |
| Short text / very few patterns | Setup cost may dominate | Often as fast or faster, simpler |
| Patterns added dynamically | Need rebuild (or dynamic variants) | Trivial – just run another KMP |
| Implementation | More complex | Simple |
| Streaming text | Natural (one state kept) | Need k states in parallel |

**Conclusion:** use KMP for one or very few patterns; use Aho–Corasick when the pattern set is large
or reused over a lot of text – its cost depends on `n + m + z` instead of `k·n`.
