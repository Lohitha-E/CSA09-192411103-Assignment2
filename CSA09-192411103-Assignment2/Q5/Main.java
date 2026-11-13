import java.util.*;


class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) {
        this.name = name; this.points = points; this.goalDiff = goalDiff;
    }
    @Override public String toString() {
        return name + "(" + points + "," + goalDiff + ")";
    }
}

class Leaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;
    Leaderboard(Comparator<? super T> order) { this.order = order; }
    void add(T item) { items.add(item); }
    List<T> ranking() {
        // TODO 1 (done): sorted COPY using 'order'; 'items' itself is never modified
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return copy;
    }
}

public class Main {
    public static void main(String[] args) {
        // TODO 2 (done): points (high first), then goal difference (high first), then name (A-Z)
        Comparator<Team> byRank = Comparator.comparingInt((Team t) -> t.points).reversed()
                .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                .thenComparing(t -> t.name);
        Leaderboard<Team> board = new Leaderboard<>(byRank);
        board.add(new Team("Panthers", 9, 4));
        board.add(new Team("Eagles", 9, 6));
        board.add(new Team("Cobras", 6, 1));
        board.add(new Team("Bisons", 9, 4));
        System.out.println(board.ranking());
        System.out.println();
        Experiments.main(args);
    }
}


/** Q5 - generic ranking design, faulty comparators, Comparable alternative, triple stable sort. */
class Experiments {
    static final class Team implements Comparable<Team> {
        final String name; final int points; final int goalDiff;
        Team(String n, int p, int g) { name = n; points = p; goalDiff = g; }
        @Override public String toString() { return name + "(" + points + "," + goalDiff + ")"; }
        /** Comparable alternative: ONE natural order baked into the class. */
        @Override public int compareTo(Team o) {
            int c = Integer.compare(o.points, points);
            if (c != 0) return c;
            c = Integer.compare(o.goalDiff, goalDiff);
            return c != 0 ? c : name.compareTo(o.name);
        }
    }

    /** Generic reusable component: works for any T, any Comparator that can compare T (or a supertype). */
    static final class Leaderboard<T> {
        private final List<T> items = new ArrayList<>();
        private final Comparator<? super T> order;
        Leaderboard(Comparator<? super T> order) { this.order = order; }
        void add(T t) { items.add(t); }
        void addAll(Collection<? extends T> more) { items.addAll(more); }          // wildcard: producer
        List<T> ranking() { List<T> c = new ArrayList<>(items); c.sort(order); return c; }
        List<T> top(int k) { return ranking().subList(0, Math.min(k, items.size())); }
    }

    /** Bounded type: needs no Comparator at all when T has a natural order. */
    static <T extends Comparable<? super T>> List<T> rankNatural(Collection<? extends T> in) {
        List<T> l = new ArrayList<>(in); Collections.sort(l); return l;
    }

    // ---- correct comparator ----
    static final Comparator<Team> BY_RANK = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
            .thenComparing(t -> t.name);

