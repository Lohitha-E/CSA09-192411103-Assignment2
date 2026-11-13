# Q3 – Screenshots

**Task:** hashCode–equals contract (Attendance + experiments)

## How to capture

1. Open `Q3/Main.java` in VS Code (the source code must be visible in the editor).
2. Open the integrated terminal (Terminal → New Terminal) and run:
   ```bash
   cd Q3
   javac Main.java
   java Main
   ```
3. Take a **genuine** screenshot showing the source code in the editor **and** the execution output in the terminal. Do not edit or fabricate screenshots.
4. If the output is longer than the terminal window, take one screenshot per case below (scroll the terminal).

## Cases to capture

- [ ] Case 1 – Complete the Code  →  save as `case1.png`
- [ ] Case 2 – Test 1: HashSet appears to work (same OBJECT scanned twice)  →  save as `case2.png`
- [ ] Case 3 – Test 2: duplicates exposed (two different objects, same regNo)  →  save as `case3.png`
- [ ] Case 4 – Test 3: key field changed after insertion  →  save as `case4.png`
- [ ] Case 5 – Test 4: TreeSet whose compareTo() is inconsistent with equals()  →  save as `case5.png`
- [ ] Case 6 – Alternatives: TreeSet(Comparator on regNo) and HashMap<regNo, Student>  →  save as `case6.png`
- [ ] Case 7 – Timing: 400,000 scans of 100,000 unique students (avg of 5 runs, ms)  →  save as `case7.png`

Also save one full-run capture as `../OUTPUT.PNG`.

## Expected key results

- Before TODO 1: `Unique students: 3`
- After TODO 1: `Unique students: 2`

## Notes

Timing values vary from run to run and machine to machine; use the values from **your own** run in the report.
