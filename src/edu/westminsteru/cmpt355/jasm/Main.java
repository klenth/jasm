package edu.westminsteru.cmpt355.jasm;

import edu.westminsteru.cmpt355.jasm.parser.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.classfile.*;
import java.lang.classfile.attribute.SourceFileAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class Main implements JasmParserListener {

    public static void main(String... args) throws Exception {
        String inputFilename = null;
        String outPrefix = "";

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-h") || args[i].equals("-help") || args[i].equals("--help")) {
                printUsage();
                System.exit(0);
            } else if (args[i].equals("-d") && i + 1 < args.length)
                outPrefix = args[++i] + "/";
            else if (inputFilename == null)
                inputFilename = args[i];
            else {
                System.err.printf("Unknown argument: %s\n", args[i]);
                System.exit(1);
            }
        }

        if (inputFilename == null) {
            System.err.println("No input file specified.");
            printUsage();
            System.exit(1);
        }

        try (BufferedReader in = Files.newBufferedReader(Path.of(inputFilename))) {
            JasmAssembler assembler = JasmAssembler.reading(in);
            var status = assembler.assemble();
            if (status == Status.Failure) {
                assembler.getErrorMessages().forEach(msg -> {
                    msg.print(System.err);
                    System.err.println();
                });
            } else {
                Path outPath = Path.of(outPrefix);
                Files.createDirectories(outPath);
                assembler.getAssembledBytecodes().forEach(bc -> saveBytecode(bc, outPath));
            }
        }
    }

    private static void printUsage() {
        System.out.println("""
                Usage: java edu.westminsteru.cmpt355.jasm.Main [options] filename
                
                Options:
                    -h      Show this help message
                    -d dir  Save output in the given directory
                """);
    }

    private static void saveBytecode(Bytecode bytecode, Path outPath) {
        Path destPath = outPath.resolve(bytecode.className() + ".class");
        try {
            Files.createDirectories(destPath.getParent());
        } catch (IOException ex) {
            System.err.printf("Unable to create destination directory %s", destPath.getParent());
            return;
        }

        try (var out = Files.newOutputStream(destPath)) {
            out.write(bytecode.data());
        } catch (IOException ex) {
            System.err.printf("Unable to write %s\n", destPath);
        }
    }
}