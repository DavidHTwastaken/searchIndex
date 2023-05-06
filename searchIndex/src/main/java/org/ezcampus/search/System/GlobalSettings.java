package org.ezcampus.search.System;

import org.tinylog.Logger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GlobalSettings
{
    public static final String BRAND = "searchIndex";
    public static final String BRAND_LONG = "SchedulePlatform-" + BRAND;
    public static boolean IS_DEBUG = false;

    public static final int THUMBNAIL_SIZE = 256;


    public static final Path LOGS_DIR_PATH = Paths.get(".", "logs");

    public static Boolean isLinux = false;

}