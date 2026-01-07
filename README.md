# ICS4U1 Simulation Summative — Robot Tag (VIP / Guard / Chaser)

This repository contains our **ICS4U1 Simulation Summative** project: a turn-based robot game simulation running inside a **fenced arena (max 13 × 24)**. :contentReference[oaicite:2]{index=2}  
The project demonstrates **object-oriented programming, records, sorting (non-bubble), and (optionally) recursion**, and follows the software development process (planning → design → coding → documentation → debugging). :contentReference[oaicite:3]{index=3}

---

## Team & Responsibilities
- **Xinran Li** — Guard AI (defensive positioning + threat scoring), shared BaseBot / OOP structure, testing for Guard
- **Austin Xiong** — VIP AI (escape / survival strategy), testing for VIP
- **Aadil Kureshy** — Chaser AI (pursuit / catch strategy), testing for Chaser

> The summative is completed in groups of three, with individual robot AI work by each member. :contentReference[oaicite:4]{index=4}

---

## Game Overview (Our Rules)
**Roles**
- **VIP**: must survive until the time limit.
- **Guard**: protects the VIP by escorting and intercepting threats.
- **Chaser**: attempts to catch the VIP (or win condition defined below).

**Arena**
- Fenced field no larger than **13 × 24**. :contentReference[oaicite:5]{index=5}

**Turn-Based Simulation**
- Robots do **not** move simultaneously.
- The **Application** controls turn order and handles all communication between players. :contentReference[oaicite:6]{index=6}

**Win / Lose**
- Chaser wins if it catches the required target(s) within the time limit.
- VIP/Guard win if the time limit expires without the Chaser achieving its catch condition, or if all Chasers are "caught".

> Note: Our exact win condition (e.g., “Chaser must catch both robots” vs “Chaser must catch VIP”) is defined in the `Application` rules configuration.

---

## Project Requirements Checklist (Aligned to Rubric)

### Level 3 Core Requirements
- ✅ **Application class** directs the simulation (turn order + communication). :contentReference[oaicite:7]{index=7}  
- ✅ **Multiple player types** (VIP / Guard / Chaser), each member builds a different AI role. :contentReference[oaicite:8]{index=8}  
- ✅ Each player maintains a **sorted list (NOT bubble sort)** to decide priorities using **distance + movement speed**. :contentReference[oaicite:9]{index=9}  
- ✅ Use **Java records** to represent player info; the Application sends these records to each robot each turn. :contentReference[oaicite:10]{index=10}  
- ✅ Strong use of **inheritance, polymorphism, and abstract classes**. :contentReference[oaicite:11]{index=11}  
- ✅ Code is fully commented and matches our UML/design. :contentReference[oaicite:12]{index=12}  

### Level 4 Enhancements
- ✅ “Chance” system: each player has randomized **dodging/catching** ability; catch results depend on those attributes. :contentReference[oaicite:13]{index=13}  
- ✅ Robots incorporate this into decision-making **without being directly given opponent attributes**; opponents are **learned over time** through observation. :contentReference[oaicite:14]{index=14}  
- ✅ Robots can **change strategy** during the battle (e.g., intercept ↔ escort ↔ retreat/regenerate). :contentReference[oaicite:15]{index=15}  

---

## AI Summary

### Guard AI (Xinran)
Guard uses a **sorted list of candidate defensive positions** each turn (selection/insertion sort) and chooses the best move based on:
- VIP safety (distance VIP↔Chaser),
- interception potential (position between VIP and threat),
- chaser speed,
- learned risk (estimated opponent catch success over time).

Guard strategies:
- **Escort** when threats are far (maintain safe distance from VIP).
- **Intercept** when threat is near (block/pressure chaser).
- **Reposition/Retreat** when necessary (avoid being trapped / low HP).

### VIP AI
VIP prioritizes survival:
- maximizes distance from chaser,
- stays within a reasonable range of guard support when safe,
- avoids corners / dead ends when threatened.

### Chaser AI
Chaser prioritizes catching:
- targets VIP (or configured target),
- uses estimated success probability (learned from past attempts),
- adapts strategy (direct chase vs prediction / cut-off).

---

## Architecture (High-Level)
- `Application`  
  Controls the simulation loop (turn order, speed randomization, collision/catch resolution, record broadcasting). :contentReference[oaicite:16]{index=16}
- `BaseBot` (abstract)  
  Shared fields (id/role/hp/speed) and shared helper methods (position, distance, movement helpers).  
  Enforces per-robot `takeTurn(view)` via polymorphism.
- `VIPBot`, `GuardBot`, `ChaserBot`  
  Concrete implementations of `takeTurn(...)` with unique strategies.
- `PlayerInfo` (record)  
  Minimal “public” information that the Application sends to each bot each turn. :contentReference[oaicite:17]{index=17}

---

## Getting Started

### Prerequisites
- **IntelliJ IDEA**
- **Java JDK** (use the version required by your course setup)
- **Becker Robots library (.jar)** added as a project dependency (File → Project Structure → Libraries)

### Run
1. Open the project in IntelliJ IDEA.
2. Ensure the Becker `.jar` is attached as a library.
3. Run the main entry point:
   - `Application` (or the class your team uses as the simulation driver)

---

## Testing (20% of grade)
We provide a write-up and thorough tests demonstrating:
- normal cases (typical movement / chase / guard protection),
- boundary cases (edge of arena, corner traps, minimum/maximum speed, low HP),
- AI strategy correctness and strategy-switch behavior.

> The assignment requires a write-up plus evidence of thorough testing (20% of the summative mark). :contentReference[oaicite:18]{index=18} :contentReference[oaicite:19]{index=19}

---

## Notes & Debugging Tips
We use `.setColor(...)` and `.setLabel(...)` for visual debugging and tracking robot roles during simulation runs. :contentReference[oaicite:20]{index=20}

---

## Due Date
Both code and write-up are due **Friday, January 16, 2026**. :contentReference[oaicite:21]{index=21}
