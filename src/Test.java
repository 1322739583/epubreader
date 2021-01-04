import com.sun.java.accessibility.util.Translator;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        EpubReader reader = new EpubReader();
        try {
            Book book = reader.readEpub(new FileInputStream("1.epub"));
            System.out.println(book.getContents());
            System.out.println(book.getTitle());
            TableOfContents tableOfContents = book.getTableOfContents();
            List<Resource> allUniqueResources = tableOfContents.getAllUniqueResources();
            List<TOCReference> tocReferences = tableOfContents.getTocReferences();
            System.out.println(tableOfContents.calculateDepth());
            //tocReferences.
//            System.out.println(tocReferences.get(0).getTitle());
//            System.out.println(tocReferences.get(1).getTitle());
//            System.out.println(tocReferences.get(2).getTitle());
//            System.out.println(tocReferences.get(3).getTitle());
//            System.out.println(tocReferences.get(4).getTitle());
//            for (TOCReference tocReference : tocReferences) {
//                // System.out.println(tocReference.getTitle());
//                List<TOCReference> children = tocReference.getChildren();
//                if (children != null) {
//                    System.out.println(tocReference.getTitle());
//                    for (TOCReference child : children) {
//                        System.out.println("    " + child.getTitle());
//                        // System.out.println(child.getCompleteHref());
//                        List<TOCReference> subChild = child.getChildren();
//                        for (TOCReference reference : subChild) {
//                            System.out.println("        " + reference.getTitle());
//                        }
//                    }
//                } else {
//                    System.out.println(tocReference.getTitle());
//                }
                // System.out.println(tocReference.getChildren());
//            }
//
//            System.out.println(allUniqueResources.get(1).getTitle());
            solveTitles(tocReferences,tableOfContents.calculateDepth());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static  void solveTitles(List<TOCReference> tocReferences, int depth) {
        //int oldDepth=depth;
        int count=1;
        if (depth > 0) {
            for (TOCReference tocReference : tocReferences) {

                List<TOCReference> children = tocReference.getChildren();
                if (children != null) {
                    System.out.println(tocReference.getTitle());

                    solveTitles(children, depth - 1);
                }else {
                  //  System.out.println(tocReference.getTitle());
                }
            }
        }

    }

    private static InputStream getResource(String path) {
        return Translator.class.getResourceAsStream(path);
    }

    private static Resource getResource(String path, String href) {
        Resource resource = null;
        try {
            resource = new Resource(getResource(path), href);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resource;
    }
}
