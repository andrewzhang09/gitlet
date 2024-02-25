package gitlet;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import static gitlet.Utils.*;

import static gitlet.Utils.join;

/** Represents a gitlet commit object.
 *
 *  @author Andrew Zhang
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    public static final File COMMITS_DIR = join(Repository.GITLET_DIR, "commits");
    /** The message of this Commit. */
    private String message;
    private String timestamp;
    private String parent;
    private String parent2;
    private String commitID;
    private TreeMap<String, String> commitFiles;

    public Commit(String message, String parent, String parent2, TreeMap<String, String> files) {
        DateFormat formatted = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        if (parent == null) {
            Date firstcommitDate = new Date(0);
            this.timestamp = formatted.format(firstcommitDate);
        } else {
            Date date = new Date();
            this.timestamp = formatted.format(date);
        }
        this.message = message;
        this.parent = parent;
        this.parent2 = parent2;
        this.commitFiles = files;
    }

    public TreeMap<String, String> getCommitFiles() {
        return this.commitFiles;
    }

    public static Commit fromFile(String sha1Hash) {
        if (!join(COMMITS_DIR, sha1Hash).exists()) {
            // if commit does not exist
            return null;
        } else {
            Commit c = readObject(join(COMMITS_DIR, sha1Hash), Commit.class);
            return c;
        }
    }
    public void saveCommit() {
        this.commitID = sha1(serialize(this));
        File newCommit = join(COMMITS_DIR, commitID);
        try {
            newCommit.createNewFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        writeObject(newCommit, this);
    }
    public String getMessage() {
        return this.message;
    }
    public String getTimestamp() {
        return this.timestamp;
    }
    public String getParent() {
        return this.parent;
    }
    public Set<String> getParents() {
        Set<String> parents = new HashSet<>();
        parents.add(this.parent);
        parents.add(this.parent2);
        return parents;
    }
    public String getParent2() {
        return this.parent2;
    }
    public String getCommitID() {
        return this.commitID;
    }

}
