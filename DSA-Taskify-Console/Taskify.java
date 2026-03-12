package tanmayee;
import java.util.*;
import java.time.*;
import java.time.format.*;
import java.io.*;
import java.nio.file.*;

/**
 * ╔══════════════════════════════════════════════╗
 * ║          ✦ TASKIFY — Java Console App        ║
 * ║   Academic Task Manager with Gamification    ║
 * ╚══════════════════════════════════════════════╝
 *
 * Run:  javac Taskify.java  →  java Taskify
 * Data is saved to taskify_data.txt in the same folder.
 */
public class Taskify {

    // ══════════════════════════════════════════════
    //  CONSTANTS
    // ══════════════════════════════════════════════
    static final String DATA_FILE = "taskify_data.txt";
    static final String[] QUOTES = {
        "✨ Small steps every day lead to big achievements!",
        "🚀 Believe in yourself — you've got this!",
        "📚 Knowledge is the key to unlocking your future.",
        "💡 Every task completed is a step closer to your dreams.",
        "🌟 Consistency beats perfection. Keep going!",
        "🔥 Your hard work today is your success tomorrow.",
        "🎯 Focus on progress, not perfection.",
        "🌈 The best time to start is right now!",
        "💪 Challenges are just opportunities in disguise.",
        "⚡ You are stronger than you think. Keep pushing!"
    };

    // Themes: name, XP required
    static final String[][] THEMES = {
        {"Lavender Dream", "0"},
        {"Ocean Breeze",   "200"},
        {"Forest Walk",    "500"},
        {"Sunset Glow",    "800"},
        {"Candy Pop",      "1200"},
        {"Midnight Study", "2000"}
    };

    // Badges: emoji, name, type(tasks/score), required
    static final String[][] BADGES = {
        {"🌟", "First Task",   "tasks", "1"},
        {"🔥", "5 Tasks",      "tasks", "5"},
        {"💪", "10 Tasks",     "tasks", "10"},
        {"⭐", "100 XP",       "score", "100"},
        {"🏅", "500 XP",       "score", "500"},
        {"🏆", "1000 XP",      "score", "1000"}
    };

    // ══════════════════════════════════════════════
    //  DATA MODELS  (plain inner classes)
    // ══════════════════════════════════════════════
    static class User {
        long id; String name, email, pwHash, phone, role, theme;
        int score, level;
        List<String> earnedBadges = new ArrayList<>();
        List<String> notifications = new ArrayList<>(); // stored as "type|msg|timestamp"
        List<Integer> blockedIds   = new ArrayList<>(); // ids of blocked faculty (by report)

        User() { id = System.currentTimeMillis(); theme = "Lavender Dream"; score = 0; level = 1; }
    }

    static class Task {
        long id; long uid; String name, subject, faculty, deadline; int xp; boolean done;
        Task() { id = System.currentTimeMillis(); xp = 50; done = false; }
    }

    static class Assignment {
        long id; long teacherId; String title, subject, desc, deadline; int maxMarks;
        Assignment() { id = System.currentTimeMillis(); maxMarks = 100; }
    }

    static class Grade {
        long id; long assignId; long studentId; int marks; String letter, feedback;
        Grade() { id = System.currentTimeMillis(); }
    }

    static class Report {
        long id; long reporterId; String facultyName, facultyPhone, reason;
        Report() { id = System.currentTimeMillis(); }
    }

    static class Query {
        long id; long studentId; long taskId; String text;
        Query() { id = System.currentTimeMillis(); }
    }

    // ══════════════════════════════════════════════
    //  APP STATE
    // ══════════════════════════════════════════════
    static List<User>       users       = new ArrayList<>();
    static List<Task>       tasks       = new ArrayList<>();
    static List<Assignment> assignments = new ArrayList<>();
    static List<Grade>      grades      = new ArrayList<>();
    static List<Report>     reports     = new ArrayList<>();
    static List<Query>      queries     = new ArrayList<>();

    static User currentUser = null;
    static Scanner sc = new Scanner(System.in);

    // ══════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════
    public static void main(String[] args) {
        loadData();
        clearScreen();
        printBanner();
        System.out.println(randomQuote());
        System.out.println();
        authMenu();
        saveData();
    }

    // ══════════════════════════════════════════════
    //  AUTH
    // ══════════════════════════════════════════════
    static void authMenu() {
        while (true) {
            System.out.println("╔════════════════════════════╗");
            System.out.println("║     Welcome to Taskify     ║");
            System.out.println("╠════════════════════════════╣");
            System.out.println("║  1. Register               ║");
            System.out.println("║  2. Login                  ║");
            System.out.println("║  3. Exit                   ║");
            System.out.println("╚════════════════════════════╝");
            String ch = prompt("Choose");
            switch (ch) {
                case "1" -> registerMenu();
                case "2" -> loginMenu();
                case "3" -> { saveData(); System.out.println("Goodbye! 👋"); return; }
                default  -> System.out.println("❌ Invalid choice.");
            }
        }
    }

