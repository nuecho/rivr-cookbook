/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.wiki;

import static com.nuecho.rivr.cookbook.wiki.WikiDocProcessor.Mode.*;
import static java.lang.String.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;

/**
 * @author Nu Echo Inc.
 */
public class WikiDocProcessor {

    static final Pattern SECTION_1_MARK = Pattern.compile("^#(?!#)(.*)");
    static final Pattern SECTION_2_MARK = Pattern.compile("^##(?!#)(.*)");
    static final Pattern SECTION_3_MARK = Pattern.compile("^###(.*)");

    static final Pattern RIVR_JAVA_ITEM = Pattern.compile("`((com[.]nuecho[.]rivr[.].*?[.])([A-Z][^#]+?)(#.*?)?)`");
    static final Pattern RIVR_JAVA_ITEM_METHOD_PART = Pattern.compile("#((.*?)[(].*?[)])"); //method name + arg list

    static final Pattern SET_MARK = Pattern.compile("!set\\s+(.+?)\\s+(.+)");
    static final Pattern INLINE_CODE_MARK = Pattern.compile("!inline\\s+(\\S+)(\\s+(\\d+)-(\\d+))?");
    static final Pattern EOL = Pattern.compile("(\\r\\n|\\n)");
    static final Pattern INDENT = Pattern.compile("^\\s+");
    static final Pattern LINK = Pattern.compile("\\[\\[(.*)\\]\\]");

    public enum Mode {
        GITHUB, PANDOC, GITLAB;
    }

    public static void main(String[] arguments) throws IOException, WikiDocProcessorException {

        if (arguments.length < 7) {
            System.out.println("usage:\n    java "
                               + WikiDocProcessor.class.getName()
                               + " inputFile outputFile repositoryPath rep javadoc-base-path [github|gitlab|pandoc]");
            System.exit(1);
        }

        String inputFilename = arguments[0];
        String outputDirectory = arguments[1];
        String repositoryPath = arguments[2];
        String gitHubUser = arguments[3];
        String rep = arguments[4];
        String javadocBasePath = arguments[5];

        Mode mode;

        String modeString = arguments[6];
        if (modeString.equals("github")) {
            mode = GITHUB;
        } else if (modeString.equals("gitlab")) {
            mode = GITLAB;
        } else if (modeString.equals("pandoc")) {
            mode = PANDOC;
        } else throw new WikiDocProcessorException("Invalid mode: '" + modeString + "'.");

        processFile(inputFilename, outputDirectory, repositoryPath, gitHubUser, mode, rep, javadocBasePath);
    }

