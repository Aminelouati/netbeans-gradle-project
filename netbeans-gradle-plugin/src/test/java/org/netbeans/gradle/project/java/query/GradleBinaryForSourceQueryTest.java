package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.util.JavaModelTestUtils;
import org.netbeans.gradle.project.util.NbSupplier;
import org.netbeans.gradle.project.util.SafeTmpFolder;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.util.Utilities;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;

public class GradleBinaryForSourceQueryTest {
    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    private NbSupplier<NbJavaModule> testModule(File rootDir) throws IOException {
        final NbJavaModule module = JavaModelTestUtils.createModule(rootDir);
        return new NbSupplier<NbJavaModule>() {
            @Override
            public NbJavaModule get() {
                return module;
            }
        };
    }

    private void verifyAllSourceDirsHaveClassesDir(BinaryForSourceQueryImplementation query, NbJavaModule module) throws IOException {
        for (JavaSourceSet sourceSet: module.getSources()) {
            try {
                verifyAllSourceDirsHaveClassesDir(query, sourceSet);
            } catch (Throwable ex) {
                throw new AssertionError("Test failed for source set: " + sourceSet.getName(), ex);
            }
        }
    }

    private void verifyAllSourceDirsHaveClassesDir(final BinaryForSourceQueryImplementation query, JavaSourceSet sourceSet) throws IOException {
        final JavaOutputDirs expectedOutput = sourceSet.getOutputDirs();

        for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
            for (File sourceRoot: sourceGroup.getSourceRoots()) {
                verifyBinaryRoots(query, sourceRoot, expectedOutput);
                if (sourceRoot.isDirectory()) {
                    Files.walkFileTree(sourceRoot.toPath(), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            verifyBinaryRoots(query, file.toFile(), expectedOutput);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }
            }
        }
    }

    private void verifyBinaryRoots(
            BinaryForSourceQueryImplementation query,
            File sourcePath,
            JavaOutputDirs expectedOutput) throws IOException {

        URL sourceUrl = Utilities.toURI(sourcePath).toURL();

        BinaryForSourceQuery.Result result = query.findBinaryRoots(sourceUrl);
        if (result == null) {
            throw new AssertionError("Missing result for " + sourcePath);
        }

        assertEquals("classes dir", expectedOutput.getClassesDir(), expectedSingleFile(result));
    }

    @Test
    public void testOutputOfSourceFiles() throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        NbSupplier<NbJavaModule> moduleRef = testModule(rootDir);
        NbJavaModule module = moduleRef.get();

        GradleBinaryForSourceQuery query = new GradleBinaryForSourceQuery(moduleRef);
        verifyAllSourceDirsHaveClassesDir(query, module);
    }

    private void verifyDoesNotHaveBinaryRoot(
            BinaryForSourceQueryImplementation query,
            File dir) throws IOException {

        URL dirUrl = Utilities.toURI(dir).toURL();

        BinaryForSourceQuery.Result queryResult = query.findBinaryRoots(dirUrl);
        assertNull("result of binary query", queryResult);
    }

    @Test
    public void verifyDoesNotHaveBinaryRoot() throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        NbSupplier<NbJavaModule> moduleRef = testModule(rootDir);
        NbJavaModule module = moduleRef.get();

        GradleBinaryForSourceQuery query = new GradleBinaryForSourceQuery(moduleRef);
        verifyDoesNotHaveBinaryRoot(query, module.getModuleDir());
    }
}
