import java.io.*;
import java.util.*;
import java.util.function.*;

/** Q2 - ArrayList vs LinkedList vs ArrayDeque (+ manual array) for a playlist. Reports ns per operation. */
public class Main {
    record Song(int id) {}
    static long sink;
    static final int[] SIZES = {10_000, 50_000, 100_000};
    static final int REPS = 3;
    static PrintWriter csv;

    static Collection<Song> fill(Collection<Song> c, int n) { for (int i = 0; i < n; i++) c.add(new Song(i)); return c; }

    /** average ns/op: warm-up once, then REPS timed runs on freshly built data (setup not timed). */
    static <C> double measure(String structure, String op, int n, int ops, Supplier<C> setup, Consumer<C> body) {
        body.accept(setup.get());                       // warm-up
        double total = 0;
        for (int r = 0; r < REPS; r++) {
            C c = setup.get();
            long t0 = System.nanoTime();
            body.accept(c);
            double perOp = (System.nanoTime() - t0) / (double) ops;
            csv.printf("%s,%s,%d,%d,%.1f%n", structure, op, n, r + 1, perOp);
            total += perOp;
        }
        return total / REPS;
    }

    public static void main(String[] args) throws Exception {
        new java.io.File("results").mkdirs();
        // pass 0: JIT warm-up (output discarded); pass 1: the measured run
        PrintStream real = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        csv = new PrintWriter(Writer.nullWriter());
        run();
        System.setOut(real);
        csv = new PrintWriter("results/raw_q2.csv");
        csv.println("structure,operation,n,run,ns_per_op");
        run();
        csv.close();
        System.out.println("(sink=" + sink + ")");
    }

    static void run() throws Exception {
        String[] ops = {"add at end", "insert front", "insert middle", "get k-th", "iterate (per element)", "remove front", "remove middle"};
        for (int n : SIZES) {
            Random rnd = new Random(42);
            int K = 2000, KF = 5000;
            int[] idx = new int[K];
            for (int i = 0; i < K; i++) idx[i] = rnd.nextInt(n);
            System.out.println("\n=== n = " + n + " songs (ns per operation) ===");
            System.out.printf("%-24s %14s %14s %14s%n", "operation", "ArrayList", "LinkedList", "ArrayDeque");
            String[] S = {"ArrayList", "LinkedList", "ArrayDeque"};
            double[][] res = new double[ops.length][3];
            for (int s = 0; s < 3; s++) {
                final int sIdx = s;
                Supplier<Collection<Song>> empty = () -> sIdx == 0 ? new ArrayList<>() : sIdx == 1 ? new LinkedList<>() : new ArrayDeque<>();
                Supplier<Collection<Song>> full = () -> fill(empty.get(), n);
                res[0][s] = measure(S[s], ops[0], n, n, empty, c -> { for (int i = 0; i < n; i++) c.add(new Song(i)); });
                res[4][s] = measure(S[s], ops[4], n, n * 5, full, c -> { long sum = 0; for (int r = 0; r < 5; r++) for (Song x : c) sum += x.id(); sink += sum; });
                if (s == 2) {
                    res[1][s] = measure(S[s], ops[1], n, KF, full, c -> { ArrayDeque<Song> d = (ArrayDeque<Song>) c; for (int i = 0; i < KF; i++) d.addFirst(new Song(-i)); });
                    res[5][s] = measure(S[s], ops[5], n, KF, full, c -> { ArrayDeque<Song> d = (ArrayDeque<Song>) c; for (int i = 0; i < KF; i++) d.pollFirst(); });
                    res[2][s] = res[3][s] = res[6][s] = Double.NaN;
                } else {
                    res[1][s] = measure(S[s], ops[1], n, KF, full, c -> { List<Song> l = (List<Song>) c; for (int i = 0; i < KF; i++) l.add(0, new Song(-i)); });
                    res[2][s] = measure(S[s], ops[2], n, K, full, c -> { List<Song> l = (List<Song>) c; for (int i = 0; i < K; i++) l.add(l.size() / 2, new Song(-i)); });
                    res[3][s] = measure(S[s], ops[3], n, K, full, c -> { List<Song> l = (List<Song>) c; long sum = 0; for (int i = 0; i < K; i++) sum += l.get(idx[i]).id(); sink += sum; });
                    res[5][s] = measure(S[s], ops[5], n, KF, full, c -> { List<Song> l = (List<Song>) c; for (int i = 0; i < KF; i++) l.remove(0); });
                    res[6][s] = measure(S[s], ops[6], n, K, full, c -> { List<Song> l = (List<Song>) c; for (int i = 0; i < K; i++) l.remove(l.size() / 2); });
                }
            }
            for (int o = 0; o < ops.length; o++)
                System.out.printf("%-24s %14s %14s %14s%n", ops[o], f(res[o][0]), f(res[o][1]), f(res[o][2]));
        }
        upNext();
        manualArray();
    }

