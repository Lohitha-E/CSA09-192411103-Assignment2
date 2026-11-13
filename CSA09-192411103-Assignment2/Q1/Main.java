import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;

/** Q1 - Building text under load: + vs StringBuilder vs StringBuffer (+ join, StringJoiner, format). */
public class Main {
    static final com.sun.management.ThreadMXBean MX =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static long sink;
    static String fmt;

    static String[] fields(int n) {
        String[] f = new String[n];
        for (int i = 0; i < n; i++) f[i] = "F" + i + "=V" + (i * 7919 % 1000);
        return f;
    }

    static int plus(String[] f)   { String s = ""; for (String x : f) s = s + x + ";"; return s.length(); }
    static int sb(String[] f)     { StringBuilder b = new StringBuilder(); for (String x : f) b.append(x).append(';'); return b.toString().length(); }
    static int sbuf(String[] f)   { StringBuffer b = new StringBuffer(); for (String x : f) b.append(x).append(';'); return b.toString().length(); }
    static int join(String[] f)   { return String.join(";", f).length(); }
    static int joiner(String[] f) { StringJoiner j = new StringJoiner(";"); for (String x : f) j.add(x); return j.toString().length(); }
    static int format(String[] f) { return String.format(fmt, (Object[]) f).length(); }

    interface B { int build(String[] f); }
    static final String[] NAMES = {"plus(+)", "StringBuilder", "StringBuffer", "String.join", "StringJoiner", "String.format"};
    static final B[] BUILDERS = {Main::plus, Main::sb, Main::sbuf, Main::join, Main::joiner, Main::format};

    public static void main(String[] a) throws Exception {
        new java.io.File("results").mkdirs();
        int[][] workloads = {{200_000, 10}, {10_000, 50}, {20, 3000}};
        String[] wname = {"A: short ticket (10 fields) x 200000", "B: 10000 tickets x 50 fields", "C: 20 very long tickets x 3000 fields"};
        int WARM = 2, RUNS = 5;
        PrintWriter csv = new PrintWriter("results/raw_q1.csv");
        csv.println("workload,approach,run,ms,allocatedMB,heapDeltaMB");
        Runtime rt = Runtime.getRuntime();
        long tid = Thread.currentThread().getId();
        for (int w = 0; w < workloads.length; w++) {
            int tickets = workloads[w][0], n = workloads[w][1];
            String[] f = fields(n);
            fmt = "%s;".repeat(n);
            System.out.println("\n=== Workload " + wname[w] + " ===");
            System.out.printf("%-14s %10s %12s %16s%n", "approach", "avg ms", "alloc MB", "est. temp Strings");
            for (int k = 0; k < BUILDERS.length; k++) {
                for (int i = 0; i < WARM; i++) for (int t = 0; t < tickets; t++) sink += BUILDERS[k].build(f);
                double sumMs = 0, sumAlloc = 0;
                for (int r = 0; r < RUNS; r++) {
                    System.gc();
                    long usedBefore = rt.totalMemory() - rt.freeMemory();
                    long a0 = MX.getThreadAllocatedBytes(tid);
                    long t0 = System.nanoTime();
                    for (int t = 0; t < tickets; t++) sink += BUILDERS[k].build(f);
                    double ms = (System.nanoTime() - t0) / 1e6;
                    double alloc = (MX.getThreadAllocatedBytes(tid) - a0) / 1048576.0;
                    double heapDelta = ((rt.totalMemory() - rt.freeMemory()) - usedBefore) / 1048576.0;
                    csv.printf("\"%s\",%s,%d,%.3f,%.2f,%.2f%n", wname[w], NAMES[k], r + 1, ms, alloc, heapDelta);
                    sumMs += ms; sumAlloc += alloc;
                }
                long est = k == 0 ? (long) tickets * n : (long) tickets; // + : one new String per field; others: 1 final String
                System.out.printf("%-14s %10.2f %12.1f %16d%n", NAMES[k], sumMs / RUNS, sumAlloc / RUNS, est);
            }
        }
        csv.close();
        threadSafetyDemo();
        System.out.println("\n(sink=" + sink + ")");
    }

    /** Several threads append to one shared log: StringBuilder is not thread-safe, StringBuffer is. */
    static void threadSafetyDemo() throws Exception {
        final int THREADS = 8, PER = 200_000;
        System.out.println("\n=== Shared booking log, " + THREADS + " threads x " + PER + " appends (cores=" + Runtime.getRuntime().availableProcessors() + ") ===");
        for (int trial = 1; trial <= 5; trial++) {
            long[] res = new long[2]; String[] err = {"none", "none"};
            for (int kind = 0; kind < 2; kind++) {
                final Appendable[] dummy = null;
                final StringBuilder sbd = new StringBuilder();
                final StringBuffer sbf = new StringBuffer();
                final int kk = kind;
                final int idx = kind;
                Thread[] ts = new Thread[THREADS];
                for (int i = 0; i < THREADS; i++) {
                    ts[i] = new Thread(() -> {
                        try { for (int j = 0; j < PER; j++) { if (kk == 0) sbd.append("PNR-1234;"); else sbf.append("PNR-1234;"); } }
                        catch (Throwable e) { err[idx] = e.getClass().getSimpleName(); }
                    });
                }
                for (Thread t : ts) t.start();
                for (Thread t : ts) t.join();
                res[kind] = kind == 0 ? sbd.length() : sbf.length();
            }
            long expected = (long) THREADS * PER * 9;
            System.out.printf("trial %d: expected=%d  StringBuilder=%d (exception: %s)  StringBuffer=%d (exception: %s)%n",
                    trial, expected, res[0], err[0], res[1], err[1]);
        }
    }
}
