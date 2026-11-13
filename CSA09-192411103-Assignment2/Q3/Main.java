import java.util.*;

/** Q3 - hashCode-equals contract. Part 1: Complete the Code (before/after TODO 1). Part 2: designed experiments. */
class Student {
    private final String regNo;
    private final String name;
    Student(String regNo, String name) { this.regNo = regNo; this.name = name; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        return regNo.equals(((Student) o).regNo);
    }
    // TODO 1 (done): consistent with equals() - same field
    @Override public int hashCode() { return regNo.hashCode(); }
    @Override public String toString() { return regNo + " " + name; }
}

/** Same class exactly as given, WITHOUT hashCode (TODO 1 not completed) - used to record the "before" output. */
class StudentBefore {
    private final String regNo;
    private final String name;
    StudentBefore(String regNo, String name) { this.regNo = regNo; this.name = name; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentBefore)) return false;
        return regNo.equals(((StudentBefore) o).regNo);
    }
    @Override public String toString() { return regNo + " " + name; }
}

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Complete the Code ===");
        Set<StudentBefore> before = new HashSet<>();
        before.add(new StudentBefore("192311001", "Arun"));
        before.add(new StudentBefore("192311002", "Divya"));
        before.add(new StudentBefore("192311001", "Arun"));
        System.out.println("BEFORE TODO 1 (no hashCode)  -> Unique students: " + before.size());
        Set<Student> present = new HashSet<>();
        present.add(new Student("192311001", "Arun"));   // scanned at Gate 1
        present.add(new Student("192311002", "Divya"));
        present.add(new Student("192311001", "Arun"));   // scanned again at Gate 3
        System.out.println("AFTER  TODO 1 (hashCode done) -> Unique students: " + present.size());
        System.out.println();
        Experiments.main(args);
    }
}