    static void registerMenu() {
        System.out.println("\n--- REGISTER ---");
        System.out.println("Role: 1) Student   2) Teacher");
        String roleChoice = prompt("Role");
        String role = roleChoice.equals("2") ? "teacher" : "student";

        String name  = prompt("Full Name");
        String email = prompt("Email").toLowerCase();
        String pass  = prompt("Password (min 4 chars)");
        String phone = prompt("WhatsApp (e.g. +91XXXXXXXXXX)");

        if (name.isBlank() || email.isBlank() || pass.length() < 4) {
            System.out.println("❌ Invalid input. Name, email and password (4+ chars) required.");
            return;
        }
        if (findUserByEmail(email) != null) {
            System.out.println("❌ Email already registered. Please login.");
            return;
        }

        User u = new User();
        u.name = name; u.email = email; u.phone = phone;
        u.pwHash = hash(pass); u.role = role;
        users.add(u);
        saveData();
        System.out.println("✅ Account created! Welcome, " + name + "! 🎉");
        currentUser = u;
        addNotification(u, "s", "Welcome to Taskify, " + name + "! 🎉");
        bootUser();
    }

    static void loginMenu() {
        System.out.println("\n--- LOGIN ---");
        String email = prompt("Email").toLowerCase();
        String pass  = prompt("Password");

        User u = findUserByEmail(email);
        if (u == null) { System.out.println("❌ No account found. Please register first."); return; }
        if (!u.pwHash.equals(hash(pass))) { System.out.println("❌ Wrong password."); return; }

        currentUser = u;
        System.out.println("✅ Welcome back, " + u.name + "! 👋");
        bootUser();
    }

    static void bootUser() {
        if (currentUser == null) return;
        saveData();
        if (currentUser.role.equals("teacher")) teacherMenu();
        else studentMenu();
    }

    // ══════════════════════════════════════════════
    //  STUDENT MENU
    // ══════════════════════════════════════════════
    static void studentMenu() {
        while (true) {
            clearScreen();
            User u = getUser(currentUser.id);
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.printf( "║  👋 %s | ⭐ %d XP | Lv.%d%n", padRight(u.name, 20), u.score, u.level);
            System.out.println("╠════════════════════════════════════════════╣");
            System.out.println("║  1. 🏠 Dashboard                           ║");
            System.out.println("║  2. 📋 My Tasks                            ║");
            System.out.println("║  3. 📈 Progress & Grades                   ║");
            System.out.println("║  4. 🎨 Themes                              ║");
            System.out.println("║  5. 💬 Queries                             ║");
            System.out.println("║  6. 🛡️  Safety Centre                      ║");
            System.out.println("║  7. 🔔 Notifications                       ║");
            System.out.println("║  8. Logout                                 ║");
            System.out.println("╚════════════════════════════════════════════╝");
            System.out.println("  💡 " + randomQuote());
            System.out.println();
            String ch = prompt("Choose");
            switch (ch) {
                case "1" -> dashboard();
                case "2" -> taskMenu();
                case "3" -> progressMenu();
                case "4" -> themesMenu();
                case "5" -> queriesMenu();
                case "6" -> safetyMenu();
                case "7" -> showNotifications();
                case "8" -> { currentUser = null; return; }
                default  -> System.out.println("❌ Invalid choice.");
            }
        }
    }

    // ── Dashboard ──────────────────────────────────
    static void dashboard() {
        User u = getUser(currentUser.id);
        List<Task> myTasks = getTasksOf(u.id);
        long done = myTasks.stream().filter(t -> t.done).count();
        long total = myTasks.size();
        long overdue = myTasks.stream().filter(t -> !t.done && isOverdue(t.deadline)).count();
        int pct = total == 0 ? 0 : (int)(done * 100 / total);

        System.out.println("\n━━━━━━━━━━━━ 🏠 DASHBOARD ━━━━━━━━━━━━");
        System.out.println("  Name    : " + u.name);
        System.out.println("  XP      : " + u.score + " (" + (u.score % 500) + "/500 to next level)");
        System.out.println("  Level   : " + u.level);
        System.out.println("  Theme   : " + u.theme);

        // XP bar
        int filled = (u.score % 500) / 5; // 0-100 scale, 50 chars
        System.out.print("  XP Bar  : [");
        for (int i = 0; i < 50; i++) System.out.print(i < filled/2 ? "█" : "░");
        System.out.println("] " + (u.score % 500) + "/500");

        System.out.println("  Tasks   : " + done + "/" + total + " done (" + pct + "%)");
        if (overdue > 0) System.out.println("  ⚠️  Overdue: " + overdue + " task(s)!");

        System.out.println("\n  🏆 Badges:");
        printBadges(u);

        // Upcoming tasks
        System.out.println("\n  📅 Upcoming Tasks (next 5):");
        myTasks.stream()
            .filter(t -> !t.done)
            .sorted(Comparator.comparing(t -> t.deadline))
            .limit(5)
            .forEach(t -> {
                String flag = isOverdue(t.deadline) ? " ⚠️ OVERDUE" : "";
                System.out.println("    • " + t.name + " | " + t.subject + " | Due: " + t.deadline + flag);
            });
        if (myTasks.stream().filter(t -> !t.done).count() == 0) System.out.println("    (No pending tasks)");

        waitEnter();
    }

    // ── Tasks ──────────────────────────────────────
    static void taskMenu() {
        while (true) {
            System.out.println("\n━━━━━━━━━━━━ 📋 MY TASKS ━━━━━━━━━━━━");
            System.out.println("  1. View All Tasks");
            System.out.println("  2. Add New Task");
            System.out.println("  3. Mark Task as Done");
            System.out.println("  4. Delete Task");
            System.out.println("  5. Filter by Subject");
            System.out.println("  6. View Assignments (from teachers)");
            System.out.println("  0. Back");
            String ch = prompt("Choose");
            switch (ch) {
                case "1" -> listTasks(null);
                case "2" -> addTask();
                case "3" -> completeTask();
                case "4" -> deleteTask();
                case "5" -> filterBySubject();
                case "6" -> viewAssignments();
                case "0" -> { return; }
                default  -> System.out.println("❌ Invalid.");
            }
        }
    }