    private static void processFile(String inputFilename,
                                    String outputDirectory,
                                    String repositoryPath,
                                    String namespace,
                                    Mode mode,
                                    String currentRepository,
                                    String javadocBasePath) throws IOException, UnsupportedEncodingException,
            FileNotFoundException, AmbiguousObjectException, IncorrectObjectTypeException, WikiDocProcessorException {

        Git git = Git.open(new File(repositoryPath));
        Repository repository = git.getRepository();

        File inputFile = new File(inputFilename);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf-8"));
        PrintWriter writer;

        if (mode == PANDOC) {
            writer = openFile(outputDirectory + "/wiki.md");
        } else {
            writer = new PrintWriter(new StringWriter());
        }

        String line;
        String currentSection1 = null;
        String currentSection2 = null;
        String currentSection3 = null;
        String currentBranch = null;

        Map<String, Map<String, List<String>>> tableOfContents = new LinkedHashMap<String, Map<String, List<String>>>();
        Map<String, List<String>> sections2 = new HashMap<String, List<String>>();
        List<String> sections3 = new ArrayList<String>();

        int lineNumber = 0;
        boolean inSection = false;
        try {
            while (null != (line = reader.readLine())) {
                lineNumber++;
                Matcher setMarkMatcher = SET_MARK.matcher(line);
                Matcher inlineCodeMatcher = INLINE_CODE_MARK.matcher(line);
                Matcher section1MarkMatcher = SECTION_1_MARK.matcher(line);
                Matcher section2MarkMatcher = SECTION_2_MARK.matcher(line);
                Matcher section3MarkMatcher = SECTION_3_MARK.matcher(line);

                if (section1MarkMatcher.find()) {
                    if (inSection) {
                        generateSectionFooter(namespace, writer, currentBranch, currentRepository, mode);
                        inSection = false;
                    }

                    currentSection1 = section1MarkMatcher.group(1).trim();
                    sections2 = new LinkedHashMap<String, List<String>>();
                    if (mode == PANDOC) {
                        writer.println("# " + currentSection1);
                    }
                    tableOfContents.put(currentSection1, sections2);

                } else if (section2MarkMatcher.find()) {
                    if (inSection) {
                        generateSectionFooter(namespace, writer, currentBranch, currentRepository, mode);
                        inSection = false;
                    }

                    currentSection2 = section2MarkMatcher.group(1).trim();
                    sections3 = new ArrayList<String>();
                    if (mode == PANDOC) {
                        writer.println("## " + currentSection2);
                    }
                    sections2.put(currentSection2, sections3);

                } else if (section3MarkMatcher.find()) {
                    currentSection3 = section3MarkMatcher.group(1).trim();
                    sections3.add(currentSection3);

                    if (inSection) {
                        generateSectionFooter(namespace, writer, currentBranch, currentRepository, mode);
                        inSection = false;
                    }

                    inSection = true;

                    if (mode != PANDOC) {
                        writer.close();
                        writer = openFile(outputDirectory + "/" + normalizeFileName(currentSection3, mode));
                    } else {
                        writer.println("### " + currentSection3);
                    }

                } else if (inlineCodeMatcher.find()) {
                    String filename = inlineCodeMatcher.group(1);

                    int startLine;
                    int stopLine;

                    String lineNumbers = inlineCodeMatcher.group(2);
                    if (lineNumbers != null) {
                        startLine = Integer.parseInt(inlineCodeMatcher.group(3));
                        stopLine = Integer.parseInt(inlineCodeMatcher.group(4));
                    } else {
                        startLine = 0;
                        stopLine = Integer.MAX_VALUE;
                    }

                    InputStream blobInputStream = getBlobInputStream(repository, currentBranch, filename);
                    String lines = getFileLines(startLine, stopLine, blobInputStream);
                    String inlinedFile = normalizeIndent(lines);

                    String repoUrl = getRepoUrl(namespace, mode, currentRepository);
                    String revisionUrl = repoUrl + "/blob/" + currentBranch + "/" + filename;

                    String justFilename = filename.substring(filename.lastIndexOf('/') + 1);

                    if (mode == GITLAB) {
                        writer.println(">    [" + justFilename + "](" + revisionUrl + "):\n");
                    } else {
                        writer.println(">    [_" + justFilename + "_](" + revisionUrl + "):\n");
                    }

                    if (mode == PANDOC) {
                        writer.println("~~~~{.java .numberLines startFrom=\"" + startLine + "\"}");
                    } else {
                        writer.println("```java");
                    }
                    writer.print(inlinedFile);

                    if (mode == PANDOC) {
                        writer.println("~~~~");
                    } else {
                        writer.println("```");
                    }
                    writer.println();
                } else if (setMarkMatcher.find()) {
                    String key = setMarkMatcher.group(1);
                    String value = setMarkMatcher.group(2);
                    if (key.equals("branch")) {
                        currentBranch = value;
                    } else throw new WikiDocProcessorException("Unknown key '" + key + "' at line " + lineNumber);
                } else {

                    if (mode == GITLAB) {
                        Matcher linkMatcher = LINK.matcher(line);
                        while (linkMatcher.find()) {
                            String slug = linkMatcher.group(1).replaceAll("[^0-9A-Za-z]", "-");
                            line = linkMatcher.replaceFirst("[$1](" + slug + ")");
                        }
                    } else if (mode == PANDOC) {
                        Matcher linkMatcher = LINK.matcher(line);
                        while (linkMatcher.find()) {
                            String slug = linkMatcher.group(1).replaceAll("[^0-9A-Za-z]", "-").toLowerCase();
                            line = linkMatcher.replaceFirst("[$1](#" + slug + ")");
                        }
                    }

                    Matcher rivrClassMatcher = RIVR_JAVA_ITEM.matcher(line);
                    while (rivrClassMatcher.find()) {
                        String item = rivrClassMatcher.group(1);
                        String shortClassName = rivrClassMatcher.group(3);
                        String packageName = rivrClassMatcher.group(2);

                        String uri = javadocBasePath + packageName.replace('.', '/') + shortClassName + ".html";

                        String displayItem;

                        Matcher methodPartMatcher = RIVR_JAVA_ITEM_METHOD_PART.matcher(item);

                        if (methodPartMatcher.find()) {
                            String methodNameAndArguments = methodPartMatcher.group(1);
                            methodNameAndArguments = methodNameAndArguments.replaceAll(" ", "%20");
							uri += "#" + methodNameAndArguments;
                            String methodName = methodPartMatcher.group(2);
                            displayItem = methodName;
                        } else {
                            displayItem = shortClassName;
                        }

                        line = rivrClassMatcher.replaceFirst("[`" + displayItem + "`](" + uri + ")");
                        rivrClassMatcher = RIVR_JAVA_ITEM.matcher(line);
                    }

                    writer.println(line);
                }
            }

            generateSectionFooter(namespace, writer, currentBranch, currentRepository, mode);

            if (mode != PANDOC) {
                writeTableOfContents(outputDirectory, tableOfContents, mode, namespace, currentRepository);
            }
        } finally {
            writer.close();
            reader.close();
        }

    }

