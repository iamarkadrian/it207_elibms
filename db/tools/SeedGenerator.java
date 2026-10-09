import edu.bulsu.lms.security.PasswordHasher;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates the demo data for both databases from one dataset, so MySQL and SQL Server always match.
 *
 * <p>PRIVACY (Data Privacy Act): no real person appears in the output. Names, student numbers,
 * phone numbers and addresses are randomly combined from pools; any resemblance to a real person
 * is coincidence. Only bibliographic data (titles, ISBNs) and the university's college/course list
 * come from real sources, and those are not personal data.
 *
 * <p>Run from the project root (the app must be compiled first so PasswordHasher is available):
 * <pre>
 *   ./mvnw -q compile
 *   java -cp target/classes db/tools/SeedGenerator.java
 * </pre>
 * The generator is deterministic except for password salts. Re-running rewrites
 * {@code db/mysql/02_seed.sql} and {@code db/mssql/02_seed.sql}.
 */
public class SeedGenerator {

    // ---- how much data to make ----
    static final long RANDOM_SEED = 20261009L;
    static final int LIBRARIANS = 29;
    static final int STUDENTS = 600;
    static final int EXTRA_BOOKS = 210;
    static final int EXTRA_DIGITAL = 90;
    static final int EXTRA_OTHER = 75;
    static final int OPEN_BOOK_LOANS = 60;
    static final int OPEN_DIGITAL_LOANS = 25;
    static final int OVERDUE_SHARE_PERCENT = 40;

    // ---- demo credentials, documented in the README. They are hashed (PBKDF2) in the output. ----
    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_PASSWORD = "Admin@2026";
    static final String LIBRARIAN_PASSWORD = "Library@2026";

    static final long DAY = 86_400L;
    static final Random R = new Random(RANDOM_SEED);

    // ---- model ----
    record Dept(String code, String name) { }
    record Course(String code, String name, String dept) { }
    record Staff(int id, String first, String mi, String last, String position, String dept, String username,
                 String hash, String role, boolean locked, int failed) { }
    record Student(String number, String first, String mi, String last, String dept, String course, String section,
                   LocalDate joined, String gender, String contact, String houseNo, String street, String barangay,
                   String municipality, String province) { }
    record Loan(int id, String item, String student, int staff, long issuedAgo, long dueAgo, Long returnedAgo) { }

    static final class Item {
        final String code, type, title, author, publisher, genre, lcClass, url, material;
        final int year, pages, days;
        boolean available;

        Item(String code, String type, String title, String author, String publisher, int year, String genre,
             String lcClass, int pages, int days, String url, String material) {
            this.code = code; this.type = type; this.title = title; this.author = author; this.publisher = publisher;
            this.year = year; this.genre = genre; this.lcClass = lcClass; this.pages = pages; this.days = days;
            this.url = url; this.material = material;
            this.available = !type.equals("OTHER");
        }
    }