    static void listTasks(String subjectFilter) {
        List<Task> mine = getTasksOf(currentUser.id);
        if (subjectFilter != null) mine = mine.stream().filter(t -> t.subject.equalsIgnoreCase(subjectFilter)).collect(java.util.stream.Collectors.toList());
        if (mine.isEmpty()) { System.out.println("  (No tasks found)"); waitEnter(); return; }

        System.out.println("\n  #  | Status | Name                  | Subject  | XP  | Deadline");
        System.out.println("  ---|--------|-----------------------|----------|-----|-------------------");
        for (int i = 0; i < mine.size(); i++) {
            Task t = mine.get(i);
            String status = t.done ? "✅ Done" : (isOverdue(t.deadline) ? "⚠️ Over" : "⏳ Pend");
            System.out.printf("  %-2d | %-6s | %-21s | %-8s | %-3d | %s%n",
                i+1, status, truncate(t.name,21), truncate(t.subject,8), t.xp, t.deadline);
        }
        waitEnter();
    }

    static void addTask() {
        System.out.println("\n--- ADD NEW TASK ---");
        String name    = prompt("Task Name *");
        if (name.isBlank()) { System.out.println("❌ Name required."); return; }
        String subject = prompt("Subject (e.g. DSA)");
        String xpStr   = prompt("XP Points [50]");
        int xp = xpStr.isBlank() ? 50 : parseIntSafe(xpStr, 50);
        String date    = prompt("Deadline Date (YYYY-MM-DD)");
        String time    = prompt("Deadline Time (HH:MM) [23:59]");
        if (time.isBlank()) time = "23:59";
        String faculty = prompt("Faculty WhatsApp (+91...)");

        Task t = new Task();
        t.id = System.currentTimeMillis();
        t.uid = currentUser.id;
        t.name = name; t.subject = subject;
        t.xp = xp; t.deadline = date + " " + time;
        t.faculty = faculty;
        tasks.add(t);
        saveData();
        System.out.println("✅ Task added! ✦");
    }

    static void completeTask() {
        List<Task> mine = getTasksOf(currentUser.id).stream().filter(t -> !t.done).collect(java.util.stream.Collectors.toList());
        if (mine.isEmpty()) { System.out.println("  (No pending tasks)"); return; }
        listTasks(null);
        String idx = prompt("Task number to mark done");
        int i = parseIntSafe(idx, 0) - 1;
        // re-fetch all tasks to find the correct one
        List<Task> all = getTasksOf(currentUser.id);
        if (i < 0 || i >= all.size()) { System.out.println("❌ Invalid."); return; }
        Task t = all.get(i);
        if (t.done) { System.out.println("Already done!"); return; }
        t.done = true;

        // award XP
        User u = getUser(currentUser.id);
        int oldLevel = u.level;
        u.score += t.xp;
        u.level = u.score / 500 + 1;
        if (u.level > oldLevel) System.out.println("🎉 LEVEL UP! Now Level " + u.level + "!");
        checkBadges(u);
        saveData();
        System.out.println("✅ Task complete! +" + t.xp + " XP earned! 🎉");
        addNotification(u, "s", "Task completed: " + t.name + " (+" + t.xp + " XP)");
    }

    static void deleteTask() {
        listTasks(null);
        List<Task> mine = getTasksOf(currentUser.id);
        if (mine.isEmpty()) return;
        String idx = prompt("Task number to delete (0 to cancel)");
        int i = parseIntSafe(idx, 0) - 1;
        if (i < 0 || i >= mine.size()) { System.out.println("Cancelled."); return; }
        tasks.remove(mine.get(i));
        saveData();
        System.out.println("🗑️ Task deleted.");
    }

    static void filterBySubject() {
        String sub = prompt("Subject to filter by");
        listTasks(sub);
    }

    static void viewAssignments() {
        System.out.println("\n━━━━ 📋 ASSIGNMENTS FROM TEACHERS ━━━━");
        if (assignments.isEmpty()) { System.out.println("  (No assignments posted yet)"); waitEnter(); return; }
        assignments.forEach(a -> {
            System.out.println("  📋 " + a.title + " [" + a.subject + "]");
            System.out.println("     By: " + getUserName(a.teacherId) + " | Due: " + a.deadline + " | Max: " + a.maxMarks + " marks");
            if (!a.desc.isBlank()) System.out.println("     " + a.desc);
            // check if graded
            grades.stream()
                .filter(g -> g.assignId == a.id && g.studentId == currentUser.id)
                .findFirst()
                .ifPresent(g -> System.out.println("     ✅ Grade: " + g.letter + " (" + g.marks + "/" + a.maxMarks + ") — " + g.feedback));
            System.out.println();
        });
        waitEnter();
    }

