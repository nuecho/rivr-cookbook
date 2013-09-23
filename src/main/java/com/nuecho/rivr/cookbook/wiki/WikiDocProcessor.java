/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.wiki;

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

    static final Pattern DOWNLOAD_MARK = Pattern.compile("DOWNLOAD\\|(.*?)\\|(.*?)\\|");
    static final Pattern INLINE_CODE_MARK = Pattern.compile("INLINE_CODE\\|(.*?)\\|(.*?)\\|(.*?)(\\|(\\d+)-(\\d+))?\\|");
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

        if (arguments.length == 4) {
            String pandocModeString = arguments[4];
            pandocMode = Boolean.parseBoolean(pandocModeString);
        } else {
            pandocMode = false;
        }

        Git git = Git.open(new File(repositoryPath));
        Repository repository = git.getRepository();

        File inputFile = new File(inputFilename);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf-8"));
        PrintWriter writer = new PrintWriter(new StringWriter());

        String line;
        String currentSection1 = null;
        String currentSection2 = null;
        String currentSection3 = null;

        Map<String, Map<String, List<String>>> tableOfContents = new LinkedHashMap<String, Map<String, List<String>>>();
        Map<String, List<String>> sections2 = new HashMap<String, List<String>>();
        List<String> sections3 = new ArrayList<String>();

        while (null != (line = reader.readLine())) {

            Matcher inlineCodeMatcher = INLINE_CODE_MARK.matcher(line);
            Matcher downloadMarkMatcher = DOWNLOAD_MARK.matcher(line);
            Matcher section1MarkMatcher = SECTION_1_MARK.matcher(line);
            Matcher section2MarkMatcher = SECTION_2_MARK.matcher(line);
            Matcher section3MarkMatcher = SECTION_3_MARK.matcher(line);

            if (section1MarkMatcher.find()) {
                currentSection1 = section1MarkMatcher.group(1).trim();
                sections2 = new LinkedHashMap<String, List<String>>();
                tableOfContents.put(currentSection1, sections2);
            } else if (section2MarkMatcher.find()) {
                currentSection2 = section2MarkMatcher.group(1).trim();
                sections3 = new ArrayList<String>();
                sections2.put(currentSection2, sections3);
            } else if (section3MarkMatcher.find()) {
                writer.close();
                currentSection3 = section3MarkMatcher.group(1).trim();
                sections3.add(currentSection3);
                writer = openFile(outputDirectory + "/" + normalizeFileName(currentSection3));
            } else if (inlineCodeMatcher.find()) {
                String repositoryName = inlineCodeMatcher.group(1);
                String branch = inlineCodeMatcher.group(2);
                String filename = inlineCodeMatcher.group(3);

                int startLine;
                int stopLine;

                String lineNumbers = inlineCodeMatcher.group(4);
                if (lineNumbers != null) {
                    startLine = Integer.parseInt(inlineCodeMatcher.group(5));
                    stopLine = Integer.parseInt(inlineCodeMatcher.group(6));
                } else {
                    startLine = 0;
                    stopLine = Integer.MAX_VALUE;
                }

                InputStream blobInputStream = getBlobInputStream(repository, branch, filename);
                String lines = getFileLines(startLine, stopLine, blobInputStream);
                String inlinedFile = inlineCodeMatcher.replaceAll(lines);
                inlinedFile = normalizeIndent(inlinedFile);

                String revisionUrl = "https://github.com/"
                                     + gitHubUser
                                     + "/"
                                     + repositoryName
                                     + "/blob/"
                                     + branch
                                     + "/"
                                     + filename;
                String rawUrl = "https://raw.github.com/"
                                + gitHubUser
                                + "/"
                                + repositoryName
                                + "/"
                                + branch
                                + "/"
                                + filename;
                String justFilename = filename.substring(filename.lastIndexOf('/') + 1);

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
                writer.println("> " + justFilename + " [(raw)](" + rawUrl + ") [(revision)](" + revisionUrl + ")");

            } else if (downloadMarkMatcher.find()) {
                String repositoryName = downloadMarkMatcher.group(1);
                String branch = downloadMarkMatcher.group(2);
                String url = "https://github.com/" + gitHubUser + "/" + repositoryName + "/archive/" + branch + ".zip";
                writer.println("> [Download](" + url + ") the complete code for this example");

            } else {
                writer.println(line);
            }
        }

        writer.close();
        reader.close();

        writeTableOfContents(outputDirectory, tableOfContents);

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

        tableOfContentsWriter.close();
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
        if (type != Constants.OBJ_BLOB) throw new WikiDocProcessorException("Path does not refer to a BLOB.");

        return objectLoader.openStream();
    }

    private static String getFileLines(int startLine, int stopLine, InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuilder builder = new StringBuilder();
        String line;

        int lineNumber = 0;
        while (null != (line = reader.readLine())) {
            lineNumber++;

            if (lineNumber < startLine) {
                continue;
            }

            if (lineNumber > stopLine) {
                break;
            }

            builder.append(line);
            builder.append('\n');
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
