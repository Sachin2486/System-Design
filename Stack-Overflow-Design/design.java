import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class StackOverflow {

    private static final AtomicLong idGenerator =
            new AtomicLong(1);

    enum VoteType {
        UPVOTE,
        DOWNVOTE
    }

    static class User {

        long id;
        String name;
        int reputation;

        public User(String name) {
            this.id = idGenerator.getAndIncrement();
            this.name = name;
        }

        public void addReputation(int points) {
            reputation += points;
        }

        @Override
        public String toString() {
            return name +
                    " (Rep=" +
                    reputation +
                    ")";
        }
    }

    static class Tag {

        String value;

        public Tag(String value) {
            this.value = value.toLowerCase();
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object o) {

            if (!(o instanceof Tag))
                return false;

            Tag t = (Tag) o;

            return value.equals(t.value);
        }
    }

    static class Comment {

        long id;

        User author;

        String text;

        LocalDateTime createdAt;

        public Comment(User author,
                       String text) {

            id = idGenerator.getAndIncrement();

            this.author = author;

            this.text = text;

            createdAt = LocalDateTime.now();
        }
    }

    static class Vote {

        User voter;

        VoteType type;

        public Vote(User voter,
                    VoteType type) {

            this.voter = voter;

            this.type = type;
        }
    }


    abstract static class Post {

        long id;

        User author;

        List<Comment> comments =
                new ArrayList<>();

        Map<Long, Vote> votes =
                new HashMap<>();

        LocalDateTime createdAt;

        protected Post(User author) {

            this.author = author;

            id = idGenerator.getAndIncrement();

            createdAt = LocalDateTime.now();
        }

        public void addComment(Comment c) {

            comments.add(c);

            c.author.addReputation(1);
        }

        public void vote(User user,
                         VoteType type) {

            if (votes.containsKey(user.id)) {

                System.out.println(
                        "Already voted");

                return;
            }

            votes.put(user.id,
                    new Vote(user, type));

            if (type ==
                    VoteType.UPVOTE) {

                author.addReputation(10);

            }
            else {

                author.addReputation(-2);

            }

        }

        public int score() {

            int score = 0;

            for (Vote v :
                    votes.values()) {

                score += v.type ==
                        VoteType.UPVOTE
                        ? 1 : -1;

            }

            return score;
        }

    }


    static class Answer
            extends Post {

        String content;

        Question question;

        public Answer(User author,
                      String content,
                      Question q) {

            super(author);

            this.content = content;

            this.question = q;

            author.addReputation(10);
        }

        @Override
        public String toString() {

            return "Answer("
                    + content
                    + ") Score="
                    + score();

        }

    }


    static class Question
            extends Post {

        String title;

        String description;

        Set<Tag> tags =
                new HashSet<>();

        List<Answer> answers =
                new ArrayList<>();

        public Question(

                User author,

                String title,

                String description,

                Set<Tag> tags) {

            super(author);

            this.title = title;

            this.description =
                    description;

            this.tags.addAll(tags);

            author.addReputation(5);

        }

        public void addAnswer(
                Answer answer) {

            answers.add(answer);

        }

        @Override
        public String toString() {

            return "Question{"

                    + title

                    + ", score="

                    + score()

                    +

                    "}";

        }

    }


    static class SearchService {

        Map<String,

                Set<Question>>

                tagIndex =

                new ConcurrentHashMap<>();


        Map<String,

                Set<Question>>

                keywordIndex =

                new ConcurrentHashMap<>();


        Map<Long,

                Set<Question>>

                userIndex =

                new ConcurrentHashMap<>();


        public void index(
                Question q) {

            for (Tag tag :
                    q.tags) {

                tagIndex

                        .computeIfAbsent(

                                tag.value,

                                k ->

                                        new HashSet<>())

                        .add(q);

            }

            userIndex

                    .computeIfAbsent(

                            q.author.id,

                            k ->

                                    new HashSet<>())

                    .add(q);


            String[] words =

                    (q.title + " "

                            + q.description)

                            .toLowerCase()

                            .split("\\s+");


            for (String word :

                    words) {

                keywordIndex

                        .computeIfAbsent(

                                word,

                                k ->

                                        new HashSet<>())

                        .add(q);

            }

        }


        public Set<Question>

        searchByTag(

                String tag) {

            return tagIndex

                    .getOrDefault(

                            tag.toLowerCase(),

                            Collections.emptySet());

        }


        public Set<Question>

        searchByKeyword(

                String keyword) {

            return keywordIndex

                    .getOrDefault(

                            keyword.toLowerCase(),

                            Collections.emptySet());

        }


        public Set<Question>

        searchByUser(

                User user) {

            return userIndex

                    .getOrDefault(

                            user.id,

                            Collections.emptySet());

        }

    }


    static class StackOverflowService {

        Map<Long,

                User>

                users =

                new HashMap<>();


        Map<Long,

                Question>

                questions =

                new HashMap<>();


        SearchService search =

                new SearchService();


        public User createUser(

                String name) {

            User u =

                    new User(name);

            users.put(

                    u.id,

                    u);

            return u;

        }


        public Question askQuestion(

                User author,

                String title,

                String description,

                Set<Tag> tags) {

            Question q =

                    new Question(

                            author,

                            title,

                            description,

                            tags);

            questions.put(

                    q.id,

                    q);

            search.index(q);

            return q;

        }


        public Answer answerQuestion(

                User author,

                Question question,

                String answer) {

            Answer a =

                    new Answer(

                            author,

                            answer,

                            question);

            question.addAnswer(a);

            return a;

        }


        public void comment(

                Post post,

                User user,

                String text) {

            post.addComment(

                    new Comment(

                            user,

                            text));

        }


        public void vote(

                Post post,

                User user,

                VoteType type) {

            post.vote(

                    user,

                    type);

        }

    }



    public static void main(
            String[] args) {

        StackOverflowService service =

                new StackOverflowService();


        User alice =

                service.createUser(

                        "Alice");


        User bob =

                service.createUser(

                        "Bob");


        User charlie =

                service.createUser(

                        "Charlie");


        Question q =

                service.askQuestion(

                        alice,

                        "What is HashMap?",

                        "Need explanation",

                        Set.of(

                                new Tag("Java"),

                                new Tag("Collections")

                        )

                );


        service.vote(

                q,

                bob,

                VoteType.UPVOTE);


        service.vote(

                q,

                charlie,

                VoteType.UPVOTE);


        Answer a =

                service.answerQuestion(

                        bob,

                        q,

                        "HashMap stores key value pairs"

                );


        service.vote(

                a,

                alice,

                VoteType.UPVOTE);


        service.comment(

                q,

                charlie,

                "Interesting question");


        System.out.println(q);

        System.out.println(a);



        System.out.println();

        System.out.println(

                "Java Questions");



        service.search

                .searchByTag(

                        "java")

                .forEach(

                        System.out::println);



        System.out.println();

        System.out.println(

                "Users");


        System.out.println(alice);

        System.out.println(bob);

        System.out.println(charlie);

    }

}