    // ── Progress ───────────────────────────────────
    static void progressMenu() {
        User u = getUser(currentUser.id);
        List<Task> mine = getTasksOf(u.id);
        long done = mine.stream().filter(t -> t.done).count();
        long total = mine.size();
        int pct = total == 0 ? 0 : (int)(done * 100 / total);

        System.out.println("\n━━━━━━━━━━━ 📈 PROGRESS ━━━━━━━━━━━━━");
        System.out.println("  Overall: " + done + "/" + total + " tasks done (" + pct + "%)");
        System.out.println("  XP     : " + u.score);

        // Subject breakdown
        System.out.println("\n  Subject Breakdown:");
        mine.stream()
            .map(t -> t.subject)
            .distinct()
            .forEach(sub -> {
                long subTotal = mine.stream().filter(t -> t.subject.equals(sub)).count();
                long subDone  = mine.stream().filter(t -> t.subject.equals(sub) && t.done).count();
                int sp = (int)(subDone * 100 / subTotal);
                System.out.println("    " + padRight(sub.isBlank() ? "(none)" : sub, 12) + " : " + subDone + "/" + subTotal + " (" + sp + "%)");
            });

        // Grades
        System.out.println("\n  📊 Assignment Grades:");
        List<Grade> myGrades = grades.stream().filter(g -> g.studentId == u.id).collect(java.util.stream.Collectors.toList());
        if (myGrades.isEmpty()) {
            System.out.println("    (No grades yet)");
        } else {
            System.out.println("    Assignment             | Grade | Marks  | Feedback");
            System.out.println("    -----------------------|-------|--------|----------");
            myGrades.forEach(g -> {
                Assignment a = assignments.stream().filter(x -> x.id == g.assignId).findFirst().orElse(null);
                String aTitle = a != null ? a.title : "—";
                System.out.printf("    %-22s | %-5s | %3d/%-3d | %s%n",
                    truncate(aTitle, 22), g.letter, g.marks, (a != null ? a.maxMarks : 100), g.feedback);
            });

            // Grade distribution
            long cntA = myGrades.stream().filter(g -> "A".equals(g.letter)).count();
            long cntB = myGrades.stream().filter(g -> "B".equals(g.letter)).count();
            long cntC = myGrades.stream().filter(g -> "C".equals(g.letter)).count();
            long cntD = myGrades.stream().filter(g -> "D".equals(g.letter)).count();
            System.out.println("\n  Grade Distribution: A=" + cntA + "  B=" + cntB + "  C=" + cntC + "  D=" + cntD);
        }

        // Improvement tips
        System.out.println("\n  🔍 Improvement Tips:");
        if (done == 0) System.out.println("    • Complete your first task to get started!");
        if (mine.stream().anyMatch(t -> isOverdue(t.deadline) && !t.done))
            System.out.println("    • You have overdue tasks — tackle those first!");
        if (pct < 50) System.out.println("    • Try to complete more than half your tasks.");
        if (myGrades.stream().anyMatch(g -> "C".equals(g.letter) || "D".equals(g.letter)))
            System.out.println("    • You have C/D grades — consider revisiting those subjects.");
        if (pct >= 80) System.out.println("    • Great job! Keep maintaining this pace! 🔥");

        waitEnter();
    }

    // ── Themes ─────────────────────────────────────
    static void themesMenu() {
        User u = getUser(currentUser.id);
        System.out.println("\n━━━━━━━━━━━ 🎨 THEMES ━━━━━━━━━━━━━━");
        System.out.println("  Your XP: " + u.score);
        System.out.println();
        for (int i = 0; i < THEMES.length; i++) {
            String name = THEMES[i][0];
            int req  = Integer.parseInt(THEMES[i][1]);
            boolean unlocked = u.score >= req;
            boolean current  = u.theme.equals(name);
            String status = current ? " ◀ ACTIVE" : (unlocked ? " (unlocked)" : " 🔒 " + req + " XP needed");
            System.out.println("  " + (i+1) + ". " + name + status);
        }
        System.out.println("  0. Back");
        String ch = prompt("Choose theme (0 to back)");
        int i = parseIntSafe(ch, 0) - 1;
        if (i < 0 || i >= THEMES.length) return;
        int req = Integer.parseInt(THEMES[i][1]);
        if (u.score < req) { System.out.println("❌ Need " + req + " XP to unlock this theme."); waitEnter(); return; }
        u.theme = THEMES[i][0];
        saveData();
        System.out.println("✅ Theme applied: " + u.theme + " 🎨");
    }

    // ── Queries ────────────────────────────────────
    static void queriesMenu() {
        while (true) {
            System.out.println("\n━━━━━━━━━━━ 💬 QUERIES ━━━━━━━━━━━━━");
            System.out.println("  1. Send Query");
            System.out.println("  2. View My Queries");
            System.out.println("  0. Back");
            String ch = prompt("Choose");
            switch (ch) {
                case "1" -> sendQuery();
                case "2" -> viewMyQueries();
                case "0" -> { return; }
                default  -> System.out.println("❌ Invalid.");
            }
        }
    }

    static void sendQuery() {
        List<Task> mine = getTasksOf(currentUser.id);
        if (mine.isEmpty()) { System.out.println("  Add tasks first."); return; }
        System.out.println("\n  Your tasks:");
        for (int i = 0; i < mine.size(); i++)
            System.out.println("  " + (i+1) + ". " + mine.get(i).name + " | Faculty: " + mine.get(i).faculty);
        String idx = prompt("Task number to query about (0 to cancel)");
        int i = parseIntSafe(idx, 0) - 1;
        if (i < 0 || i >= mine.size()) return;
        Task t = mine.get(i);
        String text = prompt("Your query");
        if (text.isBlank()) { System.out.println("❌ Query text required."); return; }

        Query q = new Query();
        q.studentId = currentUser.id; q.taskId = t.id; q.text = text;
        queries.add(q);
        saveData();

        System.out.println("✅ Query saved!");
        if (!t.faculty.isBlank()) {
            System.out.println("📲 To send via WhatsApp:");
            System.out.println("   wa.me/" + t.faculty.replace("+", "") + "?text=" +
                "Re%3A%20" + t.name.replace(" ", "%20") + "%0A" + text.replace(" ", "%20"));
        }
    }

