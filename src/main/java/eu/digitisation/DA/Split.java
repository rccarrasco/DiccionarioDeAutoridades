/*
 * Copyright (C) 2014 Universidad de Alicante
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package eu.digitisation.DA;

import static eu.digitisation.DA.WordType.LOWERCASE;
import static eu.digitisation.DA.WordType.MIXED;
import static eu.digitisation.DA.WordType.UPPERCASE;
import eu.digitisation.layout.SortPageXML;
import eu.digitisation.log.Messages;
import eu.digitisation.text.CharFilter;
import eu.digitisation.text.StringNormalizer;
import eu.digitisation.xml.DocumentParser;
import eu.digitisation.xml.XPathFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author R.C.C. Diccionario de Autoridades Lista de problemas con los lemas:
 * Citas, por ejemplo "GARBI, Cart. 4". PENDING
 * <p>
 * I mayúscula transcrita incorréctamente como l. SOLVED</p>
 * <p>
 * ñ minúscula en el original. SOLVED</p>
 * <p>
 * Lemas multipalabra. SOLVED</p>
 * <p>
 * Comprobación de consistencia alfabética entre páginas consecutivas.
 * SOLVED</p>
 * <p>
 * Participios tras infinitivo, como "BIRLAR ... BIRLADO", violan orden
 * alfabético). SOLVED</p>
 *
 * <p>
 * Vacilación en el orden alfabético de B/V, X/J PENDING</p>
 *
 * <p>
 * Error en ordenación: puede estar causado por el lema actual, por el antecesor
 * o por ambos. UNSOLVABLE</p>
 *
 * @todo create collator for old Spanish
 */
public class Split {

    static XPathFilter selector;     // Selects XML elements with relevant content
    final static Collator collator;  // Defines the lexicographic order
    static CharFilter cfilter;       // Map PUA characters to standard characters

    static {
        String[] inclusions = {"TextRegion[@type='paragraph']"};
        InputStream is = Split.class.getResourceAsStream("/UnicodeCharEquivalences.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            selector = new XPathFilter(inclusions, null);
        } catch (XPathExpressionException ex) {
            Messages.severe(ex.getMessage());
        }
        collator = OldSpanishCollator.getInstance();
        cfilter = new CharFilter(true);
        cfilter.addCSV(reader);
    }

    /**
     *
     * @param text a string
     * @return the longest prefix of the text containing only uppercase letters
     */
    protected static String header(String text) {
        StringBuilder builder = new StringBuilder();
        String[] tokens = cfilter.translate(text).split("\\p{Space}+");

        for (String token : tokens) {
            String word = StringNormalizer.trim(token);

            switch (WordType.typeOf(word)) {
                case UPPERCASE: // header word
                    if (builder.length() > 0) {
                        builder.append(' ');
                    }
                    builder.append(token);
                    break;
                case LOWERCASE: // end of header
                    return builder.toString();
                case MIXED: // striking content
                    if (WordType.isFirstWordInSentence(word)) {
                        return builder.toString();
                    } else {
                        if (builder.length() > 0) {
                            builder.append(' ');
                        }
                        builder.append(token);
                    }
            }
        }
        return builder.toString();
    }

    /**
     *
     * @param e a document element
     * @return the longest prefix of the textual content containing only
     * uppercase letters
     * @throws IOException
     */
    protected static String header(Element e) throws IOException {
        String text = e.getTextContent().trim();
        return header(text);
    }

    /**
     *
     * @param doc an XML document
     * @return the firstWord sentences in every selected textual element
     * @throws IOException
     */
    public static List<String> headers(Document doc) throws IOException {
        List<String> list = new ArrayList<>();
        for (Element e : selector.selectElements(doc)) {
            String head = header(e);
            if (!head.isEmpty()) {
                list.add(head);
            }
        }
        return list;
    }

    /**
     *
     * @param text a string of text
     * @return the firstWord word (sequence of consecutive letters) in the text
     */
    private static String firstWord(String text) {
        if (text.length() > 0 && Character.isLetter(text.charAt(0))) {
            return text.split("[^\\p{L}]+")[0];
        } else {
            return "";
        }
    }

