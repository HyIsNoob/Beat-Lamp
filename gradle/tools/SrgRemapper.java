import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;

public class SrgRemapper {
    private static final Map<String, String> fieldMap = new HashMap<>();
    private static final Map<String, String> methodMap = new HashMap<>();
    private static final Map<String, String> superClasses = new HashMap<>();
    private static final Map<String, List<String>> interfaces = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: SrgRemapper <tsrgFile> <inJar> <outJar> [<mcJars>...]");
            return;
        }

        File tsrgFile = new File(args[0]);
        File inJar = new File(args[1]);
        File outJar = new File(args[2]);

        System.out.println("Loading TSRG mappings from " + tsrgFile.getName());
        loadTsrg(tsrgFile);
        System.out.println("Loaded " + fieldMap.size() + " fields and " + methodMap.size() + " methods.");

        for (int i = 3; i < args.length; i++) {
            File mcJar = new File(args[i]);
            if (mcJar.exists()) {
                System.out.println("Indexing MC hierarchy from: " + mcJar.getName());
                indexHierarchy(mcJar);
            }
        }

        // Index input mod jar
        indexHierarchy(inJar);
        System.out.println("Indexed hierarchy for " + superClasses.size() + " classes.");

        HierarchyRemapper remapper = new HierarchyRemapper();

        System.out.println("Remapping JAR: " + inJar.getName() + " -> " + outJar.getName());

        try (JarFile jarFile = new JarFile(inJar);
             JarOutputStream jos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outJar)))) {

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        ClassReader cr = new ClassReader(is);
                        ClassWriter cw = new ClassWriter(0);
                        ClassVisitor cv = new ClassRemapper(cw, remapper);
                        cr.accept(cv, 0);

                        JarEntry newEntry = new JarEntry(name);
                        jos.putNextEntry(newEntry);
                        jos.write(cw.toByteArray());
                        jos.closeEntry();
                    }
                } else {
                    JarEntry newEntry = new JarEntry(name);
                    jos.putNextEntry(newEntry);
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        is.transferTo(jos);
                    }
                    jos.closeEntry();
                }
            }
        }

        System.out.println("Remapping complete! Output size: " + outJar.length() + " bytes");
    }

    private static void loadTsrg(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currentClass = null;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("tsrg2")) {
                    continue;
                }

                if (line.startsWith("\t\t")) {
                    continue;
                } else if (line.startsWith("\t")) {
                    if (currentClass == null) continue;
                    String memberLine = line.substring(1).trim();
                    String[] parts = memberLine.split("\\s+");

                    if (parts.length == 2) {
                        // Field: srgName mojangName
                        String srgName = parts[0];
                        String mojangName = parts[1];
                        fieldMap.put(currentClass + "." + mojangName, srgName);
                    } else if (parts.length >= 3) {
                        // Method: srgName srgDesc mojangName
                        String srgName = parts[0];
                        String desc = parts[1];
                        String mojangName = parts[2];
                        methodMap.put(currentClass + "." + mojangName + desc, srgName);
                    }
                } else {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        currentClass = parts[1];
                    } else if (parts.length == 1) {
                        currentClass = parts[0];
                    }
                }
            }
        }
    }

    private static void indexHierarchy(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ClassReader cr = new ClassReader(is);
                        String className = cr.getClassName();
                        String superName = cr.getSuperName();
                        if (superName != null && !superName.equals("java/lang/Object")) {
                            superClasses.put(className, superName);
                        }
                        String[] ifaces = cr.getInterfaces();
                        if (ifaces != null && ifaces.length > 0) {
                            interfaces.put(className, Arrays.asList(ifaces));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to index jar: " + jarFile.getName() + " -> " + e.getMessage());
        }
    }

    private static class HierarchyRemapper extends Remapper {
        @Override
        public String mapFieldName(String owner, String name, String descriptor) {
            String curr = owner;
            while (curr != null && !curr.equals("java/lang/Object")) {
                String srg = fieldMap.get(curr + "." + name);
                if (srg != null) return srg;
                curr = superClasses.get(curr);
            }
            return name;
        }

        @Override
        public String mapMethodName(String owner, String name, String descriptor) {
            String srg = findMethod(owner, name, descriptor);
            return srg != null ? srg : name;
        }

        @Override
        public String mapInvokeDynamicMethodName(String name, String descriptor) {
            try {
                Type returnType = Type.getReturnType(descriptor);
                if (returnType.getSort() == Type.OBJECT) {
                    String owner = returnType.getInternalName();
                    String srg = findMethodByName(owner, name);
                    if (srg != null) return srg;
                }
            } catch (Exception ignored) {
            }
            return name;
        }

        private String findMethodByName(String owner, String name) {
            if (owner == null || owner.equals("java/lang/Object")) return null;

            String prefix = owner + "." + name + "(";
            for (Map.Entry<String, String> entry : methodMap.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    return entry.getValue();
                }
            }

            String superCls = superClasses.get(owner);
            if (superCls != null) {
                String res = findMethodByName(superCls, name);
                if (res != null) return res;
            }

            List<String> ifaces = interfaces.get(owner);
            if (ifaces != null) {
                for (String iface : ifaces) {
                    String res = findMethodByName(iface, name);
                    if (res != null) return res;
                }
            }

            return null;
        }

        private String findMethod(String owner, String name, String desc) {
            if (owner == null || owner.equals("java/lang/Object")) return null;

            // Direct check
            String srg = methodMap.get(owner + "." + name + desc);
            if (srg != null) return srg;

            // Check superclass
            String superCls = superClasses.get(owner);
            if (superCls != null) {
                String res = findMethod(superCls, name, desc);
                if (res != null) return res;
            }

            // Check interfaces
            List<String> ifaces = interfaces.get(owner);
            if (ifaces != null) {
                for (String iface : ifaces) {
                    String res = findMethod(iface, name, desc);
                    if (res != null) return res;
                }
            }

            return null;
        }
    }
}
