package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Andrew Zhang
 */
import static gitlet.Utils.*;
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        Repository newRepo;
        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                if (!validatenumArgs(args, 1)) {
                    break;
                }
                if (join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println(
                            "A Gitlet version-control system already exists in the current directory.");
                    break;
                }
                newRepo = new Repository();
                newRepo.init();
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "add":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                String fileName = args[1];
                newRepo.addFile(fileName);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "commit":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                if (args[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                String message = args[1];
                newRepo.commit(message, null);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "log":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 1)) {
                    break;
                }
                newRepo.log();
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "checkout":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (args.length == 3) {
                    if (!validateArgs(args[1], "--")) {
                        break;
                    }
                    newRepo.basicCheckout(args[2]);
                } else if (args.length == 4) {
                    if (!validateArgs(args[2], "--")) {
                        break;
                    }
                    String commitID = args[1];
                    String file = args[3];
                    newRepo.checkoutCommit(commitID, file);
                } else if (args.length == 2) {
                    newRepo.checkoutBranch(args[1]);
                }
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "rm":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                newRepo.remove(args[1]);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "global-log":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 1)) {
                    break;
                }
                newRepo.globalLog();
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "find":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                String commitMessage = args[1];
                newRepo.find(commitMessage);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "branch":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                newRepo.makeBranch(args[1]);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "status":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 1)) {
                    break;
                }
                newRepo.status();
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "rm-branch":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                newRepo.removeBranch(args[1]);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "reset":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                newRepo.reset(args[1]);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            case "merge":
                if (!join(Repository.CWD, ".gitlet").exists()) {
                    System.out.println("Not in an initialized Gitlet directory.");
                    break;
                }
                newRepo = readObject(Repository.REPOFILE, Repository.class);
                if (!validatenumArgs(args, 2)) {
                    break;
                }
                newRepo.merge(args[1]);
                writeObject(Repository.REPOFILE, newRepo);
                break;
            default:
                System.out.println("No command with that name exists.");
                break;
        }
    }
    /** source: lab6 */
    public static boolean validatenumArgs(String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            return false;
        }
        return true;
    }
    public static boolean validateArgs(String arg, String expected) {
        if (!arg.equals(expected)) {
            System.out.println("Incorrect operands.");
            return false;
        }
        return true;
    }
}
