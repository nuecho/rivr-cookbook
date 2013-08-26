/*
 * Copyright (c) 2013 Nu Echo Inc. All rights reserved.
 */

package com.nuecho.rivr.cookbook.wiki;

import java.io.*;
import java.util.regex.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.errors.*;
import org.eclipse.jgit.lib.*;

/**
 * @author Nu Echo Inc.
 */
public class WikiDocProcessor {

    static final Pattern DOWNLOAD_MARK = Pattern.compile("DOWNLOAD\\|(.*?)\\|(.*?)\\|");
    static final Pattern INLINE_CODE_MARK = Pattern.compile("INLINE_CODE\\|(.*?)\\|(.*?)\\|(.*?)(\\|(\\d+)-(\\d+))?\\|");
    static final Pattern EOL = Pattern.compile("(\\r\\n|\\n)");
    static final Pattern INDENT = Pattern.compile("^\\s+");

    public static void main(String[] arguments) throws IOException, WikiDocProcessorException {

        if (arguments.length < 3 || arguments.length > 4) {
            System.out.println("usage:\n    java "
                               + WikiDocProcessor.class.getName()
                               + " inputFile outputFile gitRepository [pandocMode]");
            System.exit(1);
        }

        String inputFilename = arguments[0];
        String outputFilename = arguments[1];
        String repositoryPath = arguments[2];

        boolean pandocMode;

        if (arguments.length == 4) {
            String pandocModeString = arguments[3];
            pandocMode = Boolean.parseBoolean(pandocModeString);
        } else {
            pandocMode = false;
        }

        Git git = Git.open(new File(repositoryPath));
        Repository repository = git.getRepository();

        File inputFile = new File(inputFilename);
        File outputFile = new File(outputFilename);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "utf-8"));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile), "utf-8"));

        String line;
        while (null != (line = reader.readLine())) {

            Matcher inlineCodeMatcher = INLINE_CODE_MARK.matcher(line);
            Matcher downloadMarkMatcher = DOWNLOAD_MARK.matcher(line);

            if (inlineCodeMatcher.find()) {
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

                String revisionUrl = "https://github.com/" + repositoryName + "/blob/" + branch + "/" + filename;
                String rawUrl = "https://raw.github.com/" + repositoryName + "/" + branch + "/" + filename;
                writer.println(filename + " [[raw]](" + rawUrl + ") [[revision]](" + revisionUrl + ")");
                writer.println();

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

            } else if (downloadMarkMatcher.find()) {
                String repositoryName = downloadMarkMatcher.group(1);
                String branch = downloadMarkMatcher.group(2);
                String url = "https://github.com/" + repositoryName + "/archive/" + branch + ".zip";
                writer.println("[download](" + url + ") the complete code for this example");

            } else {
                writer.println(line);
            }
        }

        writer.close();
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
