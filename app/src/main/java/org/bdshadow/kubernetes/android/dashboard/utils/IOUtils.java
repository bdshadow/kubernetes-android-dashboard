package org.bdshadow.kubernetes.android.dashboard.utils;

import java.io.FileInputStream;
import java.util.Scanner;

public class IOUtils {

    private IOUtils() {

    }

    public static String readFile(FileInputStream fileInputStream) {
        try (Scanner scanner = new Scanner(fileInputStream)) {
            StringBuilder fileContent = new StringBuilder();
            while (scanner.hasNextLine()) {
                fileContent.append(scanner.nextLine()).append("\n");
            }
            return fileContent.toString().trim();
        }
    }

}
