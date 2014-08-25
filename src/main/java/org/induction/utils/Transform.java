package org.induction.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.induction.constantdata.Constants.*;

public class Transform {
	public static final String HTTP_ELEMENT_CHARSET = "US-ASCII";

	/**
	 * Method that extracts a substring from a Header string using the
	 * parameters start and the end of the substring
	 *
	 * @param str
	 * @param start
	 * @param end
	 * @return
	 */
	public static String extractFromHeader(String str, String start,
	                                       String end) {
		int s = str.indexOf("\n\n", 0), e;
		if (s < 0)
			s = str.indexOf("\r\n\r\n", 0);
		if (s > 0)
			str = str.substring(0, s);
		s = str.indexOf(start, 0) + start.length();
		if (s < start.length())
			return null;
		e = str.indexOf(end, s);
		if (e < 0)
			e = str.length();
		return (str.substring(s, e)).trim();
	}

	public static StringBuilder replaceAll(StringBuilder builder, String from,
	                                       String to) {
		StringBuilder replaceBuilder = builder;
		int index = replaceBuilder.indexOf(from);
		while (index != -1) {
			replaceBuilder.replace(index, index + from.length(), to);
			index += to.length(); // Move to the end of the replacement
			index = replaceBuilder.indexOf(from, index);
		}
		return replaceBuilder;
	}

