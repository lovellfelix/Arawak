package com.lovellfelix.arawak.parser;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Xml;

import com.lovellfelix.arawak.Constants;
import com.lovellfelix.arawak.MainApplication;
import com.lovellfelix.arawak.provider.FeedData.FeedColumns;
import com.lovellfelix.arawak.provider.FeedData.FilterColumns;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class OPML {

    public static final String BACKUP_OPML = Environment.getExternalStorageDirectory() + "Arawak/Arawak_auto_backup.opml";

    private static final String[] FEEDS_PROJECTION = new String[]{FeedColumns._ID, FeedColumns.IS_GROUP, FeedColumns.NAME, FeedColumns.URL,
            FeedColumns.RETRIEVE_FULLTEXT};
    private static final String[] FILTERS_PROJECTION = new String[]{FilterColumns.FILTER_TEXT, FilterColumns.IS_REGEX,
            FilterColumns.IS_APPLIED_TO_TITLE};

    private static final String START = "<?xml version='1.0' encoding='utf-8'?>\n<opml version='1.1'>\n<head>\n<title>Arawak export</title>\n<dateCreated>";
    private static final String AFTER_DATE = "</dateCreated>\n</head>\n<body>\n";
    private static final String OUTLINE_TITLE = "\t<outline title='";
    private static final String OUTLINE_XMLURL = "' type='rss' xmlUrl='";
    private static final String OUTLINE_RETRIEVE_FULLTEXT = "' retrieveFullText='";
    private static final String OUTLINE_INLINE_CLOSING = "'/>\n";
    private static final String OUTLINE_NORMAL_CLOSING = "'>\n";
    private static final String OUTLINE_END = "\t</outline>\n";
    private static final String FILTER_TEXT = "\t\t<filter text='";
    private static final String FILTER_IS_REGEX = "' isRegex='";
    private static final String FILTER_IS_APPLIED_TO_TITLE = "' isAppliedToTitle='";
    private static final String FILTER_CLOSING = "'/>\n";
    private static final String CLOSING = "</body>\n</opml>\n";

    private static final OPMLParser parser = new OPMLParser();
    private static boolean autoBackupEnabled = true;

    public static void importFromFile(String filename) throws IOException, SAXException {
        if (BACKUP_OPML.equals(filename)) {
            autoBackupEnabled = false;  // Do not write the auto backup file while reading it...
        }

        try {
            Xml.parse(new InputStreamReader(new FileInputStream(filename)), parser);
        } finally {
            autoBackupEnabled = true;
        }
    }

    public static void importFromFile(InputStream input) throws IOException, SAXException {
        Xml.parse(new InputStreamReader(input), parser);
    }

    public static void exportToFile(String filename) throws IOException {
        if (BACKUP_OPML.equals(filename) && !autoBackupEnabled) {
            return;
        }

        Cursor cursor = MainApplication.getContext().getContentResolver()
                .query(FeedColumns.GROUPS_CONTENT_URI, FEEDS_PROJECTION, null, null, null);

        StringBuilder builder = new StringBuilder(START);
        builder.append(System.currentTimeMillis());
        builder.append(AFTER_DATE);

        while (cursor.moveToNext()) {
            builder.append(OUTLINE_TITLE);
            builder.append(cursor.isNull(2) ? "" : TextUtils.htmlEncode(cursor.getString(2)));
            if (cursor.getInt(1) == 1) { // If it is a group
                builder.append(OUTLINE_NORMAL_CLOSING);

                Cursor cursorChildren = MainApplication.getContext().getContentResolver()
                        .query(FeedColumns.FEEDS_FOR_GROUPS_CONTENT_URI(cursor.getString(0)), FEEDS_PROJECTION, null, null, null);
                while (cursorChildren.moveToNext()) {
                    builder.append("\t");
                    builder.append(OUTLINE_TITLE);
                    builder.append(cursorChildren.isNull(2) ? "" : TextUtils.htmlEncode(cursorChildren.getString(2)));
                    builder.append(OUTLINE_XMLURL);
                    builder.append(TextUtils.htmlEncode(cursorChildren.getString(3)));
                    builder.append(OUTLINE_RETRIEVE_FULLTEXT);
                    builder.append(cursorChildren.getInt(4) == 1 ? Constants.TRUE : "false");

                    Cursor cursorFilters = MainApplication.getContext().getContentResolver()
                            .query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(cursorChildren.getString(0)), FILTERS_PROJECTION, null, null, null);
                    if (cursorFilters.getCount() != 0) {
                        builder.append(OUTLINE_NORMAL_CLOSING);
                        while (cursorFilters.moveToNext()) {
                            builder.append("\t");
                            builder.append(FILTER_TEXT);
                            builder.append(TextUtils.htmlEncode(cursorFilters.getString(0)));
                            builder.append(FILTER_IS_REGEX);
                            builder.append(cursorFilters.getInt(1) == 1 ? Constants.TRUE : "false");
                            builder.append(FILTER_IS_APPLIED_TO_TITLE);
                            builder.append(cursorFilters.getInt(2) == 1 ? Constants.TRUE : "false");
                            builder.append(FILTER_CLOSING);
                        }
                        builder.append("\t");
                        builder.append(OUTLINE_END);
                    } else {
                        builder.append(OUTLINE_INLINE_CLOSING);
                    }
                    cursorFilters.close();
                }
                cursorChildren.close();

                builder.append(OUTLINE_END);
            } else {
                builder.append(OUTLINE_XMLURL);
                builder.append(TextUtils.htmlEncode(cursor.getString(3)));
                builder.append(OUTLINE_RETRIEVE_FULLTEXT);
                builder.append(cursor.getInt(4) == 1 ? Constants.TRUE : "false");
                Cursor cursorFilters = MainApplication.getContext().getContentResolver()
                        .query(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(cursor.getString(0)), FILTERS_PROJECTION, null, null, null);
                if (cursorFilters.getCount() != 0) {
                    builder.append(OUTLINE_NORMAL_CLOSING);
                    while (cursorFilters.moveToNext()) {
                        builder.append(FILTER_TEXT);
                        builder.append(TextUtils.htmlEncode(cursorFilters.getString(0)));
                        builder.append(FILTER_IS_REGEX);
                        builder.append(cursorFilters.getInt(1) == 1 ? Constants.TRUE : "false");
                        builder.append(FILTER_IS_APPLIED_TO_TITLE);
                        builder.append(cursorFilters.getInt(2) == 1 ? Constants.TRUE : "false");
                        builder.append(FILTER_CLOSING);
                    }
                    builder.append(OUTLINE_END);
                } else {
                    builder.append(OUTLINE_INLINE_CLOSING);
                }
                cursorFilters.close();
            }
        }
        builder.append(CLOSING);

        cursor.close();

        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

        writer.write(builder.toString());
        writer.close();
    }

    private static class OPMLParser extends DefaultHandler {
        private static final String TAG_BODY = "body";
        private static final String TAG_OUTLINE = "outline";
        private static final String ATTRIBUTE_TITLE = "title";
        private static final String ATTRIBUTE_XMLURL = "xmlUrl";
        private static final String ATTRIBUTE_RETRIEVE_FULLTEXT = "retrieveFullText";
        private static final String TAG_FILTER = "filter";
        private static final String ATTRIBUTE_TEXT = "text";
        private static final String ATTRIBUTE_IS_REGEX = "isRegex";
        private static final String ATTRIBUTE_IS_APPLIED_TO_TITLE = "isAppliedToTitle";
        private static final String ATTRIBUTE_SUBSCRIPTION = "subscription";

        private boolean bodyTagEntered = false;
        private boolean feedEntered = false;
        private boolean probablyValidElement = false;
        private String groupId = null;
        private String feedId = null;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (!bodyTagEntered) {
                if (TAG_BODY.equals(localName)) {
                    bodyTagEntered = true;
                    probablyValidElement = true;
                }
            } else if (TAG_OUTLINE.equals(localName)) {
                String url = attributes.getValue("", ATTRIBUTE_XMLURL);
                String title = attributes.getValue("", ATTRIBUTE_TITLE);

                ContentResolver cr = MainApplication.getContext().getContentResolver();

                if (url == null) { // No url => this is a group
                    if (title != null) {
                        ContentValues values = new ContentValues();
                        values.put(FeedColumns.IS_GROUP, true);
                        values.put(FeedColumns.NAME, title);

                        Cursor cursor = cr.query(FeedColumns.GROUPS_CONTENT_URI, null, FeedColumns.NAME + Constants.DB_ARG, new String[]{title}, null);

                        if (!cursor.moveToFirst()) {
                            groupId = cr.insert(FeedColumns.GROUPS_CONTENT_URI, values).getLastPathSegment();
                        }
                        cursor.close();
                    }

                } else { // Url => this is a feed
                    feedEntered = true;
                    ContentValues values = new ContentValues();

                    values.put(FeedColumns.URL, url);
                    values.put(FeedColumns.NAME, title != null && title.length() > 0 ? title : null);
                    if (groupId != null) {
                        values.put(FeedColumns.GROUP_ID, groupId);
                    }
                    values.put(FeedColumns.RETRIEVE_FULLTEXT, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_RETRIEVE_FULLTEXT)));
                    values.put(FeedColumns.SUBSCRIPTION, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_SUBSCRIPTION)));

                    Cursor cursor = cr.query(FeedColumns.CONTENT_URI, null, FeedColumns.URL + Constants.DB_ARG,
                            new String[]{url}, null);
                    feedId = null;
                    if (!cursor.moveToFirst()) {
                        feedId = cr.insert(FeedColumns.CONTENT_URI, values).getLastPathSegment();
                    }
                    cursor.close();
                }
            } else if (TAG_FILTER.equals(localName)) {
                if (feedEntered && feedId != null) {
                    ContentValues values = new ContentValues();
                    values.put(FilterColumns.FILTER_TEXT, attributes.getValue("", ATTRIBUTE_TEXT));
                    values.put(FilterColumns.IS_REGEX, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_REGEX)));
                    values.put(FilterColumns.IS_APPLIED_TO_TITLE, Constants.TRUE.equals(attributes.getValue("", ATTRIBUTE_IS_APPLIED_TO_TITLE)));

                    ContentResolver cr = MainApplication.getContext().getContentResolver();
                    cr.insert(FilterColumns.FILTERS_FOR_FEED_CONTENT_URI(feedId), values);
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (bodyTagEntered && TAG_BODY.equals(localName)) {
                bodyTagEntered = false;
            } else if (TAG_OUTLINE.equals(localName)) {
                if (feedEntered) {
                    feedEntered = false;
                } else {
                    groupId = null;
                }
            }
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            // ignore warnings
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            // ignore small errors
        }

        @Override
        public void endDocument() throws SAXException {
            if (!probablyValidElement) {
                throw new SAXException();
            } else {
                super.endDocument();
            }
        }
    }
}
