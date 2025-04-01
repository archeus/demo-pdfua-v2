import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.*;
//import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.PDStructureElement;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
//import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlightingMode;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.*;

import java.io.IOException;
import java.util.*;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;

public class PdfUaWithSignature3  {
    public static void main(String[] args) throws IOException {
        try (PDDocument document = new PDDocument()) {

            PDDocumentCatalog catalog = document.getDocumentCatalog();
            catalog.setLanguage("en-US");

            // Structure tree root
            PDStructureTreeRoot structureTreeRoot = new PDStructureTreeRoot();
            catalog.setStructureTreeRoot(structureTreeRoot);

            // Create a page
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            // Root structure element: Document
            PDStructureElement docElem = new PDStructureElement(StandardStructureTypes.DOCUMENT, structureTreeRoot);
            structureTreeRoot.appendKid(docElem);

            // Paragraph element
            int mcid = 0;
            PDStructureElement paraElem = new PDStructureElement(StandardStructureTypes.P, docElem);
            docElem.appendKid(paraElem);

            COSArray kids = new COSArray();
            COSDictionary mcidDict = new COSDictionary();
            mcidDict.setInt(COSName.MCID, mcid);
            kids.add(mcidDict);
            paraElem.getCOSObject().setItem(COSName.K, kids);

            // Write content with BDC/EMC and MCID
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                String bdc = String.format("/P << /MCID %d >> BDC\n", mcid);
                content.appendRawCommands(bdc);

                content.beginText();
                content.setFont(new PDType1Font(HELVETICA), 12);
                content.newLineAtOffset(50, 750);
                content.showText("Please sign below:");
                content.endText();

                content.appendRawCommands("EMC\n");
            }

            // AcroForm and signature field
            PDAcroForm acroForm = new PDAcroForm(document);
            catalog.setAcroForm(acroForm);
            acroForm.setNeedAppearances(true);

            PDSignatureField sigField = new PDSignatureField(acroForm);
            sigField.setPartialName("Signature1");

            PDAnnotationWidget widget = new PDAnnotationWidget();
            PDRectangle sigRect = new PDRectangle(50, 650, 200, 50);
            widget.setRectangle(sigRect);
            widget.setPage(page);
//            widget.setHighlightMode(PDAnnotationHighlightingMode.INVERT);

            sigField.setWidgets(List.of(widget));
            page.getAnnotations().add(widget);
            acroForm.getFields().add(sigField);

            // Structure element for the widget
            int structParent = 1;
            PDStructureElement formElem = new PDStructureElement(StandardStructureTypes.FORM, docElem);
            docElem.appendKid(formElem);

            widget.getCOSObject().setInt(COSName.STRUCT_PARENT, structParent);

            // Create ParentTree dictionary manually
            COSArray numsArray = new COSArray();
            numsArray.add(COSInteger.get(structParent));
            numsArray.add(formElem.getCOSObject());

            COSDictionary parentTreeDict = new COSDictionary();
            parentTreeDict.setItem(COSName.NUMS, numsArray);
            parentTreeDict.setInt(COSName.PARENT_TREE_NEXT_KEY, structParent + 1);

            // Make ParentTree indirect — required for PAC
            COSObject parentTreeObj = new COSObject(parentTreeDict);
//            document.getDocument().addObject(parentTreeObj);

            structureTreeRoot.getCOSObject().setItem(COSName.PARENT_TREE, parentTreeObj);

            // Save the document
            document.save("pdfua_signature_field_fixed.pdf");
            System.out.println("✅ Created: pdfua_signature_field_fixed.pdf");
        }
    }
}
