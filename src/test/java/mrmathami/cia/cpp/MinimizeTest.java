package mrmathami.cia.cpp;

import mrmathami.cia.cpp.ast.CppNode;
import mrmathami.cia.cpp.builder.ProjectVersion;
import mrmathami.cia.cpp.differ.VersionDiffer;
import mrmathami.cia.cpp.differ.VersionDifference;
import mrmathami.utils.Pair;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static mrmathami.cia.cpp.BuilderTest.createProjectVersion;

public final class MinimizeTest {
    private MinimizeTest() {
    }

    public static void main(String[] args) throws Exception {
//		System.out.println("press enter to start!");
//		System.in.read();

//        String ends = "";
//        String ends = "-nootherexts";
//        String ends = "-pcsx2only";
//        String ends = "-GS_PAD";
        String ends = "-GS_PADHostLinux";
//        String ends = "-GS_PADHost";
//        String ends = "-GSonly";
//        String ends = "-renderersonly";

        final Path inputZipFirst = Path.of(
//                "/home/thienkhutu/Documents/work/cia-tsdv/testpcsx2/pcsx2-1.7.2503-only3.zip");
                "/home/thienkhutu/Documents/work/cia-tsdv/testpcsx2/pcsx2-1.7.2504" + ends +".zip");
        final Path outputProjectFirst = Path.of("local/first" + ends + ".proj");
		final Path inputZipSecond = Path.of(
//                "/home/thienkhutu/Documents/work/cia-tsdv/testpcsx2/pcsx2-1.7.2504-only3.zip");
                "/home/thienkhutu/Documents/work/cia-tsdv/testpcsx2/pcsx2-1.7.2504" + ends +".zip");
        final Path outputProjectSecond = Path.of("local/second" + ends + ".proj");

        System.out.println("first version");
        buildTree(inputZipFirst, outputProjectFirst);

        System.out.println("second version");
        buildTree(inputZipSecond, outputProjectSecond);

        System.out.println("Diff...");
        final long start_time = System.nanoTime();

        final Path oldPath = Path.of("local/first" + ends + ".proj");
        final Path newPath = Path.of("local/second" + ends + ".proj");

        try (final InputStream oldPathInputStream = Files.newInputStream(oldPath);
             final InputStream newPathInputStream = Files.newInputStream(newPath)) {
            final ProjectVersion projectVersion = ProjectVersion.fromInputStream(oldPathInputStream);
            final ProjectVersion projectVersion2 = ProjectVersion.fromInputStream(newPathInputStream);

            System.out.println((System.nanoTime() - start_time) / 1000000.0);

            final VersionDifference difference = VersionDiffer.compare(projectVersion, projectVersion2,
                    VersionDiffer.IMPACT_WEIGHT_MAP, 8);

            List<Pair<CppNode, CppNode>> list = new ArrayList<>(difference.getChangedNodes());
            list.sort(Comparator.comparing(o -> o.getA().getName()));
            try (final FileWriter fw = new FileWriter("local/changedNodes04" + ends +".txt")) {
                fw.append("class\tname\tunique name\n");
                for (Pair<CppNode, CppNode> p : list) {
                    fw.append(p.getA().getClass().getSimpleName()).append("\t")
                            .append(p.getA().getName()).append("\t")
                            .append(p.getA().getUniqueName()).append("\n");
                }
            }

            System.out.println((System.nanoTime() - start_time) / 1000000.0);
            try (final FileOutputStream fos = new FileOutputStream("local/diff" + ends + ".VersionDifference")) {
                difference.toOutputStream(fos);
                System.out.println((System.nanoTime() - start_time) / 1000000.0);
            }
        }

//		final Project project = new Project("project", List.of(projectVersion, projectVersion2), List.of(difference));
//		try (final FileOutputStream fos = new FileOutputStream("./test/project.Project")) {
//			project.toOutputStream(fos);
//		}
//		System.out.println((System.nanoTime() - start_time) / 1000000.0);
    }


    private static void buildTree(final Path inputZip, final Path outputProject) throws Exception {
        final long start_time = System.nanoTime();

        final String id = UUID.randomUUID().toString();
        final Path extractPath = Path.of("/tmp").resolve(id);

        try (final InputStream inputStream = Files.newInputStream(inputZip)) {
            Files.createDirectories(extractPath);
            final ProjectVersion projectVersion = createProjectVersion(id, extractPath, inputStream,
                    "", "");
//					"/src", "");

            System.out.println((System.nanoTime() - start_time) / 1000000.0);

            System.out.println(projectVersion.getRootNode().toTreeString());

            try (final OutputStream outputStream = Files.newOutputStream(outputProject)) {
                projectVersion.toOutputStream(outputStream);
                System.out.println((System.nanoTime() - start_time) / 1000000.0);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
