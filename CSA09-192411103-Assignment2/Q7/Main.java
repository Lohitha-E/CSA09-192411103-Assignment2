import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Q7 - Diagnose, redesign, validate (selected solution: Q6 triage waiting-list screen).
 * ORIGINAL : PriorityQueue; waiting list produced by copying the heap and draining it (re-sorts on every refresh).
 * REDESIGN : severity is a SMALL FIXED DOMAIN (1..5) and arrival order is insertion order, so keep one FIFO per severity.
 *            Every bucket is already in order -> admit O(1), serve O(1) (scan 5 heads), display = concatenation O(n), no sort.
 */
public class Main {
    static final com.sun.management.ThreadMXBean MX = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    static final class Patient {
        final String name; final int severity; final long arrival;
        Patient(String n, int s, long a) { name = n; severity = s; arrival = a; }
    }
    static final Comparator<Patient> RULE = Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival);

    interface Triage { void admit(Patient p); Patient next(); List<Patient> waitingList(); }

    static final class Original implements Triage {
        final PriorityQueue<Patient> q = new PriorityQueue<>(RULE);
        public void admit(Patient p) { q.add(p); }
        public Patient next() { return q.poll(); }
        public List<Patient> waitingList() {
            PriorityQueue<Patient> copy = new PriorityQueue<>(q);                 // O(n) copy
            List<Patient> out = new ArrayList<>(copy.size());
            while (!copy.isEmpty()) out.add(copy.poll());                         // n x O(log n)
            return out;
        }
    }
    static final class Redesign implements Triage {
        @SuppressWarnings("unchecked") final ArrayDeque<Patient>[] b = new ArrayDeque[5];
        Redesign() { for (int i = 0; i < 5; i++) b[i] = new ArrayDeque<>(); }
        public void admit(Patient p) { b[p.severity - 1].addLast(p); }
        public Patient next() { for (ArrayDeque<Patient> q : b) { Patient p = q.pollFirst(); if (p != null) return p; } return null; }
        public List<Patient> waitingList() { int n = 0; for (ArrayDeque<Patient> q : b) n += q.size(); List<Patient> out = new ArrayList<>(n); for (ArrayDeque<Patient> q : b) out.addAll(q); return out; }
    }

    static long sink;
    public static void main(String[] args) throws Exception {
        new java.io.File("results").mkdirs();
        int[] sizes = {10_000, 50_000, 100_000, 200_000};
        int DISPLAYS = 20, RUNS = 5;
        PrintWriter csv = new PrintWriter("results/raw_q7.csv");
        csv.println("n,design,run,admit_ms,display20x_ms,serve_ms,allocMB_total,sameOrder");
        long tid = Thread.currentThread().getId();
        System.out.printf("%-8s %-10s %10s %14s %10s %12s %10s%n", "n", "design", "admit ms", "20 displays ms", "serve ms", "alloc MB", "same order");
        for (int pass = 0; pass < 2; pass++) for (int n : sizes) {
            Random rnd = new Random(1); Patient[] in = new Patient[n];
            for (int i = 0; i < n; i++) in[i] = new Patient("P" + i, 1 + rnd.nextInt(5), i);
            for (int d = 0; d < 2; d++) {
                double A = 0, D = 0, S = 0, M = 0; boolean same = true;
                for (int r = 0; r < RUNS; r++) {
                    Triage t = d == 0 ? new Original() : new Redesign();
                    long a0 = MX.getThreadAllocatedBytes(tid);
                    long t0 = System.nanoTime(); for (Patient p : in) t.admit(p); double a = (System.nanoTime() - t0) / 1e6;
                    t0 = System.nanoTime(); List<Patient> last = null; for (int k = 0; k < DISPLAYS; k++) { last = t.waitingList(); sink += last.size(); } double dd = (System.nanoTime() - t0) / 1e6;
                    t0 = System.nanoTime(); List<Patient> served = new ArrayList<>(n); Patient p; while ((p = t.next()) != null) served.add(p); double s = (System.nanoTime() - t0) / 1e6;
                    double mb = (MX.getThreadAllocatedBytes(tid) - a0) / 1048576.0;
                    same &= last.equals(served);                                   // display order must equal treatment order
                    if (pass == 1) csv.printf("%d,%s,%d,%.2f,%.2f,%.2f,%.1f,%b%n", n, d == 0 ? "PriorityQueue" : "Buckets", r + 1, a, dd, s, mb, same);
                    A += a; D += dd; S += s; M += mb;
                }
                if (pass == 1) System.out.printf("%-8d %-10s %10.2f %14.2f %10.2f %12.1f %10b%n", n, d == 0 ? "PQ (orig)" : "Buckets", A / RUNS, D / RUNS, S / RUNS, M / RUNS, same);
            }
        }
        // Cross-check: both designs serve in the identical order
        Random rnd = new Random(99); Original o = new Original(); Redesign rd = new Redesign();
        for (int i = 0; i < 50_000; i++) { Patient p = new Patient("P" + i, 1 + rnd.nextInt(5), i); o.admit(p); rd.admit(p); }
        boolean eq = true; Patient x, y; while ((x = o.next()) != null) { y = rd.next(); eq &= x == y; }
        System.out.println("\nCross-check on 50,000 random patients - identical service order: " + eq);
        csv.close(); System.out.println("(sink=" + sink + ")");
    }
}