	public static String replaceRegex(StringBuilder builder, String pattern,
	                                  String replacement) {
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(builder);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, replacement);
		}
		m.appendTail(sb);

		return sb.toString();
	}

	public static String getPath(String header) {
		String URI = extractFromHeader(header, "GET ", " "), path;
		if (URI == null)
			URI = extractFromHeader(header, "POST ", " ");
		if (URI == null)
			return null;

		path = URI.toLowerCase();
		if (path.length() >= 7 && path.indexOf("http://", 0) == 0)
			URI = URI.substring(7);
		else {
			path = extractFromHeader(header, "Host:", "\n");
			if (path == null) return null;
			URI = path + URI;
		}

		URI = Transform.fixURL(URI);
		URI = Transform.URL2File(URI, (byte) 0);
		path = CACHE_PATH + File.separator + URI;
		//System.out.println("Method getPath(fix+transform), Getted path: "+path);
		return path;

	}

	// Лечим глюки IE (приводим урл к стандарту)
	public static String fixURL(String URI) {
		//System.out.println("\tFIX URL Getted URI: "+URI);
		if (URI.length() >= 7 && URI.indexOf("http://", 0) == 0)
			URI = URI.substring(7);
		String s = URI;
		int startIndex = s.indexOf('#');
		if (startIndex >= 0) {
			StringBuilder sb = new StringBuilder(s);
			s = sb.delete(startIndex, sb.length()).toString();
		}

		// Делаем URL более однозначным
		// -Коды символов %xX в %XX (верхний регист)
		s = replaceToUpCase(s, Pattern.compile("%[A-Fa-f\\d]{2}"));
		// -Символ ' " ' в %22
		s = s.replace("\"", "%22");
		// -Символ ' < ' в %3C
		s = s.replace("<", "%3C");
		// -Символ ' > ' в %3E
		s = s.replace(">", "%3E");
		// -Символ ' ` ' в %60
		s = s.replace("`", "%60");

		int num1 = s.indexOf('?');
		s = (num1 < 0) ? s.replace("^", "%5E") : s.substring(0, num1).replace(
				"^", "%5E")
				+ s.substring(num1);
		s = escape_unicode_rus(s);
		if (s.indexOf('/') < 0 & s.length() > 0)
			s = s + '/';
		int num2 = s.indexOf('/');
		if (num2 >= 0)
			s = Pattern.compile("(:(80)?$)?").matcher(s.substring(0, num2))
					.replaceAll("").toLowerCase()
					+ s.substring(num2);

		int num3 = s.indexOf('/');
		char[] Chars = s.toCharArray();

		for (int index = 0; index < num3; ++index) {
			if (((int) Chars[index] < 48 | (int) Chars[index] > 57)
					& ((int) Chars[index] < 97 | (int) Chars[index] > 122)
					& ((int) Chars[index] != 45 & (int) Chars[index] != 46 & (int) Chars[index] != 58)) {
				s = "НЕПРАВИЛЬНЫЙ ХОСТ";
				break;
			}
		}
		//System.out.println("Transformed URI: "+s);
		return s;
	}

	public static String URL2File(String url, byte redir) {
		//System.out.println("To file: "+url);
		// Обрезаем 'http:\\' в URL чтобы не писать в путь файла
		if (url.length() >= 7
				&& url.substring(0, 7).toLowerCase().equals("http://")) {
			url = url.substring(7);
		}
	/*
	 * Преобразовать символы
	 * '*' 	в '#x' (совместимость)
	 * '\\' в '#~' (совместимость)
	 * '|' 	в '#i' (совместимость)
	 * ':'	в '#=' (НЕсовместимость!), ранее в '!', соответственно '!' ранее в '#I'
	 */

		String str1 = url.replace("*", "#x").replace("\\", "#~")
				.replace("|", "#i").replace(":", "#=");

		// Опционально:
		// Кодировать %22 (") - двойную ковычку
		// --в символ #'
		// str1 = str1.replace("%22", "#'");
		// --в символ ``
		// str1 = str1.replace("%22", "``");
		// Кодировать %3C (<) и %3E (>) - скобы
		// --в символы '#(' и '#)'
		// str1 = str1.replace("%3C", "#(").replace("%3E", "#)");
		// --в символы '#{' и '#}'
		// str1 = str1.replace("%3C", "#{").replace("%3E", "#}");

		/**
		 * Преобразование URL содержащих символ '?' (основная часть)
		 * Знак '?' преобразуется в '^\'
		 * за тем преобразуются символы:
		 * Символ 	до '?' 		после '?'	ранее
		 *  /		\		    #!		    \ и #%
		 *  //		\#!		    #!#!		\~ и #%~
		 *  ///		\#!#!		#!#!#!		\~\ и #%~#%
		 *  .\		.#n\		.#n\
		 *  пробел\	пробел#n\	пробел#n\
		 */
		int startIndex = str1.indexOf('?');
		String str2 = "";
		if (startIndex >= 0) {
			String str3 = str1.substring(startIndex + 1);

			StringBuilder sb = new StringBuilder(str1);

			str1 = sb.delete(startIndex, sb.length()).toString();

			str2 = "^\\" + str3.replace("/", "#!").replace("?", "#^");

		}

		String s = str1.replace("//", "\\#!").replace("#!\\", "#!#!")
				.replace('/', '\\').replace(".\\", ".#n\\")
				+ str2;

		// Преобразовывать русские коды Win-1251 в символы в имени файла
		// s = unescape_win1251_rus(s);

		// Преобразовывать коды русского Юникода в русские символы в имени файла
		s = unescape_unicode_rus(s);

		// Преобразовывать коды пробела %20 в символ пробела
		s = unescape_space(s).replace(" \\", " #n\\");

		// Использовать алгоритм шифрования CRC32 для длинных URL
		// [путь<maxUrlLength-15]\#-[CRC32(урл_без_хоста)]

		int maxUrlLength = 200;
		if (s.length() > maxUrlLength - 5) {
			int num1 = url.indexOf('/');
			int num2 = s.substring(0, maxUrlLength - 10 - 5).lastIndexOf('\\');
			s = s.substring(0, num2 + 1) + "#-"
					+ CRC32(url.substring(num1 + 1));
		}

		// Преобразовать редирект (301 Moved Permanently) к виду редирект#m
		if ((int) redir == 1)
			s = s + "#m";
		char[] Chars = s.toCharArray();
		if (s.length() > 0 && Chars[s.length() - 1] == 92
				| Chars[s.length() - 1] == 46 | Chars[s.length() - 1] == 32)
			s = s + "#_";

		//System.out.println("FilePath is: "+s);
		return s;
	}

	// Вычисление CRC32 кода строки
	public static String CRC32(String str) {
		CRC32 crc = new CRC32();
		crc.update(str.getBytes());
		long result = crc.getValue();
		return Long.toHexString(result).toUpperCase();
	}

	// Замена русских символов символов русского Юникода
	private static String escape_unicode_rus(String s) {
		for (int index = 0; index < RUSSIAN_CHARS.length; ++index)
			s = s.replace(RUSSIAN_CHARS[index], ESCAPE_CHARS[index]);
		s = s.replace(" ", "%20");
		return s;
	}

	// Замена кода пробела символом пробела в имени файла
	private static String unescape_space(String s) {
		s = s.replace("%20", " ");
		return s;
	}

	// Замена символов русского Юникода в русские символы в имени файла
	private static String unescape_unicode_rus(String s) {
		for (int index = 0; index < ESCAPE_CHARS.length; ++index)
			s = s.replace(ESCAPE_CHARS[index], RUSSIAN_CHARS[index]);
		return s;
	}

	// Замена русских кодов кодировки WIN1251 в русские символы в имени файла
	private static String unescape_win1251_rus(String s) {
		for (int index = 0; index < WIN1251_CHARS.length; ++index)
			s = s.replace(WIN1251_CHARS[index], RUSSIAN_CHARS[index]);
		return s;
	}

	//Преобразовать символы к верхнему регистру по шаблону
	public static String replaceToUpCase(String s, Pattern p) {
		StringBuilder text = new StringBuilder(s);
		Matcher m2 = p.matcher(text);
		int matchPointer = 0;// First search begins at the start of the string
		while (m2.find(matchPointer)) {
			matchPointer = m2.end(); // Next search starts from where this one
			// ended
			text.replace(m2.start(), m2.end(), m2.group().toUpperCase());
		}
		return text.toString();
	}

	public static byte[] readRawLine(InputStream inputStream) throws IOException {

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int ch;
		while ((ch = inputStream.read()) >= 0) {
			buf.write(ch);
			if (ch == '\n') {
				break;
			}
		}
		if (buf.size() == 0) {
			return null;
		}
		return buf.toByteArray();
	}

	public static String readLine(InputStream inputStream) throws IOException {
		byte[] rawdata = readRawLine(inputStream);
		if (rawdata == null) {
			return null;
		}
		int len = rawdata.length;
		int offset = 0;
		if (len > 0) {
			if (rawdata[len - 1] == '\n') {
				offset++;
				if (len > 1) {
					if (rawdata[len - 2] == '\r') {
						offset++;
					}
				}
			}
		}
		return getString(rawdata, 0, len - offset);
	}

	public static String getString(final byte[] data, int offset, int length) {

		if (data == null) {
			throw new IllegalArgumentException("Parameter may not be null");
		}

		try {
			return new String(data, offset, length, HTTP_ELEMENT_CHARSET);
		} catch (Exception e) {
			return new String(data, offset, length);
		}
	}
}