    static void viewMyQueries() {
        List<Query> mine = queries.stream().filter(q -> q.studentId == currentUser.id).collect(java.util.stream.Collectors.toList());
        if (mine.isEmpty()) { System.out.println("  (No queries sent yet)"); waitEnter(); return; }
        mine.forEach(q -> {
            Task t = tasks.stream().filter(x -> x.id == q.taskId).findFirst().orElse(null);
            System.out.println("  • " + q.text + " [Task: " + (t != null ? t.name : "—") + "]");
        });
        waitEnter();
    }

    // ── Safety ─────────────────────────────────────
    static void safetyMenu() {
        while (true) {
            System.out.println("\n━━━━━━━━━━━ 🛡️ SAFETY CENTRE ━━━━━━━━");
            System.out.println("  1. Report Faculty");
            System.out.println("  2. Block Faculty");
            System.out.println("  3. View Blocked Faculty");
            System.out.println("  0. Back");
            String ch = prompt("Choose");
            switch (ch) {
                case "1" -> doReport();
                case "2" -> doBlock();
                case "3" -> viewBlocked();
                case "0" -> { return; }
                default  -> System.out.println("❌ Invalid.");
            }
        }
    }

    static void doReport() {
        String name   = prompt("Faculty Name *");
        String phone  = prompt("Faculty WhatsApp");
        String reason = prompt("Reason");
        if (name.isBlank()) { System.out.println("❌ Name required."); return; }
        Report r = new Report();
        r.reporterId = currentUser.id; r.facultyName = name; r.facultyPhone = phone; r.reason = reason;
        reports.add(r);
        saveData();
        System.out.println("✅ Report submitted.");
    }

    static void doBlock() {
        String name  = prompt("Faculty Name to block");
        String phone = prompt("Faculty WhatsApp");
        if (name.isBlank()) { System.out.println("❌ Name required."); return; }
        doReport(); // also report when blocking
        System.out.println("🚫 Faculty blocked. They have also been reported.");
    }

    static void viewBlocked() {
        List<Report> mine = reports.stream().filter(r -> r.reporterId == currentUser.id).collect(java.util.stream.Collectors.toList());
        if (mine.isEmpty()) { System.out.println("  (No faculty blocked/reported)"); waitEnter(); return; }
        mine.forEach(r -> System.out.println("  🚫 " + r.facultyName + " | " + r.facultyPhone + " | " + r.reason));
        waitEnter();
    }

    // ── Notifications ──────────────────────────────
    static void showNotifications() {
        User u = getUser(currentUser.id);
        System.out.println("\n━━━━━━━━━━━ 🔔 NOTIFICATIONS ━━━━━━━━━");
        if (u.notifications.isEmpty()) { System.out.println("  (No notifications)"); waitEnter(); return; }
        u.notifications.forEach(n -> {
            String[] parts = n.split("\\|", 3);
            String type = parts[0], msg = parts.length > 2 ? parts[2] : parts[1];
            String icon = type.equals("s") ? "✅" : type.equals("u") ? "🚨" : type.equals("gd") ? "📊" : "🔔";
            System.out.println("  " + icon + " " + msg);
        });
        waitEnter();
    }

    // ══════════════════════════════════════════════
    //  TEACHER MENU
    // ══════════════════════════════════════════════
    static void teacherMenu() {
        while (true) {
            clearScreen();
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.printf( "║  👩‍🏫 %s (Teacher)%n", padRight(currentUser.name, 34));
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  1. 📋 Assignments                       ║");
            System.out.println("║  2. 📊 Grade Students                    ║");
            System.out.println("║  3. 👥 View Students                     ║");
            System.out.println("║  4. 💬 Student Queries                   ║");
            System.out.println("║  5. 🚩 Reports                           ║");
            System.out.println("║  6. Logout                               ║");
            System.out.println("╚══════════════════════════════════════════╝");
            String ch = prompt("Choose");
            switch (ch) {
                case "1" -> assignmentsMenu();
                case "2" -> gradeMenu();
                case "3" -> viewStudents();
                case "4" -> viewQueries();
                case "5" -> viewReports();
                case "6" -> { currentUser = null; return; }
                default  -> System.out.println("❌ Invalid.");
            }
        }
    }

    // ── Assignments ────────────────────────────────
    static void assignmentsMenu() {
        while (true) {
            System.out.println("\n━━━━━━━━━━ 📋 ASSIGNMENTS ━━━━━━━━━━━━");
            System.out.println("  1. View My Assignments");
            System.out.println("  2. Post New Assignment");
            System.out.println("  3. Delete Assignment");
            System.out.println("  0. Back");
            String ch = prompt("Choose");
            switch (ch) {
                case "1" -> listMyAssignments();
                case "2" -> addAssignment();
                case "3" -> deleteAssignment();
                case "0" -> { return; }
                default  -> System.out.println("❌ Invalid.");
            }
        }
    }

