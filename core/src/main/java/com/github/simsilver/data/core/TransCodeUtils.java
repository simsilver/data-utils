package com.github.simsilver.data.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class TransCodeUtils {

    static String[][] charDetList = {
            {"ISO-8859-6", "windows-1256"},//Arabic
            {"ISO-8859-5", "windows-1251"},//Bulgarian
            {"KOI8-R", "windows-1251", "ISO-8859-5", "MAC-CYRILLIC", "IBM866", "IBM855"},//Cyrillic
            {"ISO-8859-15", "ISO-8859-1", "windows-1252"},//Danish
            {"ISO-8859-3"},//Esperanto
            {"ISO-8859-15", "windows-1252", "ISO-8859-1"},//French
            {"windows-1252", "ISO-8859-1"},//German
            {"windows-1253", "ISO-8859-7"},//Greek
            {"windows-1255"},//Hebrew
            {"ISO-8859-2", "windows-1250"},//Hungarian
            {"windows-1252", "ISO-8859-1", "ISO-8859-15"},//Spanish
            {"ISO-8859-11", "TIS-620"},//Thai
            {"ISO-8859-9", "ISO-8859-3"},//Turkish
            {"VISCII", "windows-1258"},//Vietnamese
            {"HZ-GB-2312", "ISO-2022-CN", "ISO-2022-JP", "ISO-2022-KR"},//EscSM
            {"Big5", "EUC-JP", "EUC-KR", "EUC-TW", "GB2312", "GB18030", "Shift_JIS", "UTF-16BE", "UTF-16LE", "UTF-8"},//MBCS
            {"UTF-8", "X-ISO-10646-UCS-4-3412", "UTF-16BE", "UTF-32BE", "X-ISO-10646-UCS-4-2143", "UTF-32LE", "UTF-16LE", "UTF-7", "UTF-1", "UTF-EBCDIC", "SCSU", "BOCU-1", "GB18030"},//Universal
    };
    static ArrayList<Charset> sCharSetList;

    static {
        Map<String, Charset> map = Charset.availableCharsets();
        Set<Charset> csSet = new HashSet<>();
        for (String[] charsetNames : charDetList) {
            for (String charsetName : charsetNames) {
                Charset cs = map.get(charsetName);
                if (cs != null) {
                    csSet.add(cs);
                }
            }
        }
        sCharSetList = new ArrayList<>(csSet);
    }

    public static SortedMap<Float, String> testEncoding(ByteBuffer bFrom, CharBuffer cTo, boolean endOfInput) {
        SortedMap<Float, String> csMap = new TreeMap<>();
        ArrayList<Charset> csList = new ArrayList<>(sCharSetList);
        int size = csList.size();
        int idx = 0;
        for (Charset cs : csList) {
            String charName = cs.name();
            CharsetDecoder cd = cs.newDecoder();
            try {
                bFrom.rewind();
                cTo.clear();
                CoderResult cr = cd.decode(bFrom, cTo, endOfInput);
                if (cr.isError()) {
                    continue;
                }
            } catch (Throwable e) {
                continue;
            }
            float value = -(cd.maxCharsPerByte() * cd.maxCharsPerByte() / cd.averageCharsPerByte());
            value -= idx / (float) (size * 10);
            csMap.put(value, charName);
            idx++;
        }
        return csMap;
    }

    public static boolean decodeAsCharset(Charset cs, byte[] buffer, int size, InputStream in, OutputStream out, StringBuilder preview) throws IOException {
        if (buffer == null || size == 0) {
            buffer = new byte[4 * 1024];
            size = in.read(buffer);
        }
        ByteBuffer bFrom = ByteBuffer.wrap(buffer);
        CharBuffer cTo = CharBuffer.allocate(buffer.length);
        CharsetDecoder cd = cs.newDecoder();
        while (size > 0) {
            bFrom.limit(size);
            CoderResult cr;
            try {
                cr = cd.decode(bFrom, cTo, size < buffer.length);
            } catch (Throwable ignored) {
                return false;
            }
            cTo.flip();
            int start = 0;
            int left = buffer.length;
            if (bFrom.remaining() > 0) {
                start = bFrom.remaining();
                left = buffer.length - start;
                System.arraycopy(buffer, bFrom.position(), buffer, 0, bFrom.remaining());
            }
            String res = cTo.toString();
            if (preview != null && preview.length() < 1024) {
                preview.append(res);
            }
            out.write(res.getBytes(StandardCharsets.UTF_8));
            bFrom.clear();
            cTo.clear();
            if (cr == null) {
                return false;
            }
            if (cr.isOverflow()) {
                return false;
            }
            if (cr.isError()) {
                // TODO: error handle
                return false;
            }
            size = in.read(buffer, start, left);
            size += start;
        }
        return true;
    }

    public static boolean testDecodeUriStream(InputStream in, OutputStream out, StringBuilder preview, ArrayList<String> list) throws IOException {
        byte[] buffer = new byte[4 * 1024];
        int size = in.read(buffer);
        ByteBuffer bFrom = ByteBuffer.wrap(buffer);
        CharBuffer cTo = CharBuffer.allocate(buffer.length);
        SortedMap<Float, String> csMap = testEncoding(bFrom, cTo, size < buffer.length);
        Collection<String> v = csMap.values();
        if (list != null) {
            list.addAll(csMap.values());
        }
        if (!v.isEmpty()) {
            String charName = v.iterator().next();
            Charset cs = Charset.forName(charName);
            return decodeAsCharset(cs, buffer, size, in, out, preview);
        }
        return false;
    }
}