    /**
     * Test if a string can be the initial segment of a new sentence or
     * paragraph: punctuation (optional) followed by a mixed case word with only
     * the initial letter is uppercase and ended by optional punctuation/space
     *
     * @param text word a string
     * @return true if the string is a sequence of Unicode letters whose first
     * letter is uppercase and all trailing letters are lowercase (optionally,
     * preceded by punctuation)
     */
    public static boolean isSentenceHead(String text) {
        if (text.length() > 0) {
            return text.matches("(\\p{Punct}|\\p{Space})*\\p{Lu}[\\p{L}&&[^\\p{Lu}]]*((\\p{Punct}|\\p{Space}).*)?");
        } else {
            return false;
        }
    }

    public static String split(File ifile, String last) throws IOException {
        Document doc = SortPageXML.isSorted(ifile) ? DocumentParser.parse(ifile)
                : SortPageXML.sorted(DocumentParser.parse(ifile));

        for (String head : headers(doc)) {
            if (!head.isEmpty()) {
                String start = firstWord(head).replaceAll("ñ", "Ñ"); // no N tilde
                //System.out.println(text);
                if (WordType.typeOf(start) == WordType.UPPERCASE) {
                    // Discard connectors
                    if (start.length() == 1 && start.matches("[AOY]")
                            && last.length() > 0) {
                        if (Character.codePointAt(start, 0)
                                == Character.codePointAt(last, 0) + 1) {
                            System.out.println("<check>" + head + "</check>");
                        } else {
                            System.out.println("<skip>" + head + "</skip>");
                        }

                    } else {
                        int n = collator.compare(last, start);
                        if (n < 0) {
                            System.out.println("<entry>" + head + "</entry>");
                            last = start;
                        } else if (n == 0) {
                            System.out.println("  <subentry>" + head + "</subentry>");
                        } else if (isParticiple(start, last)) {
                            System.out.println("<PastPart>" + head + "</PastPart>");
                        } else {
                            System.out.println("<check>" + head + "</check>");
                            last = start;
                        }
                    }
                } else if (isSentenceHead(head)) {
                    System.out.println("<skip>" + head + "</skip>");
                } else {
                    String s = start.replaceAll("l", "I");
                    if (WordType.typeOf(s) == WordType.UPPERCASE) {
                        // wrong transcription
                        System.out.println("<Itypo>" + head + "</Itypo>");
                        last = s;
                    } else if (WordType.nearlyUpper(start)) {
                        // a single mismatch
                        System.out.println("<check>" + head + "</check>");
                    } else {
                        System.out.println("<check>" + head + "</check>");
                    }
                }
            }
        }
        return last;
    }

    /**
     * Check if an entry can be a past participle of the preceding word
     *
     * @param head the entry
     * @param last the preceding word
     * @return true if head is a past participle entry after the last word
     */
    protected static boolean isParticiple(String head, String last) {
        //System.out.println("="+last.replaceFirst("[AEI]R$", ""));
        return last.replaceFirst("[AEI]R(SE)?$", "")
                .equals(head.replaceFirst("[AI]DO$", ""));
    }

    /**
     * Function for debugging purposes (just print headers)
     *
     * @param file input file
     * @throws java.io.IOException
     */
    public static void viewHeaders(File file) throws IOException {
        Document doc = SortPageXML.isSorted(file) ? DocumentParser.parse(file)
                : SortPageXML.sorted(DocumentParser.parse(file));
        for (String head : headers(doc)) {
            System.out.println(head);
        }
    }

    /**
     * Print entries in a collection of files
     *
     * @param args XML file names 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: Split file1.xml file2.xml ...");
        } else {
            String lastEntry = "";
            for (String arg : args) {
                File file = new File(arg);
                System.out.println("-----------------");
                System.out.println(file);
                System.out.println("-----------------");
                //Split.viewHeaders(file);  
                try  {
                lastEntry = Split.split(file, lastEntry);
                } catch (IOException ex) {
                    System.out.println("Wrong file");
                }
            }
        }
    }
}
