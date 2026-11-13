# Programming in Java – Assignment 2 (CO2)

**Student:** E. Lohitha  
**Register Number:** 192411103  
**Repository folder:** `CSA09-192411103-Assignment2`  
**Java used for local verification:** OpenJDK 21 (Java 17+ is enough)

## Contents

- `Q1` – Railway e-ticket text building: `+` vs `StringBuilder` vs `StringBuffer` (plus `String.join`, `StringJoiner`, `String.format`), thread-safety demo
- `Q2` – Playlist editor: `ArrayList` vs `LinkedList` vs `ArrayDeque`, "Up Next" condition, manual array
- `Q3` – `hashCode`–`equals` contract: Complete the Code (before/after TODO 1) and designed experiments
- `Q4` – Pharmacy stock: `HashMap` vs `TreeMap` vs `LinkedHashMap` (+ `Hashtable`, sorted array with `binarySearch`)
- `Q5` – Generic leaderboard: `Comparator` chain, `Leaderboard<T>`, faulty comparators, `Comparable` alternative
- `Q6` – Emergency triage with `PriorityQueue`: hand check, growth, starvation and aging rule
- `Q7` – Day-4 design challenge: redesign of the Q6 waiting-list display (five FIFO queues) and validation

Each question folder contains `Main.java`, `OUTPUT.txt` and a `Screenshots` folder. Each `Screenshots` folder has a `README.md` listing the test cases to capture, and Q1, Q2, Q4 and Q7 also keep their raw per-run CSV data in `results/`.

## Compile and run

For each question, run the following commands from that question's folder:

```bash
javac Main.java
java Main
```
Q4, Q6 and Q7 use large data sizes; run them with `java -Xmx2g Main`. Q1, Q2, Q4 and Q7 also save raw per-run CSV data into a `results` folder that they create when run.

**Running Java Programs in Visual Studio Code (VS Code)**

1. Open Visual Studio Code.
2. Select File → Open Folder and open the extracted assignment folder.
3. Navigate to the required question folder (Q1, Q2, Q3, etc.).
4. Open that question's `Main.java` file.
5. Make sure the Extension Pack for Java is installed and a JDK is configured.
6. Open the integrated terminal using Terminal → New Terminal.
7. Navigate to the required question folder. For example: `cd Q1`
8. Compile the Java program: `javac Main.java`
9. Run the program: `java Main`

Verify that the program executes successfully and displays the expected output in the VS Code integrated terminal.
Capture your own genuine screenshot showing the Java source code in the editor and the actual execution output in the integrated terminal.

Note: Repeat these steps for Q1 to Q7, changing the question folder accordingly. Save each screenshot (and `OUTPUT.PNG`) in its corresponding question's `Screenshots` folder. Do not use fabricated or edited execution screenshots.

## Task results

| Task | Result / verification |
|---|---|
| Q1 | For long tickets (20 × 3,000 fields) `+` is ~100× slower and allocates ~440× more than `StringBuilder`; short tickets show no significant difference. Shared `StringBuilder` lost appends / threw `ArrayIndexOutOfBoundsException` in every trial; `StringBuffer` stayed correct. |
| Q2 | `ArrayList` is best for append, `get(k)` and iteration; poor at front inserts/removals. For the "Up Next" queue `ArrayDeque` is ~250× faster than `ArrayList`. |
| Q3 | Without `hashCode()` the Attendance program prints `Unique students: 3`; with TODO 1 completed it prints `2`. Same-object scans, equal-but-distinct scans, mutated key and inconsistent `TreeSet` tests all match the predictions. |
| Q4 | Three map sizes (up to 500,000). `HashMap`/`LinkedHashMap` give the fastest lookups; `TreeMap` produces the code-order report naturally; `LinkedHashMap` produces the insertion-order report naturally. |
| Q5 | Complete the Code output `[Eagles(9,6), Bisons(9,4), Panthers(9,4), Cobras(6,1)]` matches. Subtraction comparator creates a cycle; a comparator without the name tie-breaker gives 16 different rankings of the same data. |
| Q6 | Output `Meena(S1) Kumar(S1) Lata(S2) Ravi(S3) John(S3) Asha(S5)` matches. 8-patient hand check passes. A severity-5 patient is never served in 100,000 steps without aging; with the aging rule they are served. |
| Q7 | Five FIFO queues replace heap copy-and-drain for the waiting-list display: about 250× faster for 20 refreshes at n = 100,000, identical service order on 50,000 random patients. |

## Screenshot note

Each question folder contains `OUTPUT.PNG`: an image of the console output **rendered from the recorded `OUTPUT.txt`** (it carries a label saying so). It is **not** a VS Code screenshot. The `Screenshots` folders hold a `README.md` listing the cases to capture. If your faculty requires genuine VS Code screenshots, capture your own and replace `OUTPUT.PNG`.

**Important:** Timing values and thread-dependent results in `OUTPUT.txt` are measurements from one local run (1 CPU core). They vary from run to run, so a fresh run on your machine will not match `OUTPUT.txt` line by line for timing numbers. Counts, ordering results and pass/fail checks are deterministic (for example Q3 prints 3 then 2, Q5 and Q6 print their expected rankings). Use your own measured values in the report.

## Submission checklist

- [ ] Add the GitHub repository URL to the cover page and submit it to faculty.
- [ ] Capture your own screenshots and `OUTPUT.PNG` for Q1–Q7.
- [ ] Replace the timing values in your report with your own measurements.
- [ ] Commit each task progressively (at least one commit per task); this ZIP alone does not create GitHub commits.
