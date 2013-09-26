/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.wiki;

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

    static final Pattern SET_MARK = Pattern.compile("!set\\s+(.+?)\\s+(.+)");
    static final Pattern INLINE_CODE_MARK = Pattern.compile("!inline\\s+(\\S+)(\\s+(\\d+)-(\\d+))?");
    static final Pattern EOL = Pattern.compile("(\\r\\n|\\n)");
    static final Pattern INDENT = Pattern.compile("^\\s+");

    public static void main(String[] arguments) throws IOException, WikiDocProcessorException {

        if (arguments.length < 4 || arguments.length > 5) {
            System.out.println("usage:\n    java "
                               + WikiDocProcessor.class.getName()
                               + " inputFile outputFile gitRepository [pandocMode]");
            System.exit(1);
        }

        String inputFilename = arguments[0];
        String outputDirectory = arguments[1];
        String repositoryPath = arguments[2];
        String gitHubUser = arguments[3];

        processFile(arguments, inputFilename, outputDirectory, repositoryPath, gitHubUser);
    }

    private static void processFile(String[] arguments,
                                    String inputFilename,
                                    String outputDirectory,
                                    String repositoryPath,
                                    String gitHubUser) throws IOException, UnsupportedEncodingException,
            FileNotFoundException, AmbiguousObjectException, IncorrectObjectTypeException, WikiDocProcessorException {
        boolean pandocMode;

        if (arguments.length == 5) {
            String pandocModeString = arguments[4];
            pandocMode = Boolean.parseBoolean(pandocModeString);
        } else {
            pandocMode = false;
        }

        Git git = Git.open(new File(repositoryPath));
        Repository repository = git.getRepository();

        File inputFile = new File(inputFilename);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf-8"));
        PrintWriter writer;

        if (pandocMode) {
            writer = openFile(outputDirectory + "/wiki.md");
        } else {
            writer = new PrintWriter(new StringWriter());
        }

        String line;
        String currentSection1 = null;
        String currentSection2 = null;
        String currentSection3 = null;
        String currentBranch = null;
        String currentRepository = null;

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
                        generateSectionFooter(gitHubUser, writer, currentBranch, currentRepository, pandocMode);
                        inSection = false;
                    }

                    currentSection1 = section1MarkMatcher.group(1).trim();
                    sections2 = new LinkedHashMap<String, List<String>>();
                    if (pandocMode) {
                        writer.println("# " + currentSection1);
                    }
                    tableOfContents.put(currentSection1, sections2);

                } else if (section2MarkMatcher.find()) {
                    if (inSection) {
                        generateSectionFooter(gitHubUser, writer, currentBranch, currentRepository, pandocMode);
                        inSection = false;
                    }

                    currentSection2 = section2MarkMatcher.group(1).trim();
                    sections3 = new ArrayList<String>();
                    if (pandocMode) {
                        writer.println("## " + currentSection2);
                    }
                    sections2.put(currentSection2, sections3);

                } else if (section3MarkMatcher.find()) {
                    currentSection3 = section3MarkMatcher.group(1).trim();
                    sections3.add(currentSection3);

                    if (inSection) {
                        generateSectionFooter(gitHubUser, writer, currentBranch, currentRepository, pandocMode);
                        inSection = false;
                    }

                    inSection = true;

                    if (!pandocMode) {
                        writer.close();
                        writer = openFile(outputDirectory + "/" + normalizeFileName(currentSection3));
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
                    String inlinedFile = inlineCodeMatcher.replaceAll(lines);
                    inlinedFile = normalizeIndent(inlinedFile);

                    String revisionUrl = "https://github.com/"
                                         + gitHubUser
                                         + "/"
                                         + currentRepository
                                         + "/blob/"
                                         + currentBranch
                                         + "/"
                                         + filename;

                    String justFilename = filename.substring(filename.lastIndexOf('/') + 1);
                    writer.println(">    [_" + justFilename + "_](" + revisionUrl + "):\n");

                    if (pandocMode) {
                        writer.println("~~~~{.java .numberLines startFrom=\"" + startLine + "\"}");
                    } else {
                        writer.println("```java");
                    }
                    writer.print(inlinedFile);

                    if (pandocMode) {
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
                    } else if (key.equals("repository")) {
                        currentRepository = value;
                    } else throw new WikiDocProcessorException("Unknown key '" + key + "' at line " + lineNumber);
                } else {
                    writer.println(line);
                }
            }

            generateSectionFooter(gitHubUser, writer, currentBranch, currentRepository, pandocMode);

            if (!pandocMode) {
                writeTableOfContents(outputDirectory, tableOfContents);
            }
        } finally {
            writer.close();
            reader.close();
        }

    }

    private static void generateSectionFooter(String gitHubUser,
                                              PrintWriter writer,
                                              String branch,
                                              String repository,
                                              boolean pandocMode) {
        if (branch == null) return;

        if (!pandocMode) {
            writer.println();
            writer.println("---------------------------");
        }
        writer.println();
        writer.println("#### Running this example");

        String downloadUrl = "https://github.com/" + gitHubUser + "/" + repository + "/archive/" + branch + ".zip";
        String browseUrl = "https://github.com/" + gitHubUser + "/" + repository + "/tree/" + branch;

        writer.println(format("You can [download](%s) or [browse](%s) the complete code for this example at GitHub."
                                      + "This is a complete working application that you can build and run for yourself.",
                              downloadUrl,
                              browseUrl));
        writer.println();
        writer.println("You can also clone the Rivr Cookbook repository and checkout this example:");
        writer.println();
        writer.println("`git clone -b " + branch + " git@github.com:" + gitHubUser + "/" + repository + ".git`");
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

    private static void writeTableOfContents(String outputDirectory,
                                             Map<String, Map<String, List<String>>> tableOfContents)
            throws UnsupportedEncodingException, FileNotFoundException {
        writeTableOfContents(outputDirectory, tableOfContents, "Home.md");
    }

    private static void writeTableOfContents(String outputDirectory,
                                             Map<String, Map<String, List<String>>> tableOfContents,
                                             String filename) throws UnsupportedEncodingException,
            FileNotFoundException {
        PrintWriter tableOfContentsWriter = openFile(outputDirectory + "/" + filename);
        writeTableOfContents(tableOfContents, tableOfContentsWriter);
        tableOfContentsWriter.close();
    }

    private static void writeTableOfContents(Map<String, Map<String, List<String>>> tableOfContents,
                                             PrintWriter tableOfContentsWriter) {
        for (Entry<String, Map<String, List<String>>> entry : tableOfContents.entrySet()) {
            String section1Name = entry.getKey();
            tableOfContentsWriter.println();
            tableOfContentsWriter.println("1. " + section1Name);
            tableOfContentsWriter.println();
            for (Entry<String, List<String>> section2Entry : entry.getValue().entrySet()) {
                tableOfContentsWriter.println("    1. " + section2Entry.getKey());
                for (String section3Name : section2Entry.getValue()) {
                    tableOfContentsWriter.println("        1. [[" + section3Name + "]]");
                }
            }
        }
    }

    private static String normalizeFileName(String item) {
        return normalizeWiki(item) + ".md";
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
