package refraff.codegen;

// Contains the information from running the generated C code
public class CodeExecutionReport {
    String sourceFile;
    String executableFile;
    String sourcePath;
    String executablePath;
    String compileErrorMessage;
    String runErrorMessage;
    int exitCode;
    String memLeakInfo;

    // I think I can form the souce and executable strings from here
    public String getSourceFile() {
        return sourceFile;
    }

    public String getExecutableFile() {
        return executableFile;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getExecutablePath() {
        return executablePath;
    }

    public String getCompileErrorMessage() {
        return compileErrorMessage;
    }

    public String getRunErrorMessage() {
        return runErrorMessage;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getMemLeakInfo() {
        return memLeakInfo;
    }

    public void setSourceFile(String sourceFile) {
        this.sourceFile = sourceFile;
    }

    public void setExecutableFile(String executableFile) {
        this.executableFile = executableFile;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    public void setCompileErrorMessage(String compileErrorMessage) {
        this.compileErrorMessage = compileErrorMessage;
    }

    public void setRunErrorMessage(String runErrorMessage) {
        this.runErrorMessage = runErrorMessage;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public void setMemLeakInfo(String memLeakInfo) {
        this.memLeakInfo = memLeakInfo;
    }
}