    private static String getRepoUrl(String namespace, Mode mode, String currentRepository) {
        String baseUrl = getBaseUrl(mode);
        String repoUrl = baseUrl + namespace + "/" + currentRepository;
        return repoUrl;
    }

    private static void generateSectionFooter(String namespace,
                                              PrintWriter writer,
                                              String branch,
                                              String repository,
                                              Mode mode) {
        if (branch == null) return;

        if (mode == PANDOC) return;

        writer.println();
        writer.println("---------------------------");
        writer.println();
        writer.println("#### Running this example");

        String baseUrl = getBaseUrl(mode);

        String downloadUrl = baseUrl + namespace + "/" + repository + "/archive/" + branch + ".zip";
        String browseUrl = baseUrl + namespace + "/" + repository + "/tree/" + branch;

        if (mode == GITHUB) {
            writer.println(format("You can [download](%s) or [browse](%s) the complete code for this example at GitHub."
                                          + "This is a complete working application that you can build and run for yourself.",
                                  downloadUrl,
                                  browseUrl));
        } else {
            writer.println(format("You can [browse](%s) the complete code for this example at GitHub."
                                          + "This is a complete working application that you can build and run for yourself.",
                                  browseUrl));

        }
        writer.println();
        writer.println("You can also clone the Rivr Cookbook repository and checkout this example:");
        writer.println();
        if (mode == GITHUB) {
            writer.println("`git clone -b " + branch + " git@github.com:" + namespace + "/" + repository + ".git`");
        }
        if (mode == GITLAB) {
            writer.println("`git clone -b " + branch + " git@gl.s.nuecho.com:" + namespace + "/" + repository + ".git`");
        }
        writer.println();
        writer.println("Then, to build and run it:");
        writer.println();
        writer.println("`cd " + repository + "`");
        writer.println();
        writer.println("`./gradlew jettyRun`");
        writer.println();
        writer.println("The VoiceXML dialogue should be available at ");
        writer.println("[http://localhost:8080/rivr-cookbook/dialogue](http://localhost:8080/rivr-cookbook/dialogue)");
        writer.println();
        writer.println("To stop the application, press Control-C in the console.");
        writer.println();
    }

    private static String getBaseUrl(Mode mode) {
        return mode == GITHUB ? "https://github.com/" : "http://gl.s.nuecho.com/";
    }

    private static void writeTableOfContents(String outputDirectory,
                                             Map<String, Map<String, List<String>>> tableOfContents,
                                             Mode mode,
                                             String namespace,
                                             String repository) throws UnsupportedEncodingException,
            FileNotFoundException {
        String tocFilename = normalizeFileName("Home", mode);

        writeTableOfContents(outputDirectory, tableOfContents, tocFilename, mode, namespace, repository);
    }

