import java.util.*;


class Patient {
    final String name; final int severity; final long arrival;
    Patient(String name, int severity, long arrival) {
        this.name = name; this.severity = severity; this.arrival = arrival;
    }
    @Override public String toString() { return name + "(S" + severity + ")"; }
}

class Triage {
    private long counter = 0;
    // TODO 1 (done): lowest severity number first; for equal severity, earliest arrival first
    private final PriorityQueue<Patient> queue = new PriorityQueue<>(
            Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival));

    void admit(String name, int severity) {
        queue.add(new Patient(name, severity, counter++));
    }
    Patient next() { return queue.poll(); }

    public static void main(String[] args) {
        Triage er = new Triage();
        er.admit("Ravi", 3); er.admit("Meena", 1); er.admit("John", 3);
        er.admit("Asha", 5); er.admit("Kumar", 1); er.admit("Lata", 2);

        StringBuilder order = new StringBuilder();
        Patient p;
        while ((p = er.next()) != null) order.append(p).append(' ');
        System.out.println(order.toString().trim());
    }
}

public class Main {
    public static void main(String[] args) {
        Triage.main(args);          // Complete the Code program
        System.out.println();
        Experiments.main(args);     // correctness check, growth, starvation
    }
}


/** Q6 - student-designed triage: hand check, growth, starvation and fair (aging) rule, bucket-queue alternative. */
class Experiments {
    static final class Patient {
        final String name; final int severity; final long arrival;
        Patient(String n, int s, long a) { name = n; severity = s; arrival = a; }
        @Override public String toString() { return name + "(S" + severity + ")"; }
    }
    static final Comparator<Patient> RULE = Comparator.comparingInt((Patient p) -> p.severity).thenComparingLong(p -> p.arrival);

    /** Design 1: PriorityQueue + arrival counter as tie-breaker. */
    static final class HeapTriage {
        private final PriorityQueue<Patient> q = new PriorityQueue<>(RULE);
        private long counter;
        void admit(String n, int s) { q.add(new Patient(n, s, counter++)); }
        Patient next() { return q.poll(); }
        int size() { return q.size(); }
        /** waiting list in treatment order: copy then drain - O(n log n) */
        List<Patient> waitingList() {
            PriorityQueue<Patient> copy = new PriorityQueue<>(q);
            List<Patient> out = new ArrayList<>(copy.size());
            while (!copy.isEmpty()) out.add(copy.poll());
            return out;
        }
    }

    /** Design 2 (alternative): five FIFO queues, one per severity, with optional AGING. */
    static final class BucketTriage {
        @SuppressWarnings("unchecked") private final ArrayDeque<Patient>[] b = new ArrayDeque[5];
        private long counter; private int size; private final long agingStep; // 0 = no aging
        BucketTriage(long agingStep) { this.agingStep = agingStep; for (int i = 0; i < 5; i++) b[i] = new ArrayDeque<>(); }
        void admit(String n, int s) { b[s - 1].addLast(new Patient(n, s, counter++)); size++; }
        int size() { return size; }
        private long effective(Patient p) {
            if (agingStep == 0) return p.severity;
            long waited = counter - p.arrival;                         // logical clock = number of admissions so far
            return Math.max(1, p.severity - waited / agingStep);       // every agingStep arrivals waited = one level up
        }
        /** Only the HEAD of each FIFO can be best (it has waited longest in its level) -> check 5 heads: O(1). */
        Patient next() {
            Patient best = null; long bestEff = 0;
            for (ArrayDeque<Patient> q : b) {
                Patient h = q.peekFirst(); if (h == null) continue;
                long e = effective(h);
                if (best == null || e < bestEff || (e == bestEff && h.arrival < best.arrival)) { best = h; bestEff = e; }
            }
            if (best != null) { b[best.severity - 1].pollFirst(); size--; }
            return best;
        }
        List<Patient> waitingList() { List<Patient> out = new ArrayList<>(size); for (ArrayDeque<Patient> q : b) out.addAll(q); return out; } // no-aging order: O(n)
    }