    // ---- name pools (synthetic; combined at random) ----
    static final String[] FIRST_F = {"Maria", "Ana", "Angela", "Bea", "Camille", "Carla", "Diana", "Elaine", "Erica", "Faith",
            "Gabrielle", "Grace", "Hannah", "Irene", "Jasmine", "Jessa", "Joanna", "Julia", "Katrina", "Kristine", "Lara", "Lea",
            "Lorraine", "Mae", "Mariel", "Nicole", "Patricia", "Pauline", "Rachel", "Rhea", "Sarah", "Sofia", "Trisha", "Vanessa",
            "Yvonne", "Zyra", "Andrea", "Bianca", "Charlene", "Denise", "Eunice", "Frances", "Gwen", "Heidi", "Isabel", "Jenny",
            "Kaye", "Liza", "Michelle", "Nina", "Olivia", "Princess", "Queenie", "Rica", "Shaira", "Tricia"};
    static final String[] FIRST_M = {"Juan", "Jose", "Mark", "John", "Paul", "Michael", "Kevin", "Christian", "Joshua", "Daniel",
            "Ryan", "Carlo", "Miguel", "Gabriel", "Rafael", "Angelo", "Nathan", "Jericho", "Francis", "Vincent", "Patrick", "Joseph",
            "Anthony", "Aaron", "Adrian", "Albert", "Alvin", "Arnold", "Benedict", "Bryan", "Cedric", "Clarence", "Darwin", "Dennis",
            "Dominic", "Edgar", "Emmanuel", "Eric", "Felix", "Gerald", "Harold", "Ian", "Jerome", "Joel", "Jonathan", "Justin",
            "Kenneth", "Lawrence", "Leo", "Marvin", "Neil", "Noel", "Oliver", "Raymond", "Renzo", "Roland", "Samuel", "Sean",
            "Stephen", "Victor"};
    static final String[] LAST = {"Santos", "Reyes", "Cruz", "Bautista", "Ocampo", "Garcia", "Mendoza", "Torres", "Tomas", "Andrada",
            "Castillo", "Flores", "Villanueva", "Ramos", "Aquino", "Castro", "Rivera", "Gonzales", "Fernandez", "De Leon",
            "Dela Cruz", "Pascual", "Domingo", "Salvador", "Manalo", "Lopez", "Dizon", "Mercado", "Navarro", "Soriano", "Valdez",
            "Aguilar", "Alvarez", "Cabrera", "Capili", "Carino", "Concepcion", "Corpuz", "David", "Del Rosario", "Diaz", "Enriquez",
            "Espiritu", "Evangelista", "Francisco", "Guevarra", "Ignacio", "Javier", "Lacson", "Lim", "Macaraeg", "Magbanua",
            "Maglaya", "Malonzo", "Manansala", "Marcelo", "Mariano", "Miranda", "Morales", "Natividad", "Pangilinan", "Panganiban",
            "Pineda", "Quizon", "Rodriguez", "Roque", "Servando", "Sison", "Suarez", "Tolentino", "Trinidad", "Urbano", "Valencia",
            "Vergara", "Villar", "Yap", "Zamora", "Abad", "Acosta", "Agustin", "Alcantara", "Bonifacio", "Buenaventura", "Cabral",
            "Calderon", "Cortez", "Dimaculangan", "Esguerra", "Galang", "Guinto", "Hilario", "Jimenez", "Lacuesta", "Layug",
            "Lorenzo", "Pacheco", "Padilla", "Santiago", "Tiongson", "Umali", "Vasquez", "Zapanta"};
    static final String[] INITIALS = {"A.", "B.", "C.", "D.", "E.", "F.", "G.", "H.", "J.", "L.", "M.", "N.", "P.", "R.", "S.", "T.", "V."};
    static final String[] POSITIONS = {"Librarian", "Assistant Librarian", "Reference Librarian", "Technical Librarian",
            "Unit Head Librarian", "University Librarian"};

    static final String[] WRITER_FIRST = {"James", "Robert", "Linda", "Barbara", "Susan", "Thomas", "Karen", "Daniel", "Nancy", "Laura",
            "Peter", "Helen", "George", "Alice", "Henry", "Julia", "Samuel", "Rebecca", "Oliver", "Emily", "Victor", "Sophia",
            "Martin", "Clara", "Lucas", "Nora", "Hiroshi", "Amara", "Ivan", "Leila"};
    static final String[] WRITER_LAST = {"Anderson", "Brooks", "Carter", "Dawson", "Ellis", "Foster", "Grant", "Hayes", "Irwin",
            "Jensen", "Keller", "Lawson", "Morgan", "Nolan", "Owens", "Parker", "Quinn", "Rhodes", "Sutton", "Turner", "Underwood",
            "Vaughn", "Walsh", "Yates", "Zimmer", "Whitaker", "Lindgren", "Moreau", "Novak", "Okafor", "Petrov", "Tanaka", "Haddad"};
    static final String[] PUBLISHERS = {"Pearson Education", "McGraw-Hill Education", "Cengage Learning", "Oxford University Press",
            "Cambridge University Press", "John Wiley & Sons, Ltd", "Elsevier", "Springer", "Routledge", "SAGE Publications",
            "Rex Book Store", "Phoenix Publishing House", "Anvil Publishing", "University of the Philippines Press",
            "Ateneo de Manila University Press", "Vibal Group"};

    static final String[] BARANGAYS = {"San Jose", "Santo Nino", "Poblacion", "Bagong Silang", "Santa Cruz", "San Roque", "Mabolo",
            "Tikay", "Look 1st", "San Isidro", "San Miguel", "Santa Rita", "Bambang", "Caingin", "Dakila", "Longos", "Matimbo",
            "Pinagbakahan", "Santor", "Sumapang Matanda", "Tabang", "Turo", "Bulihan", "Catmon", "Ligas"};
    static final String[] MUNICIPALITIES = {"Malolos", "Baliwag", "Bustos", "Hagonoy", "Pulilan", "Bulakan", "Calumpit", "Guiguinto",
            "Paombong", "Plaridel", "San Rafael", "San Ildefonso", "Marilao", "Meycauayan", "Norzagaray", "Obando", "Pandi",
            "Angat", "Balagtas", "Santa Maria", "San Miguel"};
    static final String[] STREETS = {"Purok 1", "Purok 2", "Purok 3", "Purok 4", "Purok 5", "Purok 6", "Purok 7", "Purok 9",
            "Rizal St.", "Mabini St.", "Bonifacio St.", "Luna St.", "Del Pilar St.", "Aguinaldo St.", "Quezon Ave.", "Sampaguita St.",
            "Narra St.", "Mahogany St.", "Santol St.", "Paraluman St."};