    private static void writeTableOfContents(String outputDirectory,
                                             Map<String, Map<String, List<String>>> tableOfContents,
                                             String filename,
                                             Mode mode,
                                             String namespace,
                                             String repository) throws UnsupportedEncodingException,
            FileNotFoundException {
        PrintWriter tableOfContentsWriter = openFile(outputDirectory + "/" + filename);
        writeTableOfContents(tableOfContents, tableOfContentsWriter, mode, namespace, repository);
        tableOfContentsWriter.close();
    }

    private static void writeTableOfContents(Map<String, Map<String, List<String>>> tableOfContents,
                                             PrintWriter writer,
                                             Mode mode,
                                             String namespace,
                                             String repository) {

        writer.println("<center>\n");
        writer.println("![Rivr logo](http://rivr.nuecho.com/img/logo.png)");
        writer.println();
        writer.println("# Cookbook");
        writer.println("_A collection a various recipes explaining how to achieve various tasks with Rivr_");
        writer.println();
        writer.println("</center>");

        for (Entry<String, Map<String, List<String>>> entry : tableOfContents.entrySet()) {
            String section1Name = entry.getKey();
            writer.println();
            writer.println("1. " + section1Name);
            writer.println();
            for (Entry<String, List<String>> section2Entry : entry.getValue().entrySet()) {
                writer.println("    1. " + section2Entry.getKey());
                for (String section3Name : section2Entry.getValue()) {
                    if (mode == GITHUB) {
                        writer.println("        1. [[" + section3Name + "]]");
                    } else {
                        String section3NameSlug = section3Name.replaceAll("[^a-zA-Z0-9]", "-");
                        writer.println("        1. [" + section3Name + "](" + section3NameSlug + ")");
                    }
                }
            }
        }

        writer.println();
        writer.println("You haven't found your answer here?  Something is not right? Let us know by "
                       + "[opening an issue]("
                       + getRepoUrl(namespace, mode, repository)
                       + "/issues/new) "
                       + "and we'll do our best to adjust the recipes or to provide a new one.");
    }

    private static String normalizeFileName(String item, Mode mode) {
        String suffix;
        if (mode == GITLAB) {
            suffix = ".markdown";
        } else {
            suffix = ".md";
        }

        return normalizeWiki(item) + suffix;
    }

    private static String normalizeWiki(String item) {
        return item.replaceAll("[^A-Za-z0-9 ]", "-");
    }

    private static PrintWriter openFile(String outputFilename) throws UnsupportedEncodingException,
            FileNotFoundException {
        return new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFilename)), "utf-8"));
    }

    private static InputStream getBlobInputStream(Repository repository, String reference, String path)
            throws AmbiguousObjectException, IncorrectObjectTypeException, IOException, WikiDocProcessorException,
            UnsupportedEncodingException {
        String revString = reference + ":" + path;
        ObjectId objectId = repository.resolve(revString);
        if (objectId == null) throw new WikiDocProcessorException("Unable to find object for " + revString);

        ObjectLoader objectLoader = repository.getObjectDatabase().open(objectId);

        int type = objectLoader.getType();
        if (type != Constants.OBJ_BLOB)
            throw new WikiDocProcessorException("Path '" + path + "' does not refer to a BLOB.");

        return objectLoader.openStream();
    }

    private static String getFileLines(int startLine, int stopLine, InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder builder = new StringBuilder();
        String line;

        for (int lineNumber = 1; lineNumber <= stopLine && null != (line = reader.readLine()); lineNumber++) {
            if (lineNumber >= startLine) {
                builder.append(line);
                builder.append('\n');
            }
        }

        return builder.toString();
    }

    private static String normalizeIndent(String in) {
        String[] lines = EOL.split(in);

        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            Matcher indentMatcher = INDENT.matcher(line);
            if (indentMatcher.find()) {
                int length = indentMatcher.group(0).length();
                if (length < minIndent) {
                    minIndent = length;
                }
            } else if (line.trim().length() > 0) {
                minIndent = 0;
            }
        }

        if (minIndent == Integer.MAX_VALUE) return in;

        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (INDENT.matcher(line).find()) {
                builder.append(line.substring(minIndent));
            } else {
                builder.append(line);
            }
            builder.append('\n');

        }

        return builder.toString();
    }
}
