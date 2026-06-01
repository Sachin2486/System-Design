
// Problem
// File System API Implementation

// Design an in-memory file system that mimics basic Linux directory operations.

// Required APIs:

// mkdir(dirname: string)
// pwd()
// cd(path: string)
// Functional requirements

// mkdir(dirname)
// Creates a new directory inside the current working directory.

// pwd()
// Returns the absolute path of the current working directory.

// cd(path)
// Changes the current working directory.

// Path rules

// If the path starts with /, it represents an absolute path from root
// Otherwise it is a relative path from the current directory
// The path may contain a wildcard *.

// * can match:

// the current directory (.)
// the parent directory (..)
// any child directory
// The APIs should behave similarly to Linux directory navigation commands.


////// --------   END ---------- /////

import java.util.*;

class Directory {
    private final String name;
    private final Directory parent;
    
    //O(1) child lookup 
    private final Map<String, Directory> children;
    
    public Directory(String name, Directory parent) {
        this.name = name;
        this.parent = parent;
        this.children = new HashMap<>();
    }
    
    public String getName() {
        return name;
    }
    
    public Directory getParent() {
        return parent;
    }
    
    public Map<String, Directory> getChildren() {
        return children;
    }
    
    public void addChild(Directory child) {
        children.put(child.getName(), child);
    }
}


class FileSystem {
    
    private final Directory root;
    private Directory current;
    
    public FileSystem() {
        root = new Directory("/", null);
        current = root;
    }
    
    public void mkdir(String dirName) {
        if(current.getChildren().containsKey(dirName)) {
            throw new IllegalArgumentException(
                "Directory already exists: " + dirName
                );
        }
        
        Directory child = new Directory(dirName, current);
        current.addChild(child);
    }
    
     public String pwd() {

        if (current == root) {
            return "/";
        }

        Deque<String> stack = new ArrayDeque<>();

        Directory node = current;

        while (node != root) {
            stack.push(node.getName());
            node = node.getParent();
        }

        StringBuilder path = new StringBuilder();

        while (!stack.isEmpty()) {
            path.append("/").append(stack.pop());
        }

        return path.toString();
    }
    
    public void cd(String path) {

        if (path == null || path.isEmpty()) {
            return;
        }

        Directory node;

        if (path.startsWith("/")) {
            node = root;
        } else {
            node = current;
        }

        String[] parts = path.split("/");

        for (String part : parts) {

            if (part.isEmpty()) {
                continue;
            }

            // .
            if (part.equals(".")) {
                continue;
            }

            // ..
            if (part.equals("..")) {
                if (node.getParent() != null) {
                    node = node.getParent();
                }
                continue;
            }

            // *
            if (part.equals("*")) {

                if (!node.getChildren().isEmpty()) {

                    // deterministic first child
                    node = node.getChildren()
                            .values()
                            .iterator()
                            .next();
                }
                else if (node.getParent() != null) {
                    node = node.getParent();
                }

                continue;
            }

            Directory next =
                    node.getChildren().get(part);

            if (next == null) {
                throw new IllegalArgumentException(
                        "Invalid path: " + path);
            }

            node = next;
        }

        current = node;
    }
    
}


public class Main
{
	public static void main(String[] args) {
		FileSystem fs = new FileSystem();

        fs.mkdir("home");
        fs.cd("/home");

        fs.mkdir("docs");
        fs.mkdir("photos");

        fs.cd("docs");

        System.out.println(fs.pwd());

        fs.cd("..");

        System.out.println(fs.pwd());

        fs.cd("*");

        System.out.println(fs.pwd());
	}
}