    // ---- catalog vocabulary: topic, genre, LC letter ----
    static final String[][] TOPICS = {
            {"Algorithms and Data Structures", "Computer Science", "Q"}, {"Database Systems", "Computer Science", "Q"},
            {"Operating Systems", "Computer Science", "Q"}, {"Computer Networks", "Computer Science", "T"},
            {"Software Engineering", "Computer Science", "Q"}, {"Object-Oriented Programming", "Computer Science", "Q"},
            {"Artificial Intelligence", "Computer Science", "Q"}, {"Web Development", "Computer Science", "T"},
            {"Calculus", "Textbook", "Q"}, {"Linear Algebra", "Textbook", "Q"}, {"Statistics", "Textbook", "H"},
            {"Physics", "Science", "Q"}, {"Organic Chemistry", "Science", "Q"}, {"General Biology", "Science", "Q"},
            {"Environmental Science", "Nature", "Q"}, {"Financial Accounting", "Business", "H"},
            {"Marketing Management", "Business", "H"}, {"Entrepreneurship", "Business", "H"},
            {"Human Resource Management", "Business", "H"}, {"Business Law", "Law", "K"}, {"Criminal Law", "Law", "K"},
            {"Criminology", "Crime", "H"}, {"Constitutional Law", "Law", "K"}, {"Philippine History", "History", "D"},
            {"World History", "History", "D"}, {"Asian Civilizations", "History", "D"}, {"Political Science", "Politics", "J"},
            {"Public Administration", "Politics", "J"}, {"Sociology", "Anthropology", "H"}, {"Anthropology", "Anthropology", "G"},
            {"Geography of the Philippines", "Geography", "G"}, {"Educational Psychology", "Psychology", "L"},
            {"Curriculum Development", "Instructional", "L"}, {"Teaching Strategies", "Instructional", "L"},
            {"Early Childhood Education", "Instructional", "L"}, {"Developmental Psychology", "Psychology", "B"},
            {"Abnormal Psychology", "Psychology", "B"}, {"Philosophy of Man", "Philosophy", "B"}, {"Ethics", "Philosophy", "B"},
            {"Comparative Religion", "Religion", "B"}, {"Nursing Fundamentals", "Health", "R"}, {"Pharmacology", "Health", "R"},
            {"Community Health", "Health", "R"}, {"Nutrition and Dietetics", "Health", "R"}, {"Human Anatomy", "Health", "R"},
            {"Architectural Design", "Art & Photography", "N"}, {"Drawing and Rendering", "Art & Photography", "N"},
            {"Landscape Architecture", "Art & Photography", "N"}, {"Music Appreciation", "Art & Photography", "M"},
            {"Creative Writing", "Literature", "P"}, {"Philippine Literature", "Literature", "P"},
            {"World Literature", "Literature", "P"}, {"Journalism and Mass Media", "Literature", "P"},
            {"Engineering Mechanics", "Industrial Engineering", "T"}, {"Thermodynamics", "Industrial Engineering", "T"},
            {"Electrical Circuits", "Industrial Engineering", "T"}, {"Surveying", "Industrial Engineering", "T"},
            {"Industrial Safety", "Industrial Engineering", "T"}, {"Manufacturing Processes", "Industrial Engineering", "T"},
            {"Food Technology", "Science", "T"}, {"Hospitality Management", "Business", "T"},
            {"Tourism Management", "Business", "G"}, {"Sports Science", "Health", "G"}, {"Physical Education", "Health", "G"},
            {"Library and Information Science", "Guide", "Z"}, {"Research Methods", "Guide", "Z"},
            {"Technical Writing", "Guide", "Z"}, {"Agriculture", "Nature", "S"}, {"Military Science", "Instructional", "U"}};
    static final String[] LC_CLASSES = {"Class A – General Works", "Class B – Philosophy. Psychology. Religion",
            "Class C – Auxiliary Sciences of History",
            "Class D – World History and History of Europe, Asia, Africa, Australia, New Zealand, etc.",
            "Class E – History of America", "Class F – Local History of the Americas",
            "Class G – Geography, Anthropology, Recreation", "Class H – Social Sciences", "Class J – Political Science",
            "Class K – Law", "Class L – Education", "Class M – Music", "Class N – Fine Arts",
            "Class P – Language and Literature", "Class Q – Science", "Class R – Medicine", "Class S – Agriculture",
            "Class T – Technology", "Class U – Military Science", "Class V – Naval Science",
            "Class Z – Bibliography, Library Science"};
    static final String[] TEXT_TEMPLATES = {"Introduction to %s", "%s: Principles and Practice", "Fundamentals of %s", "Advanced %s",
            "A Guide to %s", "%s for Beginners", "Essentials of %s", "Modern %s", "Applied %s", "%s: A Student Handbook"};
    static final String[] ADJ = {"Silent", "Hidden", "Last", "Crimson", "Forgotten", "Golden", "Broken", "Distant", "Midnight",
            "Wandering", "Secret", "Burning", "Frozen", "Lonely", "Eternal", "Scarlet", "Hollow", "Restless"};
    static final String[] NOUN = {"Harbor", "Garden", "Letter", "Crown", "Compass", "River", "Lantern", "Voyage", "Archive",
            "Orchard", "Citadel", "Horizon", "Promise", "Melody", "Island", "Mirror", "Kingdom", "Whisper"};
    static final String[][] STORY_GENRES = {{"Adventure"}, {"Fantasy"}, {"Mystery/Detective"}, {"Romance"}, {"Science Fiction"},
            {"Fiction"}, {"Crime"}, {"Drama"}, {"Short Story Collection"}};

    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path data = root.resolve("db/seed-data");
        if (!Files.isDirectory(data)) {
            throw new IllegalStateException("Run this from the project root (db/seed-data not found in " + root + ")");
        }

