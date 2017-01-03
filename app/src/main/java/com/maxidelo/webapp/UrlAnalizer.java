package com.maxidelo.webapp;

import org.apache.commons.lang3.StringUtils;

public class UrlAnalizer {

    private static final String[] downloadeableExtensions = new String[]{".dd",
            ".xz",
            ".swf",
            ".pmd",
            ".csv",
            ".dm",
            ".rmf",
            ".jad",
            ".ogg",
            ".xmf",
            ".lzma",
            ".mot",
            ".mov",
            ".tar.xz",
            ".ics",
            ".jar",
            ".ipa",
            ".apk",
            ".3g2",
            ".mtf",
            ".qcp",
            ".ogv",
            ".prc",
            ".zip",
            ".tar.gz",
            ".rar",
            ".7z",
            ".mld",
            ".tar",
            ".tar.bz2",
            ".xap",
            ".wgz",
            ".tar.sz",
            ".ott",
            ".webm",
            ".pptx",
            ".au",
            ".html5.zip",
            ".mpeg",
            ".doc",
            ".utz",
            ".rm",
            ".tar.Z",
            ".aac",
            ".sdt",
            ".tar.lzma",
            ".aiff",
            ".cab",
            ".gif",
            ".mmf",
            ".amr",
            ".mid",
            ".ems",
            ".wma",
            ".emy",
            ".ppsx",
            ".imy",
            ".avi",
            ".tsv",
            ".epub",
            ".vcf",
            ".arj",
            ".Z",
            ".bz2",
            ".3gp",
            ".sis",
            ".seq",
            ".jpg",
            ".wmv",
            ".xlsx",
            ".nth",
            ".rtf",
            ".vcs",
            ".sz",
            ".wav",
            ".awb",
            ".m4v",
            ".obb",
            ".docx",
            ".mp4",
            ".mp3",
            ".flv",
            ".txt",
            ".pps",
            ".pdf",
            ".gz",
            ".srt",
            ".ppt",
            ".thm",
            ".cod",
            ".cpio",
            ".xls"
    };


    public static boolean isApkDownload(String url) {
        return StringUtils.endsWith(url, ".apk");
    }


    public static boolean isBrowserUrl(String url) {
        return StringUtils.startsWithAny(url, "http", "https");
    }

    public static boolean isDownloadeableContent(String url) {
        return StringUtils.endsWithAny(url, downloadeableExtensions);
    }
}
