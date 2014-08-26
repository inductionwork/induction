/**
 *
 */
package org.induction.constantdata;

/**
 * Class contains different application constants
 *
 * @author Andrei Ilyin
 */
public class Constants {
	public static final boolean NOCACHE = false;

	public static final String CACHE_PATH = "C:\\cache";

	public static final String PROXY_ERROR = "HTTP/1.1 200 OK\nServer: Induction\n"
			+ "Content-Type: text/plain; charset=windows-1251\n\n";

	public static final String[] ESCAPE_CHARS = new String[]{"%D0%81",
			"%D0%90", "%D0%91", "%D0%92", "%D0%93", "%D0%94", "%D0%95",
			"%D0%96", "%D0%97", "%D0%98", "%D0%99", "%D0%9A", "%D0%9B",
			"%D0%9C", "%D0%9D", "%D0%9E", "%D0%9F", "%D0%A0", "%D0%A1",
			"%D0%A2", "%D0%A3", "%D0%A4", "%D0%A5", "%D0%A6", "%D0%A7",
			"%D0%A8", "%D0%A9", "%D0%AA", "%D0%AB", "%D0%AC", "%D0%AD",
			"%D0%AE", "%D0%AF", "%D0%B0", "%D0%B1", "%D0%B2", "%D0%B3",
			"%D0%B4", "%D0%B5", "%D0%B6", "%D0%B7", "%D0%B8", "%D0%B9",
			"%D0%BA", "%D0%BB", "%D0%BC", "%D0%BD", "%D0%BE", "%D0%BF",
			"%D1%80", "%D1%81", "%D1%82", "%D1%83", "%D1%84", "%D1%85",
			"%D1%86", "%D1%87", "%D1%88", "%D1%89", "%D1%8A", "%D1%8B",
			"%D1%8C", "%D1%8D", "%D1%8E", "%D1%8F", "%D1%91"};

	public static final String[] WIN1251_CHARS = new String[]{"%A8", "%C0",
			"%C1", "%C2", "%C3", "%C4", "%C5", "%C6", "%C7", "%C8", "%C9",
			"%CA", "%CB", "%CC", "%CD", "%CE", "%CF", "%D0", "%D1", "%D2",
			"%D3", "%D4", "%D5", "%D6", "%D7", "%D8", "%D9", "%DA", "%DB",
			"%DC", "%DD", "%DE", "%DF", "%E0", "%E1", "%E2", "%E3", "%E4",
			"%E5", "%E6", "%E7", "%E8", "%E9", "%EA", "%EB", "%EC", "%ED",
			"%EE", "%EF", "%F0", "%F1", "%F2", "%F3", "%F4", "%F5", "%F6",
			"%F7", "%F8", "%F9", "%FA", "%FB", "%FC", "%FD", "%FE", "%FF",
			"%B8"};

	public static final String[] RUSSIAN_CHARS = new String[]{"Ё", "А", "Б",
			"В", "Г", "Д", "Е", "Ж", "З", "И", "Й", "К", "Л", "М", "Н", "О",
			"П", "Р", "С", "Т", "У", "Ф", "Х", "Ц", "Ч", "Ш", "Щ", "Ъ", "Ы",
			"Ь", "Э", "Ю", "Я", "а", "б", "в", "г", "д", "е", "ж", "з", "и",
			"й", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф", "х",
			"ц", "ч", "ш", "щ", "ъ", "ы", "ь", "э", "ю", "я", "ё"};
}