        List<Dept> depts = new ArrayList<>();
        for (String[] r : readCsv(data.resolve("departments.csv"))) depts.add(new Dept(r[0], r[1]));
        List<Course> courses = new ArrayList<>();
        for (String[] r : readCsv(data.resolve("courses.csv"))) courses.add(new Course(r[0], r[1], r[2]));

        PasswordHasher hasher = new PasswordHasher();
        String adminHash = hasher.hash(ADMIN_PASSWORD.toCharArray());
        String librarianHash = hasher.hash(LIBRARIAN_PASSWORD.toCharArray());

        List<Staff> staff = makeStaff(depts, adminHash, librarianHash);
        List<Student> students = makeStudents(depts, courses);
        List<Item> items = makeItems(data.resolve("catalog.csv"));
        List<Loan> loans = makeLoans(items, students, staff);

        Files.writeString(root.resolve("db/mysql/02_seed.sql"), emit(new MySql(), depts, courses, staff, students, items, loans), StandardCharsets.UTF_8);
        Files.writeString(root.resolve("db/mssql/02_seed.sql"), emit(new MsSql(), depts, courses, staff, students, items, loans), StandardCharsets.UTF_8);

        long open = loans.stream().filter(l -> l.returnedAgo() == null).count();
        long overdue = loans.stream().filter(l -> l.returnedAgo() == null && l.dueAgo() > 0).count();
        System.out.printf("departments=%d courses=%d staff=%d students=%d items=%d (BOOK=%d DIGITAL=%d OTHER=%d) loans=%d (open=%d overdue=%d returned=%d)%n",
                depts.size(), courses.size(), staff.size(), students.size(), items.size(), countType(items, "BOOK"),
                countType(items, "DIGITAL"), countType(items, "OTHER"), loans.size(), open, overdue, loans.size() - open);
    }

    // =====================================================================================
    // generation
    // =====================================================================================

    static List<Staff> makeStaff(List<Dept> depts, String adminHash, String librarianHash) {
        List<Staff> out = new ArrayList<>();
        int id = 2100101;
        out.add(new Staff(id++, "System", null, "Administrator", "Library Administrator", "UWIDE", ADMIN_USERNAME, adminHash, "ADMIN", false, 0));
        out.add(new Staff(id++, "Juan", "D.", "Dela Cruz", "University Librarian", "UWIDE", "librarian", librarianHash, "LIBRARIAN", false, 0));
        Set<String> usernames = new HashSet<>(Set.of(ADMIN_USERNAME, "librarian"));
        for (int i = 0; i < LIBRARIANS - 1; i++) {
            boolean female = R.nextBoolean();
            String first = pick(female ? FIRST_F : FIRST_M);
            String last = pick(LAST);
            String username = uniqueUsername(first, last, usernames);
            Dept d = depts.get(1 + R.nextInt(depts.size() - 1));
            boolean locked = i == 3 || i == 11; // two locked accounts so the Unlock screen has something to unlock
            out.add(new Staff(id++, first, R.nextInt(100) < 70 ? pick(INITIALS) : null, last, pick(POSITIONS), d.code(), username,
                    librarianHash, "LIBRARIAN", locked, locked ? 3 : 0));
        }
        return out;
    }

    static String uniqueUsername(String first, String last, Set<String> taken) {
        String base = (first.substring(0, 1) + last).toLowerCase().replaceAll("[^a-z]", "");
        if (base.length() > 16) base = base.substring(0, 16);
        String name = base;
        for (int n = 2; !taken.add(name); n++) name = base + n;
        return name;
    }

    static List<Student> makeStudents(List<Dept> depts, List<Course> courses) {
        Map<String, List<Course>> byDept = new LinkedHashMap<>();
        for (Course c : courses) byDept.computeIfAbsent(c.dept(), k -> new ArrayList<>()).add(c);
        List<String> studentDepts = new ArrayList<>(byDept.keySet());
        Set<String> numbers = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        List<Student> out = new ArrayList<>();
        while (out.size() < STUDENTS) {
            String number = (2017 + R.nextInt(9)) + "1" + String.format("%05d", R.nextInt(100_000));
            if (!numbers.add(number)) continue;
            boolean female = R.nextBoolean();
            String deptCode = studentDepts.get(R.nextInt(studentDepts.size()));
            List<Course> options = byDept.get(deptCode);
            Course course = options.get(R.nextInt(options.size()));
            String contact;
            do {
                contact = "09" + String.format("%09d", R.nextInt(1_000_000_000));
            } while (!contacts.add(contact));
            LocalDate joined = LocalDate.of(2019, 1, 1).plusDays(R.nextInt(2_100));
            out.add(new Student(number, pick(female ? FIRST_F : FIRST_M), R.nextInt(100) < 80 ? pick(INITIALS) : null, pick(LAST),
                    deptCode, course.code(), (1 + R.nextInt(4)) + "" + "ABCDEFG".charAt(R.nextInt(7)), joined, female ? "F" : "M",
                    R.nextInt(100) < 92 ? contact : null, String.valueOf(1 + R.nextInt(999)), pick(STREETS), pick(BARANGAYS),
                    pick(MUNICIPALITIES), "Bulacan"));
        }
        out.sort(Comparator.comparing(Student::number));
        return out;
    }

    static List<Item> makeItems(Path catalogCsv) throws IOException {
        List<Item> items = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        Set<String> titles = new HashSet<>();
        for (String[] r : readCsv(catalogCsv)) {
            items.add(new Item(r[0], r[1], r[2], blank(r[3]), blank(r[4]), Integer.parseInt(r[5]), blank(r[6]), blank(r[7]),
                    Integer.parseInt(r[8]), Integer.parseInt(r[9]), blank(r[10]), blank(r[11])));
            codes.add(r[0]);
            titles.add(r[2].toLowerCase());
        }
        int[] loanDays = {15, 30, 30, 45, 45};
        for (int i = 0; i < EXTRA_BOOKS; i++) {
            boolean story = i % 4 == 3;
            String[] topic = TOPICS[R.nextInt(TOPICS.length)];
            String title = story ? storyTitle() : String.format(pick(TEXT_TEMPLATES), topic[0]);
            if (!titles.add(title.toLowerCase())) { i--; continue; }
            String genre = story ? pick(STORY_GENRES)[0] : topic[1];
            String lc = story ? lc("P") : lc(topic[2]);
            items.add(new Item(uniqueIsbn(codes), "BOOK", title, writer(), pick(PUBLISHERS), 1995 + R.nextInt(31), genre, lc,
                    120 + R.nextInt(700), loanDays[R.nextInt(loanDays.length)], null, null));
        }
        for (int i = 0; i < EXTRA_DIGITAL; i++) {
            boolean story = i % 9 < 4;
            String[] topic = TOPICS[R.nextInt(TOPICS.length)];
            String title = story ? storyTitle() : String.format(pick(TEXT_TEMPLATES), topic[0]);
            if (!titles.add(title.toLowerCase())) { i--; continue; }
            String code = uniqueDigits(codes, 10);
            items.add(new Item(code, "DIGITAL", title, writer(), null, 1990 + R.nextInt(35), story ? pick(STORY_GENRES)[0] : topic[1], null,
                    80 + R.nextInt(600), loanDays[R.nextInt(loanDays.length)], "https://library.example.edu/ebooks/" + code + ".epub", null));
        }
        for (int i = 0; i < EXTRA_OTHER; i++) {
            String[] topic = TOPICS[R.nextInt(TOPICS.length)];
            String kind = i % 3 == 0 ? "JOURNAL" : i % 3 == 1 ? "MAGAZINE" : "THESIS";
            String title;
            String author = null;
            String publisher;
            int year;
            int pages;
            switch (kind) {
                case "JOURNAL" -> {
                    title = pick(new String[]{"Philippine Journal of %s", "International Journal of %s", "Journal of %s Research"}).formatted(topic[0]);
                    publisher = pick(PUBLISHERS);
                    year = 2005 + R.nextInt(21);
                    pages = 60 + R.nextInt(340);
                }
                case "MAGAZINE" -> {
                    title = pick(new String[]{"%s Today", "The %s Review", "%s Weekly", "%s Monthly"}).formatted(topic[0]);
                    publisher = pick(PUBLISHERS);
                    year = 2018 + R.nextInt(8);
                    pages = 40 + R.nextInt(100);
                }
                default -> {
                    title = i % 2 == 0
                            ? "Design and Development of a " + pick(new String[]{"Inventory", "Enrollment", "Library Management", "Appointment Scheduling",
                                    "Barangay Records", "Point-of-Sale", "Attendance Monitoring"}) + " System for "
                                    + pick(new String[]{"a Public Elementary School", "a Municipal Health Office", "a Small Retail Business", "a Local Government Unit"})
                            : pick(new String[]{"Academic Performance", "Study Habits", "Online Learning Experience", "Digital Literacy", "Reading Habits",
                                    "Financial Literacy", "Stress Levels", "Career Preferences"}) + " of "
                                    + pick(new String[]{"Senior High School Students", "College Freshmen", "Working Students", "Public School Teachers",
                                            "Library Users", "Nursing Students", "Engineering Students"}) + " in " + pick(new String[]{"Bulacan", "Malolos City", "Central Luzon"})
                                    + ": A Descriptive Study";
                    author = pick(LAST) + ", " + pick(R.nextBoolean() ? FIRST_F : FIRST_M);
                    publisher = R.nextBoolean() ? "Bulacan State University" : null;
                    year = 2008 + R.nextInt(18);
                    pages = 80 + R.nextInt(370);
                }
            }
            if (!titles.add(title.toLowerCase())) { i--; continue; }
            String code = kind.equals("MAGAZINE") ? uniqueDigits(codes, 13, "977") : uniqueDigits(codes, 12);
            items.add(new Item(code, "OTHER", title, author, publisher, year, null, null, pages, 0, null, kind));
        }
        return items;
    }

    /**
     * Loans are built per item as a chain going back in time, so a single printed book is never
     * lent to two people at once. A few items end with a loan that is still open (some of them overdue).
     */
    static List<Loan> makeLoans(List<Item> items, List<Student> students, List<Staff> staff) {
        List<Staff> librarians = staff.stream().filter(s -> s.role().equals("LIBRARIAN")).toList();
        List<Item> printed = new ArrayList<>(items.stream().filter(i -> i.type.equals("BOOK")).toList());
        List<Item> digital = new ArrayList<>(items.stream().filter(i -> i.type.equals("DIGITAL")).toList());
        java.util.Collections.shuffle(printed, R);
        java.util.Collections.shuffle(digital, R);

        Set<String> openItems = new HashSet<>();
        printed.stream().limit(OPEN_BOOK_LOANS).forEach(i -> openItems.add(i.code));
        digital.stream().limit(OPEN_DIGITAL_LOANS).forEach(i -> openItems.add(i.code));

        List<Loan> draft = new ArrayList<>();
        List<Item> lendable = new ArrayList<>(printed);
        lendable.addAll(digital);
        for (Item item : lendable) {
            boolean open = openItems.contains(item.code);
            boolean active = open || R.nextInt(100) < 62;
            if (!active) continue;

            long anchor; // seconds before "now" at which the newest closed loan ended, or the open loan started
            if (open) {
                boolean overdue = R.nextInt(100) < OVERDUE_SHARE_PERCENT;
                long issuedAgo = overdue
                        ? (item.days + 2 + R.nextInt(60)) * DAY + R.nextInt((int) DAY)
                        : (long) R.nextInt(Math.max(1, item.days - 1)) * DAY + R.nextInt((int) DAY);
                draft.add(new Loan(0, item.code, pick(students).number(), pick(librarians).id(), issuedAgo, issuedAgo - item.days * DAY, null));
                if (item.type.equals("BOOK")) item.available = false;
                anchor = issuedAgo;
            } else {
                anchor = (3 + R.nextInt(300)) * DAY + R.nextInt((int) DAY);
                if (!addReturned(draft, item, students, librarians, anchor)) continue;
            }
            int history = R.nextInt(5);
            long cursor = anchor;
            for (int k = 0; k < history; k++) {
                long returnedAgo = cursor + (1 + R.nextInt(40)) * DAY + R.nextInt((int) DAY);
                if (!addReturned(draft, item, students, librarians, returnedAgo)) break;
                cursor = draft.get(draft.size() - 1).issuedAgo();
            }
        }

        draft.sort(Comparator.comparingLong(Loan::issuedAgo).reversed()); // oldest first
        List<Loan> out = new ArrayList<>();
        int id = 1000;
        for (Loan l : draft) out.add(new Loan(id++, l.item(), l.student(), l.staff(), l.issuedAgo(), l.dueAgo(), l.returnedAgo()));
        return out;
    }

    static boolean addReturned(List<Loan> draft, Item item, List<Student> students, List<Staff> librarians, long returnedAgo) {
        long held = (1 + R.nextInt(Math.max(1, item.days + 14))) * DAY + R.nextInt((int) DAY);
        long issuedAgo = returnedAgo + held;
        if (issuedAgo > 520 * DAY) return false; // keep the history to roughly 17 months
        long dueAgo = issuedAgo - item.days * DAY;
        draft.add(new Loan(0, item.code, pick(students).number(), pick(librarians).id(), issuedAgo, dueAgo, returnedAgo));
        return true;
    }

    static String storyTitle() {
        return "The " + pick(ADJ) + " " + pick(NOUN);
    }

    static String writer() {
        String one = pick(WRITER_FIRST) + " " + pick(WRITER_LAST);
        return R.nextInt(100) < 18 ? one + " and " + pick(WRITER_FIRST) + " " + pick(WRITER_LAST) : one;
    }

    static String lc(String letter) {
        return Arrays.stream(LC_CLASSES).filter(c -> c.startsWith("Class " + letter + " ")).findFirst().orElseThrow();
    }

    static String uniqueIsbn(Set<String> taken) {
        while (true) {
            StringBuilder b = new StringBuilder("978");
            for (int i = 0; i < 9; i++) b.append(R.nextInt(10));
            int sum = 0;
            for (int i = 0; i < 12; i++) sum += (b.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
            b.append((10 - sum % 10) % 10);
            if (taken.add(b.toString())) return b.toString();
        }
    }

    static String uniqueDigits(Set<String> taken, int length) {
        return uniqueDigits(taken, length, "");
    }

    static String uniqueDigits(Set<String> taken, int length, String prefix) {
        while (true) {
            StringBuilder b = new StringBuilder(prefix);
            while (b.length() < length) b.append(b.length() == 0 ? 1 + R.nextInt(9) : R.nextInt(10));
            if (taken.add(b.toString())) return b.toString();
        }
    }

    static <T> T pick(T[] options) {
        return options[R.nextInt(options.length)];
    }

    static <T> T pick(List<T> options) {
        return options.get(R.nextInt(options.size()));
    }

    static long countType(List<Item> items, String type) {
        return items.stream().filter(i -> i.type.equals(type)).count();
    }

    static String blank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    // =====================================================================================
    // SQL output
    // =====================================================================================

    interface Dialect {
        String text(String s);

        String bool(boolean b);

        /** Timestamp {@code seconds} before the moment the script runs (negative = in the future). */
        String ago(long seconds);

        String name();

        void identityInsert(StringBuilder out, String table, boolean on);

        String batchSeparator();
    }

    static final class MySql implements Dialect {
        public String text(String s) { return s == null ? "NULL" : "'" + s.replace("\\", "\\\\").replace("'", "''") + "'"; }
        public String bool(boolean b) { return b ? "TRUE" : "FALSE"; }
        public String ago(long seconds) { return "DATE_SUB(NOW(), INTERVAL " + seconds + " SECOND)"; }
        public String name() { return "MySQL"; }
        public void identityInsert(StringBuilder out, String table, boolean on) { }
        public String batchSeparator() { return ";\n"; }
    }

    static final class MsSql implements Dialect {
        public String text(String s) { return s == null ? "NULL" : "N'" + s.replace("'", "''") + "'"; }
        public String bool(boolean b) { return b ? "1" : "0"; }
        public String ago(long seconds) { return "DATEADD(SECOND, " + (-seconds) + ", SYSDATETIME())"; }
        public String name() { return "SQL Server"; }
        public void identityInsert(StringBuilder out, String table, boolean on) {
            out.append("SET IDENTITY_INSERT ").append(table).append(on ? " ON;\n" : " OFF;\n");
        }
        public String batchSeparator() { return ";\n"; }
    }

    static String emit(Dialect d, List<Dept> depts, List<Course> courses, List<Staff> staff, List<Student> students,
                       List<Item> items, List<Loan> loans) {
        StringBuilder out = new StringBuilder(1 << 20);
        out.append("-- BulSU Library Management System - demo data for ").append(d.name()).append(".\n");
        out.append("-- GENERATED by db/tools/SeedGenerator.java - do not edit by hand; regenerate instead.\n");
        out.append("--\n");
        out.append("-- Every person below is synthetic (randomly combined names, numbers and addresses) for Data Privacy\n");
        out.append("-- Act compliance. Book titles and ISBNs are bibliographic data, not personal data.\n");
        out.append("--\n");
        out.append("-- Demo sign-ins (change them before any real use):\n");
        out.append("--     ").append(ADMIN_USERNAME).append(" / ").append(ADMIN_PASSWORD).append("      (administrator)\n");
        out.append("--     librarian / ").append(LIBRARIAN_PASSWORD).append("   (librarian; every seeded librarian uses this password)\n");
        out.append("-- Run after 01_schema.sql. Loan dates are relative to the moment this script runs.\n\n");

        insert(out, d, "department", List.of("code", "name"),
                depts.stream().map(x -> List.of(d.text(x.code()), d.text(x.name()))).toList());
        insert(out, d, "course", List.of("code", "name", "department_code"),
                courses.stream().map(x -> List.of(d.text(x.code()), d.text(x.name()), d.text(x.dept()))).toList());

        d.identityInsert(out, "staff", true);
        insert(out, d, "staff", List.of("id", "first_name", "middle_initial", "last_name", "position", "department_code",
                        "username", "password_hash", "role", "locked", "failed_attempts"),
                staff.stream().map(x -> List.of(String.valueOf(x.id()), d.text(x.first()), d.text(x.mi()), d.text(x.last()),
                        d.text(x.position()), d.text(x.dept()), d.text(x.username()), d.text(x.hash()), d.text(x.role()),
                        d.bool(x.locked()), String.valueOf(x.failed()))).toList());
        d.identityInsert(out, "staff", false);

        insert(out, d, "student", List.of("student_number", "first_name", "middle_initial", "last_name", "department_code",
                        "course_code", "section", "membership_date", "gender", "contact_number", "house_no", "street", "barangay",
                        "municipality", "province"),
                students.stream().map(x -> List.of(d.text(x.number()), d.text(x.first()), d.text(x.mi()), d.text(x.last()),
                        d.text(x.dept()), d.text(x.course()), d.text(x.section()), d.text(x.joined().toString()), d.text(x.gender()),
                        d.text(x.contact()), d.text(x.houseNo()), d.text(x.street()), d.text(x.barangay()),
                        d.text(x.municipality()), d.text(x.province()))).toList());

        insert(out, d, "item", List.of("code", "item_type", "title", "author", "publisher", "publication_year", "genre", "lc_class",
                        "pages", "borrow_days", "url", "material_type", "available"),
                items.stream().map(x -> List.of(d.text(x.code), d.text(x.type), d.text(x.title), d.text(x.author), d.text(x.publisher),
                        String.valueOf(x.year), d.text(x.genre), d.text(x.lcClass), String.valueOf(x.pages), String.valueOf(x.days),
                        d.text(x.url), d.text(x.material), d.bool(x.available))).toList());

        d.identityInsert(out, "loan", true);
        insert(out, d, "loan", List.of("id", "item_code", "student_number", "issued_by", "issued_at", "due_at", "returned_at"),
                loans.stream().map(x -> List.of(String.valueOf(x.id()), d.text(x.item()), d.text(x.student()), String.valueOf(x.staff()),
                        d.ago(x.issuedAgo()), d.ago(x.dueAgo()), x.returnedAgo() == null ? "NULL" : d.ago(x.returnedAgo()))).toList());
        d.identityInsert(out, "loan", false);
        return out.toString();
    }

    static void insert(StringBuilder out, Dialect d, String table, List<String> columns, List<List<String>> rows) {
        final int batch = 200; // SQL Server allows at most 1000 rows per VALUES list
        out.append("-- ").append(table).append(" (").append(rows.size()).append(" rows)\n");
        for (int start = 0; start < rows.size(); start += batch) {
            out.append("INSERT INTO ").append(table).append(" (").append(String.join(", ", columns)).append(") VALUES\n");
            List<List<String>> slice = rows.subList(start, Math.min(rows.size(), start + batch));
            for (int i = 0; i < slice.size(); i++) {
                out.append("  (").append(String.join(", ", slice.get(i))).append(i == slice.size() - 1 ? ")" : "),\n");
            }
            out.append(d.batchSeparator());
        }
        out.append('\n');
    }

    // =====================================================================================
    // minimal RFC 4180 CSV reader (header row is skipped)
    // =====================================================================================

    static List<String[]> readCsv(Path file) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cell.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { row.add(cell.toString()); cell.setLength(0); }
            else if (c == '\n') { row.add(cell.toString()); cell.setLength(0); rows.add(row.toArray(new String[0])); row = new ArrayList<>(); }
            else if (c != '\r') cell.append(c);
        }
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); rows.add(row.toArray(new String[0])); }
        return rows.subList(1, rows.size());
    }
}
