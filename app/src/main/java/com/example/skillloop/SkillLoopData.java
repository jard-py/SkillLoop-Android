package com.example.skillloop;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SkillLoopData {

    private static final String PREFS = "skillloop_shared_data";
    private static final String PROFILE_PREFS = "skillloop_my_profile";

    private static final String ACCEPTED = "accepted_ids";
    private static final String PASSED = "passed_ids";

    private SkillLoopData() {
    }

    public static class Student {
        public final String id;
        public final String name;
        public final String course;
        public final String year;
        public final String teaches;
        public final String wants;
        public final String domain;
        public final String bio;
        public final String project;
        public final String interests;
        public final String photoUrl;

        public Student(
                String id, String name, String course, String year,
                String teaches, String wants, String domain, String bio,
                String project, String interests) {
            this(id, name, course, year, teaches, wants, domain, bio, project, interests, "");
        }

        public Student(
                String id, String name, String course, String year,
                String teaches, String wants, String domain, String bio,
                String project, String interests, String photoUrl) {
            this.id = id;
            this.name = name;
            this.course = course;
            this.year = year;
            this.teaches = teaches;
            this.wants = wants;
            this.domain = domain;
            this.bio = bio;
            this.project = project;
            this.interests = interests;
            this.photoUrl = photoUrl == null ? "" : photoUrl;
        }
    }

    public static List<Student> getStudents() {
        List<Student> students = new ArrayList<>();

        students.add(new Student("aarav", "Aarav Mehta",
                "Computer Engineering", "Year 3",
                "Python, Machine Learning, Excel", "UI/UX Design",
                "Technology", "Building things. Learning constantly.",
                "A campus discovery app", "Startups, Design, New technology"));

        students.add(new Student("mira", "Mira Shah",
                "Communication Design", "Year 2",
                "Figma, Illustration", "Frontend Development",
                "Design", "Exploring how design can make technology feel more human.",
                "An interactive design portfolio", "Illustration, Branding, Creative coding"));

        students.add(new Student("ishaan", "Ishaan Rao",
                "Management Studies", "Year 4",
                "Excel, Presentations", "Python, Data Analytics",
                "Business", "Interested in turning ideas into useful products.",
                "A student startup concept", "Entrepreneurship, Analytics, Product strategy"));

        students.add(new Student("ananya", "Ananya Kulkarni",
                "Information Technology", "Year 1",
                "Video Editing, Canva", "Web Development",
                "Technology", "Learning by making small creative projects.",
                "A campus video series", "Video, Content creation, Web design"));

        students.add(new Student("kabir", "Kabir Desai",
                "Electronics Engineering", "Year 3",
                "Arduino, C++", "Product Design",
                "Technology", "Interested in building things that solve real problems.",
                "A small hardware prototype", "Robotics, Product design, Hardware"));

        students.add(new Student("rhea", "Rhea Kapoor",
                "Business Administration", "Year 2",
                "Marketing, Research", "Graphic Design",
                "Business", "Curious about brands, people, and ideas.",
                "A student-led brand concept", "Marketing, Branding, Research"));

        students.add(new Student("aditi", "Aditi Sharma",
                "Artificial Intelligence", "Year 2",
                "Python, Data Visualization", "Public Speaking, UI/UX Design",
                "Technology", "Turning messy datasets into stories people can understand.",
                "An interactive student performance dashboard", "AI, Data storytelling, Design"));

        students.add(new Student("vihaan", "Vihaan Patel",
                "Mechanical Engineering", "Year 3",
                "AutoCAD, 3D Modelling", "Arduino, Python",
                "Engineering", "I love figuring out how things work and building prototypes.",
                "A low-cost automated plant watering system", "3D Printing, Robotics, DIY projects"));

        students.add(new Student("sana", "Sana Khan",
                "Mass Media", "Year 2",
                "Photography, Video Editing", "Graphic Design, Marketing",
                "Media", "Always carrying a camera and looking for a story worth telling.",
                "A short documentary about student life", "Film, Photography, Storytelling"));

        students.add(new Student("dev", "Dev Malhotra",
                "Computer Engineering", "Year 1",
                "HTML, CSS, JavaScript", "Backend Development, Git",
                "Technology", "Building my first real projects one commit at a time.",
                "A personal portfolio website", "Web Development, Open Source, Gaming"));

        students.add(new Student("prisha", "Prisha Nair",
                "Finance", "Year 3",
                "Excel, Financial Modelling", "Python, Data Analytics",
                "Business", "Making financial concepts easier to understand.",
                "A personal finance tracker for students", "FinTech, Investing, Entrepreneurship"));

        students.add(new Student("arjun", "Arjun Verma",
                "Electronics and Telecommunications", "Year 2",
                "C++, Circuit Design", "Machine Learning, Robotics",
                "Engineering", "Interested in the intersection of hardware and intelligent systems.",
                "A sensor-based smart room prototype", "IoT, Embedded Systems, Robotics"));

        students.add(new Student("kiara", "Kiara Fernandes",
                "Communication Design", "Year 3",
                "Illustration, Branding, Figma", "3D Design, Animation",
                "Design", "I like making complicated ideas look simple and beautiful.",
                "A visual identity for a student-led initiative", "Typography, Art, Motion Design"));

        students.add(new Student("rohan", "Rohan Iyer",
                "Data Science", "Year 2",
                "SQL, Python, Statistics", "Machine Learning, Storytelling",
                "Technology", "Finding patterns in data and asking better questions.",
                "A campus food preference analysis", "Analytics, Cricket, Visualization"));

        students.add(new Student("meera", "Meera Joshi",
                "Psychology", "Year 3",
                "Research, Presentation Design", "Data Analysis, Video Editing",
                "Research", "Curious about human behaviour and why people make decisions.",
                "A student wellbeing research project", "Psychology, Behaviour, Research"));

        students.add(new Student("yash", "Yash Sethi",
                "Management Studies", "Year 1",
                "Public Speaking, Canva, Social Media", "Finance, Product Management",
                "Business", "I enjoy bringing people together and turning ideas into action.",
                "A campus event discovery concept", "Startups, Marketing, Community Building"));

        students.add(new Student("zoya", "Zoya Merchant",
                "Information Technology", "Year 4",
                "Java, Android Development, Git", "Cloud Computing, UI/UX Design",
                "Technology", "Building useful mobile apps and making them easier to use.",
                "A collaborative campus utility app", "Mobile Apps, Product Design, Technology"));

        return students;
    }

    public static Student getStudent(String id) {
        for (Student student : getStudents()) {
            if (student.id.equals(id)) {
                return student;
            }
        }
        return null;
    }

    public static Student getMyProfile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                PROFILE_PREFS, Context.MODE_PRIVATE);

        String name = prefs.getString("name", "").trim();

        if (name.isEmpty()) {
            return null;
        }

        String course = prefs.getString("course", "");
        String year = prefs.getString("year", "");
        String teaches = prefs.getString("teaches", "");
        String wants = prefs.getString("wants", "");
        String project = prefs.getString("project", "");
        String domain = prefs.getString("domain",
                inferDomain(course, teaches, wants));
        String bio = prefs.getString("bio",
                "Learning, sharing, and building together.");
        String photoPath = prefs.getString("photo_path", "");
        String interests = prefs.getString("interests", "");
        String photoUrl = prefs.getString("photo_url", "");

        return new Student(
                "me", name,
                course.isEmpty() ? "Course not added" : course,
                year.isEmpty() ? "Year not added" : year,
                teaches, wants, domain, bio,
                project.isEmpty() ? "Project not added yet" : project,
                interests, photoUrl.isEmpty() ? photoPath : photoUrl
        );
    }

    // Existing signature retained for compatibility.
    public static void saveMyProfile(
            Context context,
            String name,
            String course,
            String year,
            String teaches,
            String wants,
            String project) {

        SharedPreferences prefs = context.getSharedPreferences(
                PROFILE_PREFS, Context.MODE_PRIVATE);

        String domain = prefs.getString("domain",
                inferDomain(course, teaches, wants));
        String bio = prefs.getString("bio",
                "Learning, sharing, and building together.");
        String photoPath = prefs.getString("photo_path", "");

        saveMyProfile(context, name, course, year, teaches,
                wants, project, domain, bio, photoPath);
    }

    public static void saveMyProfile(
            Context context,
            String name,
            String course,
            String year,
            String teaches,
            String wants,
            String project,
            String domain,
            String bio,
            String photoPath) {

        context.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("name", name.trim())
                .putString("course", course.trim())
                .putString("year", year.trim())
                .putString("teaches", teaches.trim())
                .putString("wants", wants.trim())
                .putString("project", project.trim())
                .putString("domain", domain.trim())
                .putString("bio", bio.trim())
                .putString("photo_path", photoPath == null ? "" : photoPath)
                .apply();
    }

    private static String inferDomain(
            String course, String teaches, String wants) {

        String all = (course + " " + teaches + " " + wants)
                .toLowerCase();

        if (all.contains("design") || all.contains("figma")
                || all.contains("illustration")) {
            return "Design";
        }

        if (all.contains("business") || all.contains("management")
                || all.contains("marketing") || all.contains("finance")
                || all.contains("commerce")) {
            return "Business";
        }

        if (all.contains("computer") || all.contains("information technology")
                || all.contains("artificial intelligence")
                || all.contains("machine learning")
                || all.contains("programming") || all.contains("python")) {
            return "Technology";
        }

        return "Other";
    }

    public static Set<String> getAcceptedIds(Context context) {
        SharedPreferences shared = getPreferences(context);
        Set<String> saved = shared.getStringSet(ACCEPTED, null);

        if (saved == null) {
            Set<String> oldAccepted = context.getSharedPreferences(
                            "skillloop_connect", Context.MODE_PRIVATE)
                    .getStringSet("accepted_ids", new HashSet<>());

            saved = new HashSet<>(oldAccepted);
            shared.edit().putStringSet(ACCEPTED, saved).apply();
        }

        return new HashSet<>(saved);
    }

    public static Set<String> getPassedIds(Context context) {
        SharedPreferences shared = getPreferences(context);
        Set<String> saved = shared.getStringSet(PASSED, null);

        if (saved == null) {
            Set<String> oldPassed = context.getSharedPreferences(
                            "skillloop_connect", Context.MODE_PRIVATE)
                    .getStringSet("passed_ids", new HashSet<>());

            saved = new HashSet<>(oldPassed);
            shared.edit().putStringSet(PASSED, saved).apply();
        }

        return new HashSet<>(saved);
    }

    public static void acceptConnection(Context context, String studentId) {
        Set<String> accepted = getAcceptedIds(context);
        Set<String> passed = getPassedIds(context);

        accepted.add(studentId);
        passed.remove(studentId);

        saveDecisions(context, accepted, passed);
    }

    public static void passConnection(Context context, String studentId) {
        Set<String> accepted = getAcceptedIds(context);
        Set<String> passed = getPassedIds(context);

        accepted.remove(studentId);
        passed.add(studentId);

        saveDecisions(context, accepted, passed);
    }

    public static void saveDecisions(
            Context context, Set<String> accepted, Set<String> passed) {

        getPreferences(context)
                .edit()
                .putStringSet(ACCEPTED, new HashSet<>(accepted))
                .putStringSet(PASSED, new HashSet<>(passed))
                .apply();

        context.getSharedPreferences(
                        "skillloop_connect", Context.MODE_PRIVATE)
                .edit()
                .putStringSet("accepted_ids", new HashSet<>(accepted))
                .putStringSet("passed_ids", new HashSet<>(passed))
                .apply();
    }

    public static List<Student> getConnections(Context context) {
        Set<String> accepted = getAcceptedIds(context);
        List<Student> connections = new ArrayList<>();

        for (Student student : getStudents()) {
            if (accepted.contains(student.id)) {
                connections.add(student);
            }
        }

        return connections;
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