    static String f(double d) { return Double.isNaN(d) ? "n/a" : String.format("%.1f", d); }

    /** Changed condition: "Up Next" queue - add at both ends, remove from the front. */
    static void upNext() {
        int pre = 50_000, ops = 60_000;
        System.out.println("\n=== Up Next queue: preload " + pre + ", then " + ops + " mixed ops (addLast / addFirst / removeFirst) ===");
        String[] S = {"ArrayList", "LinkedList", "ArrayDeque"};
        for (int s = 0; s < 3; s++) {
            double best = Double.MAX_VALUE;
            for (int r = 0; r < 4; r++) {
                Collection<Song> c = fill(s == 0 ? new ArrayList<>() : s == 1 ? new LinkedList<>() : new ArrayDeque<>(), pre);
                long t0 = System.nanoTime();
                for (int i = 0; i < ops; i++) {
                    Song x = new Song(i);
                    switch (i % 3) {
                        case 0 -> { if (s == 2) ((ArrayDeque<Song>) c).addLast(x); else ((List<Song>) c).add(x); }
                        case 1 -> { if (s == 2) ((ArrayDeque<Song>) c).addFirst(x); else ((List<Song>) c).add(0, x); }
                        default -> { if (s == 2) ((ArrayDeque<Song>) c).pollFirst(); else ((List<Song>) c).remove(0); }
                    }
                }
                double ms = (System.nanoTime() - t0) / 1e6;
                if (r > 0) { csv.printf("%s,UpNext mixed,%d,%d,%.1f%n", S[s], pre, r, ms * 1e6 / ops); best = Math.min(best, ms); }
            }
            System.out.printf("%-12s best of 3 = %.1f ms%n", S[s], best);
        }
    }

    /** Plain array with manual shifting. */
    static class ManualList {
        Song[] a = new Song[16]; int size;
        void add(Song s) { if (size == a.length) a = Arrays.copyOf(a, size * 2); a[size++] = s; }
        void insert(int pos, Song s) {
            if (size == a.length) a = Arrays.copyOf(a, size * 2);
            for (int i = size; i > pos; i--) a[i] = a[i - 1];   // manual shifting
            a[pos] = s; size++;
        }
    }

    static void manualArray() {
        int n = 100_000, K = 5000;
        System.out.println("\n=== Plain array with manual shifting vs ArrayList: " + K + " front inserts, n = " + n + " ===");
        double mt = 0, at = 0;
        for (int r = 0; r < 4; r++) {
            ManualList m = new ManualList(); for (int i = 0; i < n; i++) m.add(new Song(i));
            ArrayList<Song> l = new ArrayList<>(); fill(l, n);
            long t0 = System.nanoTime(); for (int i = 0; i < K; i++) m.insert(0, new Song(-i)); double a = (System.nanoTime() - t0) / (double) K;
            t0 = System.nanoTime(); for (int i = 0; i < K; i++) l.add(0, new Song(-i)); double b = (System.nanoTime() - t0) / (double) K;
            if (r > 0) { mt += a; at += b; csv.printf("ManualArray,insert front,%d,%d,%.1f%n", n, r, a); }
        }
        System.out.printf("manual loop shift: %.1f ns/op   ArrayList.add(0,x) (System.arraycopy): %.1f ns/op%n", mt / 3, at / 3);
    }
}