    // ---- deliberately faulty comparators ----
    /** F1: compares by subtraction -> int overflow breaks transitivity for extreme values. */
    static final Comparator<Team> FAULTY_SUBTRACTION = (a, b) -> {
        int c = b.points - a.points;
        if (c != 0) return c;
        c = b.goalDiff - a.goalDiff;
        return c != 0 ? c : a.name.compareTo(b.name);
    };
    /** F2: forgets the name tie-breaker -> items that should be ordered compare as equal; result depends on input order. */
    static final Comparator<Team> FAULTY_NO_TIEBREAK = Comparator.comparingInt((Team t) -> t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
    /** F3: "close" points (difference <= 1) treated as equal -> NOT transitive. */
    static final Comparator<Team> FAULTY_FUZZY = (a, b) -> Math.abs(a.points - b.points) <= 1 ? a.name.compareTo(b.name) : Integer.compare(b.points, a.points);

    public static void main(String[] args) {
        System.out.println("=== Data set 1: several ties at every level ===");
        List<Team> d1 = List.of(new Team("Panthers", 9, 4), new Team("Eagles", 9, 6), new Team("Cobras", 6, 1), new Team("Bisons", 9, 4),
                new Team("Falcons", 6, 1), new Team("Aces", 6, 3), new Team("Dingoes", 3, 0), new Team("Zebras", 3, 0), new Team("Ants", 9, 6));
        Leaderboard<Team> lb = new Leaderboard<>(BY_RANK); lb.addAll(d1);
        System.out.println("ranking : " + lb.ranking());
        System.out.println("top 3   : " + lb.top(3));
        System.out.println("expected: [Ants(9,6), Eagles(9,6), Bisons(9,4), Panthers(9,4), Aces(6,3), Cobras(6,1), Falcons(6,1), Dingoes(3,0), Zebras(3,0)]");

        System.out.println("\n=== Order-independence: 2000 random shuffles of data set 1 ===");
        for (Object[] c : new Object[][]{{"BY_RANK (correct)", BY_RANK}, {"FAULTY_NO_TIEBREAK", FAULTY_NO_TIEBREAK}}) {
            @SuppressWarnings("unchecked") Comparator<Team> cmp = (Comparator<Team>) c[1];
            Set<String> distinct = new HashSet<>(); Random r = new Random(5);
            for (int i = 0; i < 2000; i++) { List<Team> l = new ArrayList<>(d1); Collections.shuffle(l, r); l.sort(cmp); distinct.add(l.toString()); }
            System.out.printf("%-20s -> %d distinct rankings produced%n", c[0], distinct.size());
        }

        System.out.println("\n=== Data set 2: built to break faulty comparators ===");
        Team a = new Team("A", 5, 0), b = new Team("B", 5, 1_500_000_000), cT = new Team("C", 5, -1_500_000_000);
        System.out.println("A=" + a + " B=" + b + " C=" + cT + "  (goal differences chosen to overflow subtraction)");
        for (Object[] c : new Object[][]{{"correct", BY_RANK}, {"subtraction", FAULTY_SUBTRACTION}}) {
            @SuppressWarnings("unchecked") Comparator<Team> cmp = (Comparator<Team>) c[1];
            System.out.printf("%-12s compare(A,B)=%d compare(B,C)=%d compare(A,C)=%d  -> %s   ranking: %s%n", c[0],
                    Integer.signum(cmp.compare(a, b)), Integer.signum(cmp.compare(b, cT)), Integer.signum(cmp.compare(a, cT)),
                    allTransitive(cmp, a, b, cT) ? "transitive" : "NOT transitive (cycle)", sortedCopy(cmp, a, b, cT));
        }
        System.out.println("\n=== Property check on 3 random teams x 200000 triples ===");
        Random r = new Random(11); List<Team> pool = new ArrayList<>();
        for (int i = 0; i < 40; i++) pool.add(new Team("T" + i, r.nextInt(12), r.nextInt(5) - 2));
        for (Object[] c : new Object[][]{{"BY_RANK", BY_RANK}, {"FAULTY_NO_TIEBREAK", FAULTY_NO_TIEBREAK}, {"FAULTY_FUZZY", FAULTY_FUZZY}}) {
            @SuppressWarnings("unchecked") Comparator<Team> cmp = (Comparator<Team>) c[1];
            int bad = 0, sym = 0, eqDistinct = 0;
            for (int i = 0; i < 200_000; i++) {
                Team x = pool.get(r.nextInt(40)), y = pool.get(r.nextInt(40)), z = pool.get(r.nextInt(40));
                if (!transitiveOK(cmp, x, y, z)) bad++;
                if (Integer.signum(cmp.compare(x, y)) != -Integer.signum(cmp.compare(y, x))) sym++;
                if (x != y && cmp.compare(x, y) == 0 && !x.name.equals(y.name)) eqDistinct++;
            }
            System.out.printf("%-20s transitivity violations=%d  antisymmetry violations=%d  'equal' but different teams=%d%n", c[0], bad, sym, eqDistinct);
        }
        System.out.println("\n=== FAULTY_FUZZY on a big list: does List.sort detect the broken contract? ===");
        List<Team> big = new ArrayList<>(); Random rr = new Random(2);
        for (int i = 0; i < 5000; i++) big.add(new Team("T" + i, rr.nextInt(40), 0));
        try { big.sort(FAULTY_FUZZY); System.out.println("sorted without exception (but the result is not a valid ranking)"); }
        catch (IllegalArgumentException e) { System.out.println("IllegalArgumentException: " + e.getMessage()); }

        System.out.println("\n=== Alternatives: Comparable.compareTo and three stable sorts ===");
        List<Team> viaComparable = rankNatural(d1);
        List<Team> viaStable = new ArrayList<>(d1);
        viaStable.sort(Comparator.comparing((Team t) -> t.name));                                   // least significant first
        viaStable.sort(Comparator.comparingInt((Team t) -> t.goalDiff).reversed());
        viaStable.sort(Comparator.comparingInt((Team t) -> t.points).reversed());                    // most significant last
        System.out.println("Comparable result == Comparator-chain result : " + viaComparable.equals(lb.ranking()));
        System.out.println("3 stable sorts == Comparator-chain result     : " + viaStable.equals(lb.ranking()));

        int n = 400_000; Random q = new Random(9); List<Team> base = new ArrayList<>();
        for (int i = 0; i < n; i++) base.add(new Team("Team" + q.nextInt(1_000_000), q.nextInt(30), q.nextInt(21) - 10));
        double[] t = new double[3];
        for (int run = 0; run < 6; run++) {
            List<Team> l1 = new ArrayList<>(base); long t0 = System.nanoTime(); l1.sort(BY_RANK); double e1 = (System.nanoTime() - t0) / 1e6;
            List<Team> l2 = new ArrayList<>(base); t0 = System.nanoTime(); Collections.sort(l2); double e2 = (System.nanoTime() - t0) / 1e6;
            List<Team> l3 = new ArrayList<>(base); t0 = System.nanoTime();
            l3.sort(Comparator.comparing((Team x) -> x.name)); l3.sort(Comparator.comparingInt((Team x) -> x.goalDiff).reversed()); l3.sort(Comparator.comparingInt((Team x) -> x.points).reversed());
            double e3 = (System.nanoTime() - t0) / 1e6;
            if (run >= 1) { t[0] += e1; t[1] += e2; t[2] += e3; }
        }
        System.out.printf("n=%d teams, avg of 5 runs (ms): Comparator chain=%.1f  Comparable=%.1f  three stable sorts=%.1f%n", n, t[0] / 5, t[1] / 5, t[2] / 5);

        System.out.println("\n=== Reuse: the SAME Leaderboard<T> ranks players, colleges, quiz teams ===");
        record Player(String name, double avg) {}
        Leaderboard<Player> pl = new Leaderboard<>(Comparator.comparingDouble(Player::avg).reversed().thenComparing(Player::name));
        pl.add(new Player("Rhea", 41.5)); pl.add(new Player("Omar", 55.0)); pl.add(new Player("Ian", 41.5));
        System.out.println(pl.ranking());
        Leaderboard<String> colleges = new Leaderboard<>(Comparator.<Object, String>comparing(Object::toString).reversed());  // Comparator<Object> is accepted because of '? super T'
        colleges.add("SIMATS"); colleges.add("Anna Univ"); colleges.add("VIT");
        System.out.println(colleges.ranking());
    }

    static boolean allTransitive(Comparator<Team> c, Team x, Team y, Team z) {
        Team[] t = {x, y, z};
        for (Team p : t) for (Team q : t) for (Team w : t) if (!transitiveOK(c, p, q, w)) return false;
        return true;
    }
    static List<Team> sortedCopy(Comparator<Team> c, Team... t) { List<Team> l = new ArrayList<>(List.of(t)); l.sort(c); return l; }

    static boolean transitiveOK(Comparator<Team> c, Team x, Team y, Team z) {
        // if x<=y and y<=z then x<=z must hold
        return !(c.compare(x, y) <= 0 && c.compare(y, z) <= 0 && c.compare(x, z) > 0);
    }
}