    static void listMyAssignments() {
        List<Assignment> mine = assignments.stream().filter(a -> a.teacherId == currentUser.id).collect(java.util.stream.Collectors.toList());
        if (mine.isEmpty()) { System.out.println("  (No assignments yet)"); waitEnter(); return; }
        System.out.println("\n  #  | Title                  | Subject  | Deadline            | Max");
        System.out.println("  ---|------------------------|----------|---------------------|----");
        for (int i = 0; i < mine.size(); i++) {
            Assignment a = mine.get(i);
            System.out.printf("  %-2d | %-22s | %-8s | %-19s | %d%n",
                i+1, truncate(a.title,22), truncate(a.subject,8), a.deadline, a.maxMarks);
        }
        waitEnter();
    }

    static void addAssignment() {
        System.out.println("\n--- POST NEW ASSIGNMENT ---");
        String title = prompt("Title *");
        if (title.isBlank()) { System.out.println("❌ Title required."); return; }
        String subject = prompt("Subject");
        String desc    = prompt("Description");
        String date    = prompt("Deadline Date (YYYY-MM-DD)");
        String time    = prompt("Deadline Time (HH:MM) [23:59]");
        if (time.isBlank()) time = "23:59";
        String maxStr  = prompt("Max Marks [100]");
        int max = maxStr.isBlank() ? 100 : parseIntSafe(maxStr, 100);

        Assignment a = new Assignment();
        a.teacherId = currentUser.id; a.title = title; a.subject = subject;
        a.desc = desc; a.deadline = date + " " + time; a.maxMarks = max;
        assignments.add(a);

        // notify all students
        users.stream().filter(u -> u.role.equals("student")).forEach(u ->
            addNotification(u, "i", "New assignment: " + title + " [" + subject + "] due " + date));
        saveData();
        System.out.println("✅ Assignment posted! 📋");
    }

    static void deleteAssignment() {
        List<Assignment> mine = assignments.stream().filter(a -> a.teacherId == currentUser.id).collect(java.util.stream.Collectors.toList());
        if (mine.isEmpty()) { System.out.println("  (No assignments)"); return; }
        listMyAssignments();
        String idx = prompt("Number to delete (0 to cancel)");
        int i = parseIntSafe(idx, 0) - 1;
        if (i < 0 || i >= mine.size()) { System.out.println("Cancelled."); return; }
        assignments.remove(mine.get(i));
        saveData();
        System.out.println("🗑️ Assignment deleted.");
    }

    // ── Grading ────────────────────────────────────
    static void gradeMenu() {
        List<Assignment> mine = assignments.stream().filter(a -> a.teacherId == currentUser.id).collect(java.util.stream.Collectors.toList());
        List<User> students = users.stream().filter(u -> u.role.equals("student")).collect(java.util.stream.Collectors.toList());

        if (mine.isEmpty()) { System.out.println("  (Post assignments first)"); waitEnter(); return; }
        if (students.isEmpty()) { System.out.println("  (No students registered)"); waitEnter(); return; }

        System.out.println("\n━━━━━━━━━━━ 📊 GRADE STUDENTS ━━━━━━━━");
        for (int i = 0; i < mine.size(); i++)
            System.out.println("  " + (i+1) + ". " + mine.get(i).title + " [Max: " + mine.get(i).maxMarks + "]");
        String ai = prompt("Select assignment (0 cancel)");
        int aIdx = parseIntSafe(ai, 0) - 1;
        if (aIdx < 0 || aIdx >= mine.size()) return;
        Assignment a = mine.get(aIdx);

        System.out.println("\n  Students:");
        for (int i = 0; i < students.size(); i++)
            System.out.println("  " + (i+1) + ". " + students.get(i).name + " | " + students.get(i).email);
        String si = prompt("Select student (0 cancel)");
        int sIdx = parseIntSafe(si, 0) - 1;
        if (sIdx < 0 || sIdx >= students.size()) return;
        User stu = students.get(sIdx);

        String marksStr = prompt("Marks (0-" + a.maxMarks + ")");
        int marks = parseIntSafe(marksStr, 0);
        marks = Math.min(marks, a.maxMarks);

        System.out.println("  Grade: 1)A  2)B  3)C  4)D");
        String gl = prompt("Grade");
        String letter = switch (gl) { case "1" -> "A"; case "2" -> "B"; case "3" -> "C"; case "4" -> "D"; default -> ""; };
        if (letter.isBlank()) { System.out.println("❌ Invalid grade."); return; }

        String feedback = prompt("Feedback (optional)");

        // save grade (update if exists)
        Grade existing = grades.stream().filter(g -> g.assignId == a.id && g.studentId == stu.id).findFirst().orElse(null);
        if (existing != null) {
            existing.marks = marks; existing.letter = letter; existing.feedback = feedback;
        } else {
            Grade g = new Grade();
            g.assignId = a.id; g.studentId = stu.id;
            g.marks = marks; g.letter = letter; g.feedback = feedback;
            grades.add(g);
        }
        addNotification(stu, "gd", "📊 Grade: " + letter + " (" + marks + "/" + a.maxMarks + ") for \"" + a.title + "\" — " + feedback);
        saveData();
        System.out.println("✅ Grade saved for " + stu.name + "! 📊");
    }