/** Q3 - three deliberately designed tests + alternatives (TreeSet with Comparator, HashMap keyed by regNo). */
class Experiments {
    /** equals() overridden, hashCode() NOT overridden (the junior developer's class). */
    static class BadStudent {
        final String regNo; final String name;
        BadStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof BadStudent b && regNo.equals(b.regNo); }
        @Override public String toString() { return regNo + " " + name; }
    }
    /** corrected: immutable key, hashCode consistent with equals. */
    static class GoodStudent {
        final String regNo; final String name;
        GoodStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof GoodStudent g && regNo.equals(g.regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
        @Override public String toString() { return regNo + " " + name; }
    }
    /** hashCode consistent with equals, but the key field is MUTABLE. */
    static class MutableStudent {
        String regNo; final String name;
        MutableStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof MutableStudent m && regNo.equals(m.regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
    }
    /** compareTo uses NAME, equals uses REGNO -> inconsistent with equals. */
    static class CmpStudent implements Comparable<CmpStudent> {
        final String regNo; final String name;
        CmpStudent(String r, String n) { regNo = r; name = n; }
        @Override public boolean equals(Object o) { return o instanceof CmpStudent c && regNo.equals(c.regNo); }
        @Override public int hashCode() { return regNo.hashCode(); }
        @Override public int compareTo(CmpStudent o) { return name.compareTo(o.name); }
        @Override public String toString() { return regNo + " " + name; }
    }

    static void report(String test, int predicted, int observed, String why) {
        System.out.printf("%-58s predicted=%d observed=%d  %s%n", test, predicted, observed, observed == predicted ? "[prediction OK]" : "[prediction WRONG]");
        System.out.println("    reason: " + why);
    }

    public static void main(String[] args) {
        System.out.println("=== Test 1: HashSet appears to work (same OBJECT scanned twice) ===");
        BadStudent arun = new BadStudent("192311001", "Arun");
        Set<BadStudent> s1 = new HashSet<>();
        s1.add(arun); s1.add(new BadStudent("192311002", "Divya")); s1.add(arun);
        report("T1 same reference added twice (no hashCode)", 2, s1.size(), "identity hashCode is identical for the same object, so it lands in the same bucket and equals() (== true) rejects it.");

        System.out.println("\n=== Test 2: duplicates exposed (two different objects, same regNo) ===");
        Set<BadStudent> s2 = new HashSet<>();
        s2.add(new BadStudent("192311001", "Arun")); s2.add(new BadStudent("192311002", "Divya")); s2.add(new BadStudent("192311001", "Arun"));
        report("T2 equal-but-distinct objects, no hashCode", 3, s2.size(), "Object.hashCode() differs for the two Arun objects -> different buckets -> equals() is never consulted.");
        System.out.println("    contents: " + s2);
        Set<GoodStudent> s2b = new HashSet<>();
        s2b.add(new GoodStudent("192311001", "Arun")); s2b.add(new GoodStudent("192311002", "Divya")); s2b.add(new GoodStudent("192311001", "Arun"));
        report("T2b same scans, hashCode overridden (corrected)", 2, s2b.size(), "equal objects now share a hash code -> same bucket -> equals() detects the duplicate.");

        System.out.println("\n=== Test 3: key field changed after insertion ===");
        MutableStudent m = new MutableStudent("192311001", "Arun");
        Set<MutableStudent> s3 = new HashSet<>();
        s3.add(m);
        m.regNo = "192311999";
        boolean found = s3.contains(m);
        boolean removed = s3.remove(m);
        System.out.println("    contains(m) after mutation = " + found + ", remove(m) = " + removed + ", size = " + s3.size());
        report("T3 mutate regNo after add -> set size (element is 'lost')", 1, s3.size(), "object stays in its OLD bucket; lookups use the NEW hash, so contains/remove miss it, but it is still counted.");
        s3.add(new MutableStudent("192311999", "Arun"));
        System.out.println("    after adding an 'equal' student again, size = " + s3.size() + " (duplicate now accepted)");

        System.out.println("\n=== Test 4: TreeSet whose compareTo() is inconsistent with equals() ===");
        Set<CmpStudent> t = new TreeSet<>();
        t.add(new CmpStudent("192311001", "Arun"));
        t.add(new CmpStudent("192311001", "Arun K"));   // equals() says same student, compareTo says different
        t.add(new CmpStudent("192311002", "Arun"));     // equals() says different student, compareTo says same
        report("T4 TreeSet(compareTo by name) with 3 scans of 2 students", 2, t.size(), "TreeSet ignores equals(): it uses compareTo (name) only. The SIZE equals 2 by coincidence, but the MEMBERS are wrong: 192311001 is stored twice (Arun / Arun K) and student 192311002 was rejected as a 'duplicate' of 192311001 because both are named Arun.");
        System.out.println("    contents: " + t);

        System.out.println("\n=== Alternatives: TreeSet(Comparator on regNo) and HashMap<regNo, Student> ===");
        TreeSet<BadStudent> ts = new TreeSet<>(Comparator.comparing(x -> x.regNo));
        Map<String, BadStudent> hm = new HashMap<>();
        for (BadStudent b : List.of(new BadStudent("192311001", "Arun"), new BadStudent("192311002", "Divya"), new BadStudent("192311001", "Arun"))) {
            ts.add(b); hm.putIfAbsent(b.regNo, b);
        }
        System.out.println("TreeSet(Comparator on regNo) size = " + ts.size() + ", HashMap<String,Student> size = " + hm.size());

        System.out.println("\n=== Timing: 400,000 scans of 100,000 unique students (avg of 5 runs, ms) ===");
        int uniq = 100_000, scans = 400_000;
        Random r = new Random(1);
        String[] ids = new String[scans];
        for (int i = 0; i < scans; i++) ids[i] = String.format("19%07d", r.nextInt(uniq));
        for (int rep = 0; rep < 3; rep++) timing(ids, false);
        timing(ids, true);
    }

    static void timing(String[] ids, boolean print) {
        double hs = 0, ts = 0, hm = 0; int sizes = 0;
        for (int run = 0; run < 5; run++) {
            long t0 = System.nanoTime();
            Set<GoodStudent> a = new HashSet<>(); for (String id : ids) a.add(new GoodStudent(id, "x"));
            hs += (System.nanoTime() - t0) / 1e6; sizes += a.size();
            t0 = System.nanoTime();
            TreeSet<GoodStudent> b = new TreeSet<>(Comparator.comparing(x -> x.regNo)); for (String id : ids) b.add(new GoodStudent(id, "x"));
            ts += (System.nanoTime() - t0) / 1e6; sizes += b.size();
            t0 = System.nanoTime();
            Map<String, GoodStudent> c = new HashMap<>(); for (String id : ids) c.putIfAbsent(id, new GoodStudent(id, "x"));
            hm += (System.nanoTime() - t0) / 1e6; sizes += c.size();
        }
        if (print) System.out.printf("HashSet<Student> %.1f   TreeSet<Student>(Comparator) %.1f   HashMap<String,Student> %.1f   (sizes check: %d)%n", hs / 5, ts / 5, hm / 5, sizes / 15);
    }
}
