/*
 * Copyright (C) 2016 Baidu, Inc. All Rights Reserved.
 */
package com.dodola.rocoofix.utils

import com.android.SdkConstants
import com.android.build.gradle.api.BaseVariant
import com.google.common.collect.Sets
import groovy.xml.Namespace
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.apache.tools.ant.util.JavaEnvUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task

public class RocooUtils {
    private static final String MAP_SEPARATOR = ":"

    public static boolean notSame(Map map, String name, String hash) {
        def notSame = false
        if (map) {
            def value = map.get(name)
            println("notSame-------->" + value + "," + name + "," + hash)
            if (value) {
                if (!value.equals(hash)) {
                    notSame = true
                }
            } else {
                notSame = true
            }
        }
        return notSame
    }

    public static Map parseMap(File hashFile) {
        def hashMap = [:]
        if (hashFile.exists()) {
            hashFile.eachLine {
                List list = it.split(MAP_SEPARATOR)
                if (list.size() == 2) {
                    hashMap.put(list[0], list[1])
                }
            }
        } else {
            println "$hashFile does not exist"
        }
        return hashMap
    }

    public static format(String path, String hash) {
        return path + MAP_SEPARATOR + hash + "\n"
    }


    public static String getApplication(File manifestFile) {
        def manifest = new XmlParser().parse(manifestFile)
        def androidTag = new Namespace("http://schemas.android.com/apk/res/android", 'android')
        return manifest.application[0].attribute(androidTag.name)
    }

    private static List<String> getFilesHash(String baseDirectoryPath, File directoryFile) {
        List<String> javaFiles = new ArrayList<String>();

        File[] children = directoryFile.listFiles();
        if (children == null) {
            return javaFiles;
        }

        for (final File file : children) {
            if (file.isDirectory()) {
                List<String> tempList = getFilesHash(baseDirectoryPath, file);
                if (!tempList.isEmpty()) {
                    javaFiles.addAll(tempList);
                }
            } else {
                InputStream is = new FileInputStream(file);
                def hash = DigestUtils.shaHex(IOUtils.toByteArray(is))
                javaFiles.add(hash)

                is.close()
            }
        }

        return javaFiles;
    }

    /**
     * 主要用于处理使用Android Annotation产生的class文件
     * @param classDir
     */
    private static void deleteInvalidFile(File classDir){
        if (classDir.listFiles() != null && classDir.listFiles().size()) {
            for(File file : classDir.listFiles()){
                if(file.isDirectory()){
                    deleteInvalidFile(file);
                } else {
                    String fileName = file.getName();
                    int pos = fileName.indexOf('_$');
                    if(pos != -1){
                        String rawClassName = fileName.subSequence(0, pos);
                        File rawFile = new File(file.getParentFile(), rawClassName + ".class");
                        if(!rawFile.exists()){
                            file.delete();
                            continue;
                        }
                    }

                    if(fileName.endsWith('_.class')){
                        String rawClassName = fileName.subSequence(0, fileName.length() - 7);
                        File rawFile = new File(file.getParentFile(), rawClassName + ".class");
                        if(!rawFile.exists()){
                            file.delete();
                            continue;
                        }
                    }
                }
            }
        }

        if(classDir.isDirectory()){
            String[] list = classDir.list();
            if(list != null && list.length == 0){
                classDir.delete();
            }
        }
    }

    public static makeDex(Project project, File classDir, File patchJar) {
        deleteInvalidFile(classDir);
        if (classDir.listFiles() != null && classDir.listFiles().size()) {
//            StringBuilder builder = new StringBuilder();
//
//            def baseDirectoryPath = classDir.getAbsolutePath() + "/";
//            getFilesHash(baseDirectoryPath, classDir).each {
//                builder.append(it)
//            }
//            def hash = DigestUtils.shaHex(builder.toString().bytes)

            def sdkDir

            Properties properties = new Properties()
            File localProps = project.rootProject.file("local.properties")
            if (localProps.exists()) {
                properties.load(localProps.newDataInputStream())
                sdkDir = properties.getProperty("sdk.dir")
            } else {
                sdkDir = System.getenv("ANDROID_HOME")
            }

            println("-----------sdkDir:" + sdkDir)
            if (sdkDir) {
                def cmdExt = Os.isFamily(Os.FAMILY_WINDOWS) ? '.bat' : ''
                def stdout = new ByteArrayOutputStream()
                project.exec {
                    commandLine "${sdkDir}/build-tools/${project.android.buildToolsVersion}/dx${cmdExt}",
                            '--dex',
                            "--output=${patchJar.absolutePath}",
                            "${classDir.absolutePath}"
                    standardOutput = stdout
                }
                def error = stdout.toString().trim()
                if (error) {
                    println "dex error:" + error
                }
            } else {
            }
        }
    }