    // ── View Students ──────────────────────────────
    static void viewStudents() {
        List<User> students = users.stream().filter(u -> u.role.equals("student")).collect(java.util.stream.Collectors.toList());
        System.out.println("\n━━━━━━━━━━━ 👥 STUDENTS ━━━━━━━━━━━━━");
        if (students.isEmpty()) { System.out.println("  (No students yet)"); waitEnter(); return; }
        System.out.println("  Name                  | Email                      | XP   | Level");
        System.out.println("  ----------------------|----------------------------|------|------");
        students.forEach(u -> System.out.printf("  %-22s | %-26s | %-4d | %d%n",
            truncate(u.name,22), truncate(u.email,26), u.score, u.level));
        waitEnter();
    }

    // ── Queries (Teacher view) ─────────────────────
    static void viewQueries() {
        System.out.println("\n━━━━━━━━━━━ 💬 STUDENT QUERIES ━━━━━━━");
        if (queries.isEmpty()) { System.out.println("  (No queries yet)"); waitEnter(); return; }
        queries.forEach(q -> {
            User stu = users.stream().filter(u -> u.id == q.studentId).findFirst().orElse(null);
            Task t   = tasks.stream().filter(x -> x.id == q.taskId).findFirst().orElse(null);
            String sName = stu != null ? stu.name : "Unknown";
            String tName = t   != null ? t.name   : "Unknown task";
            System.out.println("  👤 " + sName + " | Task: " + tName);
            System.out.println("     " + q.text);
            if (stu != null && !stu.phone.isBlank())
                System.out.println("     📲 Reply: wa.me/" + stu.phone.replace("+",""));
            System.out.println();
        });
        waitEnter();
    }

    // ── Reports (Teacher view) ─────────────────────
    static void viewReports() {
        System.out.println("\n━━━━━━━━━━━ 🚩 REPORTS ━━━━━━━━━━━━━━");
        if (reports.isEmpty()) { System.out.println("  (No reports)"); waitEnter(); return; }
        reports.forEach(r -> {
            String by = getUserName(r.reporterId);
            System.out.println("  🚩 " + r.facultyName + " | Phone: " + r.facultyPhone);
            System.out.println("     Reason: " + r.reason + " | By: " + by);
        });
        waitEnter();
    }

    // ══════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════
    static String prompt(String label) {
        System.out.print("  " + label + ": ");
        return sc.nextLine().trim();
    }

    static void waitEnter() {
        System.out.print("\n  [Press ENTER to continue] ");
        sc.nextLine();
    }

    static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033[H\033[2J");
        } catch (Exception ignored) {}
        System.out.flush();
    }

    static void printBanner() {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║           ✦  T A S K I F Y  ✦            ║");
        System.out.println("║    Your Academic Journey, Gamified 🎮     ║");
        System.out.println("╚═══════════════════════════════════════════╝");
        System.out.println();
    }

    static String randomQuote() {
        return QUOTES[new Random().nextInt(QUOTES.length)];
    }

    static String hash(String pw) {
        int h = 5381;
        for (char c : pw.toCharArray()) h = ((h << 5) + h) ^ c;
        return "h" + Math.abs(h) + "_" + pw.length();
    }

    static User findUserByEmail(String email) {
        return users.stream().filter(u -> u.email.equalsIgnoreCase(email)).findFirst().orElse(null);
    }

    static User getUser(long id) {
        return users.stream().filter(u -> u.id == id).findFirst().orElse(currentUser);
    }

    static String getUserName(long id) {
        User u = users.stream().filter(x -> x.id == id).findFirst().orElse(null);
        return u != null ? u.name : "Unknown";
    }

    static List<Task> getTasksOf(long uid) {
        return tasks.stream().filter(t -> t.uid == uid).collect(java.util.stream.Collectors.toList());
    }

