Requirement : Implement core Linux File System commands (e.g., mkdir, cd, ls, pwd).


import java.util.*;

/*
 =========================================================
 Linux File System Commands (mkdir, cd, ls, pwd)
 =========================================================
*/

/* ================= NODE ================= */
class Node {
    String name;
    Map<String, Node> children;
    Node parent;

    Node(String name, Node parent) {
        this.name = name;
        this.parent = parent;
        this.children = new HashMap<>();
    }
}

/* ================= FILE SYSTEM ================= */
class FileSystem {

    private final Node root;
    private Node current;

    FileSystem() {
        root = new Node("/", null);
        current = root;
    }

    /* -------- MKDIR -------- */
    public void mkdir(String path) {
        Node node = resolve(path, true);
    }

    /* -------- LS -------- */
    public List<String> ls(String path) {
        Node node = resolve(path, false);
        List<String> res = new ArrayList<>(node.children.keySet());
        Collections.sort(res);
        return res;
    }

    /* -------- CD -------- */
    public void cd(String path) {
        Node node = resolve(path, false);
        current = node;
    }

    /* -------- PWD -------- */
    public String pwd() {
        if (current == root) return "/";

        List<String> parts = new ArrayList<>();
        Node temp = current;

        while (temp != null && temp != root) {
            parts.add(temp.name);
            temp = temp.parent;
        }

        Collections.reverse(parts);
        return "/" + String.join("/", parts);
    }

    /* -------- PATH RESOLUTION -------- */
    private Node resolve(String path, boolean create) {

        String[] parts = path.split("/");
        Node node = path.startsWith("/") ? root : current;

        for (String part : parts) {

            if (part.isEmpty() || part.equals(".")) continue;

            if (part.equals("..")) {
                if (node.parent != null) node = node.parent;
            } else {
                node.children.putIfAbsent(part, create ? new Node(part, node) : null);

                if (!node.children.containsKey(part) || node.children.get(part) == null) {
                    throw new RuntimeException("Path not found: " + part);
                }

                node = node.children.get(part);
            }
        }
        return node;
    }
}

public class Main {
    public static void main(String[] args) {

        FileSystem fs = new FileSystem();

        fs.mkdir("/a/b/c");
        fs.cd("/a/b");

        System.out.println("PWD: " + fs.pwd());   // /a/b

        fs.mkdir("d");
        System.out.println("LS /a/b: " + fs.ls("/a/b"));

        fs.cd("..");
        System.out.println("PWD: " + fs.pwd());   // /a
    }
}