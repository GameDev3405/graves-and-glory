package gg.gravesandglory;

/** Task 16 P1 (R72): a capture's event windows do not overlap, so a hit five frames into an attack owns only the frames that can show it. */
public final class StepWindowsCheck {
    public static void main(String[] args) {
        System.out.println("StepWindowsCheck: " + StepWindows.selfCheck());
    }
}