    static boolean isOverdue(String deadline) {
        if (deadline == null || deadline.isBlank()) return false;
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return LocalDateTime.parse(deadline, f).isBefore(LocalDateTime.now());
        } catch (Exception e) { return false; }
    }

    static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max-1) + "…";
    }

    static String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s.length() > n ? s.substring(0, n) : s);
    }

    static void addNotification(User u, String type, String msg) {
        u.notifications.add(0, type + "|" + System.currentTimeMillis() + "|" + msg);
        if (u.notifications.size() > 50) u.notifications = u.notifications.subList(0, 50);
    }

    static void checkBadges(User u) {
        int done = (int) tasks.stream().filter(t -> t.uid == u.id && t.done).count();
        for (String[] b : BADGES) {
            String bId = b[1]; // badge name as id
            if (u.earnedBadges.contains(bId)) continue;
            int req = Integer.parseInt(b[3]);
            boolean earned = b[2].equals("tasks") ? done >= req : u.score >= req;
            if (earned) {
                u.earnedBadges.add(bId);
                System.out.println("🏆 Badge unlocked: " + b[0] + " " + b[1] + "!");
                addNotification(u, "s", "Badge unlocked: " + b[0] + " " + b[1]);
            }
        }
    }

    static void printBadges(User u) {
        StringBuilder sb = new StringBuilder("    ");
        for (String[] b : BADGES) {
            boolean earned = u.earnedBadges.contains(b[1]);
            sb.append(earned ? b[0] : "⬜").append(" ");
        }
        System.out.println(sb);
        for (String[] b : BADGES) {
            boolean earned = u.earnedBadges.contains(b[1]);
            if (earned) System.out.println("    " + b[0] + " " + b[1]);
        }
        if (u.earnedBadges.isEmpty()) System.out.println("    (Complete tasks to earn badges!)");
    }

    // ══════════════════════════════════════════════
    //  PERSISTENCE  (simple CSV-style flat file)
    // ══════════════════════════════════════════════
    static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(DATA_FILE))) {
            // USERS
            for (User u : users) {
                pw.println("USER|" + u.id + "|" + esc(u.name) + "|" + esc(u.email) + "|" + esc(u.pwHash)
                    + "|" + esc(u.phone) + "|" + esc(u.role) + "|" + u.score + "|" + u.level + "|" + esc(u.theme)
                    + "|" + esc(String.join("~", u.earnedBadges))
                    + "|" + esc(String.join("~~", u.notifications)));
            }
            // TASKS
            for (Task t : tasks) {
                pw.println("TASK|" + t.id + "|" + t.uid + "|" + esc(t.name) + "|" + esc(t.subject)
                    + "|" + esc(t.faculty) + "|" + esc(t.deadline) + "|" + t.xp + "|" + t.done);
            }
            // ASSIGNMENTS
            for (Assignment a : assignments) {
                pw.println("ASSIGN|" + a.id + "|" + a.teacherId + "|" + esc(a.title) + "|" + esc(a.subject)
                    + "|" + esc(a.desc) + "|" + esc(a.deadline) + "|" + a.maxMarks);
            }
            // GRADES
            for (Grade g : grades) {
                pw.println("GRADE|" + g.id + "|" + g.assignId + "|" + g.studentId
                    + "|" + g.marks + "|" + esc(g.letter) + "|" + esc(g.feedback));
            }
            // REPORTS
            for (Report r : reports) {
                pw.println("REPORT|" + r.id + "|" + r.reporterId + "|" + esc(r.facultyName)
                    + "|" + esc(r.facultyPhone) + "|" + esc(r.reason));
            }
            // QUERIES
            for (Query q : queries) {
                pw.println("QUERY|" + q.id + "|" + q.studentId + "|" + q.taskId + "|" + esc(q.text));
            }
        } catch (IOException e) {
            System.out.println("⚠️  Could not save data: " + e.getMessage());
        }
    }

    static void loadData() {
        File f = new File(DATA_FILE);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|", -1);
                try {
                    switch (p[0]) {
                        case "USER" -> {
                            User u = new User();
                            u.id = Long.parseLong(p[1]); u.name = unesc(p[2]); u.email = unesc(p[3]);
                            u.pwHash = unesc(p[4]); u.phone = unesc(p[5]); u.role = unesc(p[6]);
                            u.score = Integer.parseInt(p[7]); u.level = Integer.parseInt(p[8]); u.theme = unesc(p[9]);
                            if (p.length > 10 && !p[10].isBlank())
                                u.earnedBadges = new ArrayList<>(Arrays.asList(unesc(p[10]).split("~")));
                            if (p.length > 11 && !p[11].isBlank())
                                u.notifications = new ArrayList<>(Arrays.asList(unesc(p[11]).split("~~")));
                            users.add(u);
                        }
                        case "TASK" -> {
                            Task t = new Task();
                            t.id = Long.parseLong(p[1]); t.uid = Long.parseLong(p[2]);
                            t.name = unesc(p[3]); t.subject = unesc(p[4]); t.faculty = unesc(p[5]);
                            t.deadline = unesc(p[6]); t.xp = Integer.parseInt(p[7]); t.done = Boolean.parseBoolean(p[8]);
                            tasks.add(t);
                        }
                        case "ASSIGN" -> {
                            Assignment a = new Assignment();
                            a.id = Long.parseLong(p[1]); a.teacherId = Long.parseLong(p[2]);
                            a.title = unesc(p[3]); a.subject = unesc(p[4]); a.desc = unesc(p[5]);
                            a.deadline = unesc(p[6]); a.maxMarks = Integer.parseInt(p[7]);
                            assignments.add(a);
                        }
                        case "GRADE" -> {
                            Grade g = new Grade();
                            g.id = Long.parseLong(p[1]); g.assignId = Long.parseLong(p[2]);
                            g.studentId = Long.parseLong(p[3]); g.marks = Integer.parseInt(p[4]);
                            g.letter = unesc(p[5]); g.feedback = unesc(p[6]);
                            grades.add(g);
                        }
                        case "REPORT" -> {
                            Report r = new Report();
                            r.id = Long.parseLong(p[1]); r.reporterId = Long.parseLong(p[2]);
                            r.facultyName = unesc(p[3]); r.facultyPhone = unesc(p[4]); r.reason = unesc(p[5]);
                            reports.add(r);
                        }
                        case "QUERY" -> {
                            Query q = new Query();
                            q.id = Long.parseLong(p[1]); q.studentId = Long.parseLong(p[2]);
                            q.taskId = Long.parseLong(p[3]); q.text = unesc(p[4]);
                            queries.add(q);
                        }
                    }
                } catch (Exception ignored) {} // skip malformed lines
            }
        } catch (IOException e) {
            System.out.println("⚠️  Could not load data: " + e.getMessage());
        }
    }

    // Escape pipe chars and newlines in data
    static String esc(String s)   { return s == null ? "" : s.replace("|","<PIPE>").replace("\n","<NL>"); }
    static String unesc(String s) { return s == null ? "" : s.replace("<PIPE>","|").replace("<NL>","\n"); }
}