    /**
     * 签名补丁
     */
    public static signJar(File patchFile, File storeFile, String keyPassword, String storePassword, String keyAlias) {
        if(!patchFile.exists() || !storeFile.exists() || keyPassword == null || storePassword == null || keyAlias==null) {
            return
        }

        def args = [JavaEnvUtils.getJdkExecutable('jarsigner'),
                    '-verbose',
                    '-sigalg', 'MD5withRSA',
                    '-digestalg', 'SHA1',
                    '-keystore', storeFile.absolutePath,
                    '-keypass', keyPassword,
                    '-storepass', storePassword,
                    patchFile.absolutePath,
                    keyAlias]

        def proc = args.execute()
        def outRedir = new StreamRedir(proc.inputStream, System.out)
        def errRedir = new StreamRedir(proc.errorStream, System.out)

        outRedir.start()
        errRedir.start()

        def result = proc.waitFor()
        outRedir.join()
        errRedir.join()

        if (result != 0) {
            throw new GradleException('Couldn\'t sign')
        }
    }

    static String getProcessManifestTaskName(Project project, BaseVariant variant) {
        return "process${variant.name.capitalize()}Manifest"
    }

    static String getProGuardTaskName(Project project, BaseVariant variant) {
        if (isUseTransformAPI(project)) {
            return "transformClassesAndResourcesWithProguardFor${variant.name.capitalize()}"
        } else {
            return "proguard${variant.name.capitalize()}"
        }
    }

    static String getPreDexTaskName(Project project, BaseVariant variant) {
        if (isUseTransformAPI(project)) {
            return ""
        } else {
            return "preDex${variant.name.capitalize()}"
        }
    }

    static String getDexTaskName(Project project, BaseVariant variant) {
        if (isUseTransformAPI(project)) {
            return "transformClassesWithDexFor${variant.name.capitalize()}"
        } else {
            return "dex${variant.name.capitalize()}"
        }
    }


    static Set<File> getDexTaskInputFiles(Project project, BaseVariant variant, Task dexTask) {
        if (dexTask == null) {
            dexTask = project.tasks.findByName(getDexTaskName(project, variant));
        }

        if (isUseTransformAPI(project)) {
            def extensions = [SdkConstants.EXT_JAR] as String[]

            Set<File> files = Sets.newHashSet();

            dexTask.inputs.files.files.each {
                if (it.exists()) {
                    println("--------->" + it.absolutePath+","+"intermediates/classes/${variant.dirName}")
                    if (it.isDirectory()) {
                        Collection<File> jars = FileUtils.listFiles(it, extensions, true);
                        files.addAll(jars)

                        if (it.absolutePath.toLowerCase().endsWith("intermediates/classes/${variant.dirName}".toLowerCase())) {
                            files.add(it)
                        }
                    } else if (it.name.endsWith(SdkConstants.DOT_JAR)) {
                        files.add(it)
                    }
                }
            }
            return files
        } else {
            return dexTask.inputs.files.files;
        }
    }


    public static boolean isUseTransformAPI(Project project) {
//        println("==========gradleVersion:" + compareVersionName("1.9.0", "1.4.0"))
        return compareVersionName(project.gradle.gradleVersion, "1.4.0") >= 0;
    }


    private static int compareVersionName(String str1, String str2) {
        String[] thisParts = str1.split("-")[0].split("\\.");
        String[] thatParts = str2.split("-")[0].split("\\.");
        int length = Math.max(thisParts.length, thatParts.length);
        for (int i = 0; i < length; i++) {
            int thisPart = i < thisParts.length ?
                    Integer.parseInt(thisParts[i]) : 0;
            int thatPart = i < thatParts.length ?
                    Integer.parseInt(thatParts[i]) : 0;
            if (thisPart < thatPart)
                return -1;
            if (thisPart > thatPart)
                return 1;
        }
        return 0;
    }

    static class StreamRedir extends Thread {
        private inStream
        private outStream

        public StreamRedir(inStream, outStream) {
            this.inStream = inStream
            this.outStream = outStream
        }

        public void run() {
            int b;
            while ((b = inStream.read()) != -1)
                outStream.write(b)
        }
    }
}