    public static void main(String[] args) {
        System.out.println("=== Correctness check: 8 patients, order worked out by hand ===");
        String[][] in = {{"Ravi","3"},{"Meena","1"},{"John","3"},{"Asha","5"},{"Kumar","1"},{"Lata","2"},{"Dev","2"},{"Sana","3"}};
        HeapTriage h = new HeapTriage(); BucketTriage bt = new BucketTriage(0);
        for (String[] p : in) { h.admit(p[0], Integer.parseInt(p[1])); bt.admit(p[0], Integer.parseInt(p[1])); }
        String hand = "Meena(S1) Kumar(S1) Lata(S2) Dev(S2) Ravi(S3) John(S3) Sana(S3) Asha(S5)";
        // printing the PriorityQueue directly shows the HEAP ARRAY layout, not the treatment order
        PriorityQueue<Patient> raw = new PriorityQueue<>(RULE);
        long c = 0; for (String[] p : in) raw.add(new Patient(p[0], Integer.parseInt(p[1]), c++));
        System.out.println("hand-worked order : " + hand);
        System.out.println("println(queue)    : " + raw + "   <-- heap array order, NOT treatment order");
        System.out.println("waitingList() heap: " + join(h.waitingList()));
        StringBuilder sb = new StringBuilder(); Patient p; while ((p = h.next()) != null) sb.append(p).append(' ');
        System.out.println("poll() order      : " + sb.toString().trim());
        System.out.println("matches hand order: " + sb.toString().trim().equals(hand));
        StringBuilder sb2 = new StringBuilder(); while ((p = bt.next()) != null) sb2.append(p).append(' ');
        System.out.println("bucket design     : " + sb2.toString().trim() + "  matches: " + sb2.toString().trim().equals(hand));

        System.out.println("\n=== Growth investigation: predicted admit O(log n), serve O(log n); ns per operation, avg of 5 runs ===");
        System.out.printf("%-10s %-14s %14s %14s %16s%n", "n", "design", "admit ns/op", "serve ns/op", "display ms (1x)");
        int[] sizes = {1_000, 10_000, 100_000, 1_000_000};
        for (int pass = 0; pass < 2; pass++) for (int n : sizes) for (int d = 0; d < 2; d++) {
            double adm = 0, srv = 0, dsp = 0; int R = 5;
            Random rnd = new Random(1); int[] sev = new int[n]; for (int i = 0; i < n; i++) sev[i] = 1 + rnd.nextInt(5);
            for (int r = 0; r < R; r++) {
                HeapTriage hh = new HeapTriage(); BucketTriage bb = new BucketTriage(0);
                long t0 = System.nanoTime();
                if (d == 0) for (int i = 0; i < n; i++) hh.admit("P", sev[i]); else for (int i = 0; i < n; i++) bb.admit("P", sev[i]);
                adm += (System.nanoTime() - t0) / (double) n;
                t0 = System.nanoTime(); List<Patient> l = d == 0 ? hh.waitingList() : bb.waitingList(); dsp += (System.nanoTime() - t0) / 1e6; sink += l.size();
                t0 = System.nanoTime();
                if (d == 0) while (hh.next() != null) sink++; else while (bb.next() != null) sink++;
                srv += (System.nanoTime() - t0) / (double) n;
            }
            if (pass == 1) System.out.printf("%-10d %-14s %14.1f %14.1f %16.2f%n", n, d == 0 ? "PriorityQueue" : "5 FIFO buckets", adm / R, srv / R, dsp / R);
        }

        System.out.println("\n=== Starvation: one S5 patient (Asha) waits while 2 critical (S1) patients arrive for every 1 served ===");
        System.out.println("steps simulated: 100000");
        HeapTriage starve = new HeapTriage(); starve.admit("Asha", 5);
        int servedAt = -1;
        for (int step = 1; step <= 100_000 && servedAt < 0; step++) { starve.admit("crit" + step, 1); starve.admit("crit" + step + "b", 1); Patient x = starve.next(); if (x.name.equals("Asha")) servedAt = step; }
        System.out.println("plain priority rule : Asha served at step " + (servedAt < 0 ? "NEVER (still waiting after 100000 steps; " + starve.size() + " patients queued)" : servedAt));
        for (long T : new long[]{50, 500}) {
            BucketTriage fair = new BucketTriage(T); fair.admit("Asha", 5); int at = -1;
            for (int step = 1; step <= 100_000 && at < 0; step++) { fair.admit("crit" + step, 1); fair.admit("crit" + step + "b", 1); Patient x = fair.next(); if (x.name.equals("Asha")) at = step; }
            System.out.printf("aging rule, +1 level per %d arrivals waited : Asha served at step %d (waited %d arrivals)%n", T, at, 2L * at);
        }
        System.out.println("\nDeployment: 5,000 patients/day = 5,000 admits + 5,000 serves. At the measured cost per operation this is a tiny fraction of a millisecond of CPU per day.");
        System.out.println("(sink=" + sink + ")");
    }
    static long sink;
    static String join(List<Patient> l) { StringBuilder s = new StringBuilder(); for (Patient p : l) s.append(p).append(' '); return s.toString().trim(); }
}
