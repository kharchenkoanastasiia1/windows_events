package org.windows_events.service;

import java.io.File;

public class ShortcutUtil {

    public static void createShortcut(String targetExe,
                                      String shortcutPath,
                                      String description) throws Exception {

        String psCommand =
                "$WshShell = New-Object -ComObject WScript.Shell;\n" +
                        "$Shortcut = $WshShell.CreateShortcut('" + shortcutPath + "');\n" +
                        "$Shortcut.TargetPath = '" + targetExe + "';\n" +
                        "$Shortcut.WorkingDirectory = '" + new File(targetExe).getParent() + "';\n" +
                        "$Shortcut.Description = '" + description + "';\n" +
                        "$Shortcut.Save();";

        ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-ExecutionPolicy", "Bypass",
                "-Command", psCommand
        );

        pb.inheritIO();
        Process p = pb.start();
        int exitCode = p.waitFor();

        if (exitCode != 0) {
            throw new IllegalStateException(
                    "Failed to create shortcut. PowerShell exit code: "
                            + exitCode
            );
        }
    }
}
