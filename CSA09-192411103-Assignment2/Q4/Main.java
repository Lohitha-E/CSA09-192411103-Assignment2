import java.io.*;
import java.util.*;

/** Q4 - HashMap vs TreeMap vs LinkedHashMap (+Hashtable, + sorted array with binarySearch) for pharmacy stock. */
public class Main {
    static final class Medicine {
        final String code; final String name; int stock;
        Medicine(String c, String n, int s) { code = c; name = n; stock = s; }
    }
    static long sink;

    static String code(int id) { return String.format("MED%07d", id); }

    /** codes in random (arrival) order */
    static String[] arrivalOrder(int n) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) ids.add(i * 3 + 1);
        Collections.shuffle(ids, new Random(7));
        String[] c = new String[n];
        for (int i = 0; i < n; i++) c[i] = code(ids.get(i));
        return c;
    }

    static boolean isSorted(List<String> l) { for (int i = 1; i < l.size(); i++) if (l.get(i - 1).compareTo(l.get(i)) > 0) return false; return true; }

    public static void main(String[] args) throws Exception {
        new java.io.File("results").mkdirs();
        int[] sizes = {1_000, 10_000, 100_000, 500_000};
        PrintWriter csv = new PrintWriter("results/raw_q4.csv");
        csv.println("n,structure,run,put_ms,lookup100k_ms,codeOrderReport_ms,insertionOrderReport_ms,codeOrderCorrect,insertionOrderCorrect");
        for (int pass = 0; pass < 2; pass++) {
            boolean report = pass == 1;
            if (report) System.out.printf("%-9s %-14s %9s %14s %16s %16s  %s%n", "n", "map", "put ms", "100k get ms", "code-order rpt", "insert-order rpt", "orders correct (code/insert)");
            for (int n : sizes) {
                String[] arr = arrivalOrder(n);
                String[] sortedCodes = arr.clone(); Arrays.sort(sortedCodes);
                Random rnd = new Random(3);
                String[] q = new String[100_000];
                for (int i = 0; i < q.length; i++) q[i] = arr[rnd.nextInt(n)];
                String[] names = {"HashMap", "TreeMap", "LinkedHashMap", "Hashtable"};
                for (int m = 0; m < names.length; m++) {
                    double put = 0, get = 0, rc = 0, ri = 0; boolean okC = true, okI = true;
                    int RUNS = report ? 5 : 1;
                    for (int run = 0; run < RUNS; run++) {
                        Map<String, Medicine> map = m == 0 ? new HashMap<>() : m == 1 ? new TreeMap<>() : m == 2 ? new LinkedHashMap<>() : new Hashtable<>();
                        List<String> insertionLog = new ArrayList<>(n);          // extra structure needed by the "wrong" maps
                        long t0 = System.nanoTime();
                        for (String c : arr) { map.put(c, new Medicine(c, "med", 100)); insertionLog.add(c); }
                        double tPut = (System.nanoTime() - t0) / 1e6;
                        t0 = System.nanoTime();
                        long sum = 0; for (String c : q) sum += map.get(c).stock;
                        sink += sum; double tGet = (System.nanoTime() - t0) / 1e6;

                        // Report 1: all medicines in CODE order
                        t0 = System.nanoTime();
                        List<String> codeReport = new ArrayList<>(n);
                        if (m == 1) { for (Map.Entry<String, Medicine> e : map.entrySet()) codeReport.add(e.getKey()); }       // natural
                        else { List<String> keys = new ArrayList<>(map.keySet()); Collections.sort(keys); codeReport = keys; }  // extra O(n log n)
                        double tC = (System.nanoTime() - t0) / 1e6;
                        // Report 2: medicines in ORDER ADDED
                        t0 = System.nanoTime();
                        List<String> insReport = new ArrayList<>(n);
                        if (m == 2) { for (String k : map.keySet()) insReport.add(k); }                                       // natural
                        else { for (String k : insertionLog) insReport.add(map.get(k).code); }                                 // extra log + n lookups
                        double tI = (System.nanoTime() - t0) / 1e6;
                        okC = codeReport.equals(Arrays.asList(sortedCodes));
                        okI = insReport.equals(Arrays.asList(arr));
                        if (report) csv.printf("%d,%s,%d,%.2f,%.2f,%.2f,%.2f,%b,%b%n", n, names[m], run + 1, tPut, tGet, tC, tI, okC, okI);
                        put += tPut; get += tGet; rc += tC; ri += tI;
                    }
                    if (report) System.out.printf("%-9d %-14s %9.1f %14.2f %16.2f %16.2f  %b / %b%n", n, names[m], put / RUNS, get / RUNS, rc / RUNS, ri / RUNS, okC, okI);
                }
                // Alternative: sorted array + Arrays.binarySearch
                if (report) {
                    Medicine[] arrM = new Medicine[n];
                    for (int i = 0; i < n; i++) arrM[i] = new Medicine(sortedCodes[i], "med", 100);
                    Comparator<Medicine> cmp = Comparator.comparing(x -> x.code);
                    double lk = 0;
                    for (int run = 0; run < 5; run++) {
                        long t0 = System.nanoTime(); long sum = 0;
                        for (String c : q) { int idx = Arrays.binarySearch(arrM, new Medicine(c, "", 0), cmp); sum += arrM[idx].stock; }
                        sink += sum; lk += (System.nanoTime() - t0) / 1e6;
                    }
                    // insertion into sorted array: 1000 inserts (binarySearch + arraycopy shift)
                    Medicine[] grow = Arrays.copyOf(arrM, n + 1000); int size = n;
                    long t0 = System.nanoTime();
                    for (int i = 0; i < 1000; i++) {
                        Medicine nm = new Medicine(code(3 * (i + n) + 2), "new", 1);
                        int pos = -Arrays.binarySearch(grow, 0, size, nm, cmp) - 1;
                        System.arraycopy(grow, pos, grow, pos + 1, size - pos); grow[pos] = nm; size++;
                    }
                    double ins = (System.nanoTime() - t0) / 1e6;
                    System.out.printf("%-9d %-14s %9s %14.2f %16s %16s  insert 1000 new codes = %.2f ms%n", n, "sorted array", "-", lk / 5, "O(n) scan", "n/a", ins);
                    csv.printf("%d,SortedArray,-,-,%.2f,-,-,-,-%n", n, lk / 5);
                }
            }
        }
        csv.close();
        System.out.println("(sink=" + sink + ")");
    }
}
