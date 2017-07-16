package de.letsbuildacompiler.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import jasmin.ClassFile;

public class CompilerTest {

    private Path tempDir;

    @BeforeMethod
    public void createTempDir() throws IOException {
        tempDir = Files.createTempDirectory("compilerTest");
    }

    @AfterMethod
    public void deleteTempDir() {
        deleteRecursive(tempDir.toFile());
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursive(child);
            }
        }
        if (!file.delete()) {
            throw new Error("Could not delete file <" + file + ">");
        }
    }

    @Test(dataProvider = "provideCodeExpectedText")
    public void runningCodeOutputsExpectedText(String code, String expectedText) throws Exception { // testen das bestimmter Output ausgegeben wurde

        //execution
        String actualOutput = compileAndRun(code);

        //evaluation
        Assert.assertEquals(actualOutput, expectedText);
    }

    @DataProvider
    public Object[][] provideCodeExpectedText() {
        return new Object[][] {
                { "1+2", "3\n" },
                { "1+2+42", "46\n" }
        };
    }

    private String compileAndRun(String code) throws Exception {
        code = Main.compile(new ANTLRInputStream(code));

        ClassFile classFile = new ClassFile();
        classFile.readJasmin(new StringReader(code), "", false); //klasse kompilieren
        Path outputPath = tempDir.resolve(classFile.getClassName() + ".class");
        classFile.write(Files.newOutputStream(outputPath));
        return runJavaClass(tempDir, classFile.getClassName());
    }

    private String runJavaClass(Path dir, String className) throws Exception {
        //neuen Java prozess starten
        Process process = Runtime.getRuntime().exec(new String[] { "java", "-cp", dir.toString(), className });
        try (InputStream in = process.getInputStream()) {
            return new Scanner(in).useDelimiter("\\A").next();
        }
    }
}
