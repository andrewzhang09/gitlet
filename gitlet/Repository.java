package gitlet;


import java.awt.*;
import java.io.File;
import static gitlet.Utils.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.List;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Andrew Zhang
 */
public class Repository implements Serializable {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File BLOB_DIR = join(GITLET_DIR, "blob");
    public static final File STAGE = join(GITLET_DIR, "stage");
    public static final File REMOVESTAGE = join(GITLET_DIR, "removestage");
    public static final File REPOFILE = join(GITLET_DIR, "REPOFILE");

    private ArrayDeque<String> pastCommits;
    private TreeMap<String, String> stageMap;
    private TreeMap<String, String> branchMap;

    private String headPointer;

    public Repository() {
        this.pastCommits = new ArrayDeque<>();
        this.stageMap = new TreeMap<>();
        this.branchMap = new TreeMap<>();
    }

    public void setupPersistence() {
        GITLET_DIR.mkdir();
        Commit.COMMITS_DIR.mkdir();
        BLOB_DIR.mkdir();
        STAGE.mkdir();
        REMOVESTAGE.mkdir();
        try {
            REPOFILE.createNewFile();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void init() {
        setupPersistence();
        TreeMap<String, String> files = new TreeMap<>();
        String commitID = makenewCommit("initial commit", null, null, files);
        pastCommits.addFirst(commitID);
        headPointer = "master";
        branchMap.put(headPointer, commitID);
    }

    public void addFile(String fileName) {
        if (!join(CWD, fileName).exists() && !join(REMOVESTAGE, fileName).exists()) {
            System.out.println("File does not exist.");
            return;
        }
        removeStage(STAGE, fileName);
        File currentFile = returnFile(fileName);
        String fileHash = sha1(readContents(currentFile));
        boolean shouldstage = shouldStage(fileName, fileHash);
        if (!shouldstage) {
            return;
        }
        addtoStage(STAGE, fileName, currentFile, fileHash);
    }
    /** sets currentFile to either file in CWD or file in REMOVESTAGE */
    private File returnFile(String fileName) {
        if (join(CWD, fileName).exists()) {
            return join(CWD, fileName);
        } else {
            return join(REMOVESTAGE, fileName);
        }
    }
    /** remove specified file from staging area (if it exists)
     * returns false if file does not exist*/
    private boolean removeStage(File stage, String fileName) {
        if (join(stage, fileName).exists()) {
            join(stage, fileName).delete();
            stageMap.remove(fileName);
            return true;
        }
        return false;
    }
    private void addtoStage(File stage, String fileName, File currentFile, String fileHash) {
        File filetoAdd = join(stage, fileName);
        writeContents(filetoAdd, readContents(currentFile));
        if (join(REMOVESTAGE, fileName).exists() && stage.equals(STAGE)) {
            writeContents(join(CWD, fileName), readContents(currentFile));
            join(REMOVESTAGE, fileName).delete();
        }
        stageMap.put(fileName, fileHash);
    }
    /** if current working file == current file in commit, do not stage */
    private boolean shouldStage(String fileName, String hash) {
        TreeMap<String, String> recentFiles = getoldFiles(branchMap.get(headPointer));
        if (!recentFiles.containsKey(fileName)) {
            // file isn't in last commit
            return true;
        }
        String sha1Hash = recentFiles.get(fileName);
        if (sha1Hash.equals(hash)) {
            /** current working file == current file in commit */
            removeStage(REMOVESTAGE, fileName);
            return false;
        }
        return true;
    }
    /** returns treemap of <filename: sha1Hash> of files stored in specified Commit */
    private TreeMap<String, String> getoldFiles(String commitID) {
        Commit recentCommit = Commit.fromFile(commitID);
        return recentCommit.getCommitFiles();
    }

    public void commit(String message, String parent2) {
        List<String> stagedFiles = plainFilenamesIn(STAGE);
        List<String> removedFiles = plainFilenamesIn(REMOVESTAGE);
        if (stagedFiles.isEmpty() && removedFiles.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        TreeMap<String, String> clone = getoldFiles(branchMap.get(headPointer));
        clone = modifyClone(clone, stagedFiles, removedFiles);
        String commitID = makenewCommit(message, branchMap.get(headPointer), parent2, clone);
        addtoBlobDir(stagedFiles);
        clearStage(stagedFiles, removedFiles);
        pastCommits.addFirst(commitID);
        branchMap.put(headPointer, commitID);
    }
    private TreeMap<String, String> modifyClone(TreeMap<String, String> clone,
                                                List<String> stagedFiles,
                                                List<String> removedFiles) {
        for (String fileName : stagedFiles) {
            // key: fileName, val: sha1hash of new/edited file
            clone.put(fileName, stageMap.get(fileName));
        }
        for (String fileName : removedFiles) {
            clone.remove(fileName);
        }
        return clone;
    }
    private void addtoBlobDir(List<String> stagedFiles) {
        for (String fileName : stagedFiles) {
            File newFile = join(BLOB_DIR, stageMap.get(fileName));
            writeContents(newFile, readContents(join(STAGE, fileName)));
        }
    }
    private void clearStage(List<String> stagedFiles, List<String> removedFiles) {
        for (String fileName : stagedFiles) {
            join(STAGE, fileName).delete(); // remove from STAGE_DIR
        }
        for (String fileName : removedFiles) {
            join(REMOVESTAGE, fileName).delete();
        }
        stageMap.clear();
    }
    private String makenewCommit(String message, String parent,
                                 String parent2, TreeMap<String, String> files) {
        Commit newCommit;
        newCommit = new Commit(message, parent, parent2, files);
        newCommit.saveCommit();
        return newCommit.getCommitID();
    }

    public void remove(String fileName) {
        TreeMap<String, String> commitFiles = getoldFiles(branchMap.get(headPointer));
        boolean unstaged = removeStage(STAGE, fileName);
        boolean exist = doesFileExistinCommit(commitFiles, fileName);
        if (!exist) {
            if (!unstaged) {
                removeStage(REMOVESTAGE, fileName);
                System.out.println("No reason to remove the file.");
            } else {
                removeStage(STAGE, fileName);
            }
            return;
        }
        // unstage file that has been deleted
        if (!join(CWD, fileName).exists() && exist) {
            String sha1Hash = commitFiles.get(fileName);
            addtoStage(REMOVESTAGE, fileName, join(BLOB_DIR, sha1Hash), sha1Hash);
            return;
        }
        File filetoRemove = join(CWD, fileName);
        addtoStage(REMOVESTAGE, fileName, filetoRemove, sha1(readContents(filetoRemove)));
        filetoRemove.delete();
    }

    public void log() {
        Commit recentCommit = Commit.fromFile(branchMap.get(headPointer));
        String commitID = recentCommit.getCommitID();
        String timestamp = recentCommit.getTimestamp();
        String message = recentCommit.getMessage();
        /** modify text with new commits */
        while (!commitID.equals(pastCommits.getLast())) {
            printLog(commitID, timestamp, message);
            System.out.println();
            commitID = recentCommit.getParent();
            recentCommit = Commit.fromFile(commitID);
            timestamp = recentCommit.getTimestamp();
            message = recentCommit.getMessage();
        }
        printLog(commitID, timestamp, message);
    }
    private void printLog(String commitID, String timestamp, String message) {
        System.out.println("===");
        System.out.println("commit " + commitID);
        Commit commit = Commit.fromFile(commitID);
        String parent1 = commit.getParent();
        String parent2 = commit.getParent2();
        if (parent2 != null) {
            System.out.println("Merge: " + parent1.substring(0, 7)
                    + " " + parent2.substring(0, 7));
        }
        System.out.println("Date: " + timestamp);
        System.out.println(message);
    }

    public void globalLog() {
        List<String> allCommits = plainFilenamesIn(Commit.COMMITS_DIR);
        for (String commitID : allCommits) {
            Commit c = Commit.fromFile(commitID);
            printLog(commitID, c.getTimestamp(), c.getMessage());
            System.out.println();
        }
    }

    public void find(String commitMessage) {
        int commitsCounter = 0;
        List<String> allCommits = plainFilenamesIn(Commit.COMMITS_DIR);
        for (String commitID : allCommits) {
            Commit c = Commit.fromFile(commitID);
            if (c.getMessage().equals(commitMessage)) {
                commitsCounter += 1;
                System.out.println(commitID);
            }
        }
        if (commitsCounter == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        List<String> branchesList = sortList(branchMap.keySet());
        List<String> stagedFiles = plainFilenamesIn(STAGE);
        List<String> removedFiles = plainFilenamesIn(REMOVESTAGE);
        Map<String, String> modifications = modificationsNotStaged(stagedFiles, removedFiles);
        List<String> untrackedFiles = checkUntrackedFiles();
        printStatus(branchesList, stagedFiles, removedFiles, modifications, untrackedFiles);
    }
    private List<String> sortList(Set<String> unsortedSet) {
        List<String> list = new ArrayList<>(unsortedSet);
        Collections.sort(list);
        return list;
    }
    private void printStatus(List<String> branchesList, List<String> stagedFiles,
                             List<String> removedFiles, Map<String, String> modifications,
                             List<String> untrackedFiles) {
        System.out.println("=== Branches ===");
        for (String branchName : branchesList) {
            if (branchName.equals(headPointer)) {
                System.out.print("*");
            }
            System.out.println(branchName);
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String fileName : stagedFiles) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fileName : removedFiles) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        List<String> sortedModifications =
                sortList(modificationsNotStaged(stagedFiles, removedFiles).keySet());
        for (String fileName : sortedModifications) {
            System.out.print(fileName);
            if (modifications.get(fileName).equals("modified")) {
                System.out.println(" (modified)");
            } else {
                System.out.println(" (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String fileName : untrackedFiles) {
            System.out.println(fileName);
        }
    }
    private Map<String, String> modificationsNotStaged(List<String> stagedFiles,
                                                       List<String> removedFiles) {
        Map<String, String> modifications = new TreeMap<>();
        Map<String, String> recentFiles = getoldFiles(branchMap.get(headPointer));
        Set<String> recentFileNames = recentFiles.keySet();
        modifications = modificationsDeletions(modifications, stagedFiles, recentFileNames,
                removedFiles, recentFiles);
        return modifications;
    }
    /** checks for modified and deleted files */
    private Map<String, String> modificationsDeletions(Map<String, String> modifications,
                                                      List<String> stagedFiles,
                                                      Set<String> recentFileNames,
                                                      List<String> removedFiles,
                                                      Map<String, String> recentFiles) {
        // DELETED FILES
        // Staged for addition, but deleted in the working directory
        HashSet<String> deletedFiles = new HashSet<>();
        for (String fileName : stagedFiles) {
            if (!(join(CWD, fileName).exists())) {
                deletedFiles.add(fileName);
                modifications.put(fileName, "deleted");
            }
        }
        // Not staged for removal, but tracked in the current commit
        // and deleted from the working directory
        for (String fileName : recentFileNames) {
            if (!(join(CWD, fileName).exists())) {
                if (!removedFiles.contains(fileName)) {
                    deletedFiles.add(fileName);
                    modifications.put(fileName, "deleted");
                }
            }
        }
        // MODIFIED FILES
        // Tracked in the current commit, changed in the working directory, but not staged
        for (String fileName : recentFileNames) {
            if (!join(CWD, fileName).exists()) {
                continue;
            }
            if (!stagedFiles.contains(fileName)) {
                String recentHash = recentFiles.get(fileName);
                String cwdHash = sha1(readContents(join(CWD, fileName)));
                if (!recentHash.equals(cwdHash)) {
                    modifications.put(fileName, "modified");
                }
            }
        }
        // Staged for addition, but with different contents than in the working directory
        for (String fileName : stagedFiles) {
            if (!join(CWD, fileName).exists()) {
                continue;
            }
            String stageHash = sha1(readContents(join(STAGE, fileName)));
            String cwdHash = sha1(readContents(join(CWD, fileName)));
            if (!stageHash.equals(cwdHash)) {
                modifications.put(fileName, "modified");
            }
        }
        return modifications;
    }

    public void basicCheckout(String fileName) {
        TreeMap<String, String> recentFiles = getoldFiles(branchMap.get(headPointer));
        boolean exist = doesFileExistinCommit(recentFiles, fileName);
        if (!exist) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        replaceCWDFile(recentFiles, fileName);
    }
    private boolean doesFileExistinCommit(TreeMap<String, String> commitFiles, String fileName) {
        if (!commitFiles.containsKey(fileName)) {
            return false;
        }
        return true;
    }
    private void replaceCWDFile(TreeMap<String, String> commitFiles, String fileName) {
        File filetobeReplaced = join(CWD, fileName);
        writeContents(filetobeReplaced, readContents(join(BLOB_DIR, commitFiles.get(fileName))));
    }

    public void checkoutCommit(String commitUID, String fileName) {
        String commitID = null;
        for (String id : pastCommits) {
            if (id.startsWith(commitUID)) {
                commitID = id;
                break;
            }
        }
        if (commitID == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        TreeMap<String, String> commitFiles = getoldFiles(commitID);
        boolean exist = doesFileExistinCommit(commitFiles, fileName);
        if (!exist) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        replaceCWDFile(commitFiles, fileName);
    }

    public void checkoutBranch(String branchName) {
        if (!branchMap.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (headPointer.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        List<String> untrackedFiles = checkUntrackedFiles();
        if (!untrackedFiles.isEmpty()) {
            System.out.println("There is an untracked file in the way; delete it, "
                    + "or add and commit it first.");
            return;
        }
        TreeMap<String, String> filestoMove = getoldFiles(branchMap.get(branchName));
        replaceallCWDFiles(filestoMove);
        headPointer = branchName;
        clearStage(plainFilenamesIn(STAGE), plainFilenamesIn(REMOVESTAGE));
    }
    /** delete all current CWD files and replace them with files
     * tracked by specified treemap of files */
    private void replaceallCWDFiles(TreeMap<String, String> filestoMove) {
        clearCWD(plainFilenamesIn(CWD));
        Set<String> files = filestoMove.keySet();
        for (String fileName : files) {
            File newFile = join(CWD, fileName);
            writeContents(newFile, readContents(join(BLOB_DIR, filestoMove.get(fileName))));
        }
    }
    private void clearCWD(List<String> cwdFiles) {
        for (String fileName : cwdFiles) {
            join(CWD, fileName).delete();
        }
    }
    /** returns list of untracked files (not in stage or tracked by recent commit) */
    private List<String> checkUntrackedFiles() {
        Set<String> untrackedFiles = new HashSet<>();
        List<String> cwdFiles = plainFilenamesIn(CWD);
        TreeMap<String, String> recentFiles = getoldFiles(branchMap.get(headPointer));
        for (String fileName : cwdFiles) {
            if (!recentFiles.containsKey(fileName)) {
                // if file in CWD is not tracked by recent commit
                if (!stageMap.containsKey(fileName)) {
                    untrackedFiles.add(fileName);
                }
            }
        }
        return sortList(untrackedFiles);
    }

    public void makeBranch(String branchName) {
        if (branchMap.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branchMap.put(branchName, branchMap.get(headPointer));
    }

    public void removeBranch(String branchName) {
        if (!branchMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (headPointer.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branchMap.remove(branchName);
    }

    public void reset(String commitID) {
        if (!pastCommits.contains(commitID)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        if (!checkUntrackedFiles().isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return;
        }
        replaceallCWDFiles(getoldFiles(commitID));
        clearStage(plainFilenamesIn(STAGE), plainFilenamesIn(REMOVESTAGE));
        branchMap.put(headPointer, commitID);
    }

    private boolean mergeErrors(String branchName) {
        if (!plainFilenamesIn(REMOVESTAGE).isEmpty() || !plainFilenamesIn(STAGE).isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        }
        if (!branchMap.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        }
        if (headPointer.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        if (!checkUntrackedFiles().isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            return true;
        }
        return false;
    }
    // checks if split point is current or given branch
    private boolean checksplitPoint(String splitPoint, String branchName) {
        if (branchMap.get(branchName).equals(splitPoint)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return true;
        }
        if (branchMap.get(headPointer).equals(splitPoint)) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }
    public void merge(String branchName) {
        if (mergeErrors(branchName)) {
            return;
        }
        String splitPoint = findsplitPoint(branchName);
        if (checksplitPoint(splitPoint, branchName)) {
            return;
        }
        String currentID = branchMap.get(headPointer);
        String branchID = branchMap.get(branchName);
        Set<String> allFiles = new HashSet<>();
        for (String currentFile : getoldFiles(currentID).keySet()) {
            allFiles.add(currentFile);
        }
        for (String branchFile : getoldFiles(branchID).keySet()) {
            allFiles.add(branchFile);
        }
        boolean mergeConflict = false;
        for (String fileName : allFiles) {
            String splitFileHash = getoldFiles(splitPoint).get(fileName);
            String currentFileHash = getoldFiles(currentID).get(fileName);
            String branchFileHash = getoldFiles(branchID).get(fileName);
            boolean filatSplit = getoldFiles(splitPoint).containsKey(fileName);
            boolean filatBranch = getoldFiles(branchID).containsKey(fileName);
            boolean filatCurrent = getoldFiles(currentID).containsKey(fileName);
            if (filatSplit && filatBranch && filatCurrent) {
                if (!splitFileHash.equals(branchFileHash)
                        && splitFileHash.equals(currentFileHash)) {
                    checkoutCommit(branchID, fileName);
                    addtoStage(STAGE, fileName, join(BLOB_DIR, branchFileHash),
                            branchFileHash);
                    continue;
                } else if (!splitFileHash.equals(currentFileHash)
                        && splitFileHash.equals(branchFileHash)) {
                    continue;
                } else if (currentFileHash.equals(branchFileHash)
                        && !splitFileHash.equals(currentFileHash)) {
                    continue;
                }
            }
            if (!filatSplit && !filatBranch) {
                continue;
            } else if (!filatSplit && !filatCurrent) {
                checkoutCommit(branchID, fileName);
                addtoStage(STAGE, fileName,
                        join(BLOB_DIR, branchFileHash), branchFileHash);
                continue;
            } else if (filatSplit && splitFileHash.equals(currentFileHash)
                    && !filatBranch) {
                remove(fileName);
                continue;
            } else if (filatSplit && splitFileHash.equals(branchFileHash)
                    && !filatCurrent) {
                continue;
            } else {
                mergeConflict(currentFileHash, branchFileHash, fileName);
                mergeConflict = true;
            }
        }
        commit("Merged " + branchName + " into " + headPointer + ".", branchID);
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }
    private void mergeConflict(String currentFileHash, String branchFileHash, String fileName) {
        String currentFile;
        String branchFile;
        File mergedFile = join(CWD, fileName);
        if (currentFileHash == null || branchFileHash == null) {
            // or the contents of one are changed and the other file is deleted,
            if (currentFileHash == null) {
                branchFile = readContentsAsString(join(BLOB_DIR, branchFileHash));
                writeContents(mergedFile, "<<<<<<< HEAD" + "\n"
                        + "=======" + "\n" + branchFile + ">>>>>>>" + "\n");
            } else {
                currentFile = readContentsAsString(join(BLOB_DIR, currentFileHash));
                writeContents(mergedFile, "<<<<<<< HEAD" + "\n" + currentFile
                        + "=======" + "\n" + ">>>>>>>" + "\n");
            }
        } else {
            // contents of both are changed and different from other,
            // or the file was absent at the split point
            // and has different contents in the given and current branches.
            currentFile = readContentsAsString(join(BLOB_DIR, currentFileHash));
            branchFile = readContentsAsString(join(BLOB_DIR, branchFileHash));
            writeContents(mergedFile, "<<<<<<< HEAD" + "\n" + currentFile
                    + "=======" + "\n" + branchFile + ">>>>>>>" + "\n");
        }
        addtoStage(STAGE, fileName, mergedFile, sha1(readContents(mergedFile)));
    }
    /** finds UID of latest common ancestor (LCA) of current and given branch */
    private String findsplitPoint(String branchName) {
        Map<String, Integer> currentAncestors = findAncestors(headPointer);
        Map<String, Integer> branchAncestors = findAncestors(branchName);
        String lca = branchMap.get(headPointer);
        Integer minimumdistTo  = pastCommits.size();
        for (String commitID : pastCommits) {
            if (currentAncestors.containsKey(commitID)
                    && branchAncestors.containsKey(commitID)) {
                if (currentAncestors.get(commitID) < minimumdistTo) {
                    minimumdistTo = currentAncestors.get(commitID);
                    lca = commitID;
                }
            }
        }
        return lca;
    }
    // returns map of all ancestors of current/given branch
    private Map<String, Integer> findAncestors(String branchName) {
        Set<String> marked = new HashSet<>();
        Map<String, Integer> distTo = new TreeMap<>();
        Deque<String> q = new ArrayDeque<>();
        String headCommit = branchMap.get(branchName);
        distTo.put(headCommit, 0);
        marked.add(headCommit);
        q.addFirst(headCommit);
        while (!q.isEmpty()) {
            String commitID = q.removeLast();
            Commit commit = Commit.fromFile(commitID);
            for (String parentID : commit.getParents()) {
                if (!marked.contains(parentID) && parentID != null) {
                    distTo.put(parentID, distTo.get(commitID) + 1);
                    marked.add(parentID);
                    q.addFirst(parentID);
                }
            }
        }
        return distTo;
    }
}
