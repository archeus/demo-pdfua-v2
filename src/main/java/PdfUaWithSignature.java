import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDNumberTreeNode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureTreeRoot;
import org.apache.pdfbox.pdmodel.documentinterchange.taggedpdf.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.cos.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA;

public class PdfUaWithSignature {
    public static void main(String[] args) throws IOException {
        try (PDDocument document = new PDDocument()) {

            // 1. Create tagged document and page
            document.getDocumentCatalog().setLanguage("en-US");
            PDStructureTreeRoot structureTreeRoot = new PDStructureTreeRoot();
            document.getDocumentCatalog().setStructureTreeRoot(structureTreeRoot);

            PDPage page = new PDPage();
            document.addPage(page);

            // 2. Root structure element (Document)
            PDStructureElement docElem = new PDStructureElement(StandardStructureTypes.DOCUMENT, structureTreeRoot);
            structureTreeRoot.appendKid(docElem);

            // 3. Add a paragraph structure element
            PDStructureElement paraElem = new PDStructureElement(StandardStructureTypes.P, docElem);
            docElem.appendKid(paraElem);

            // 4. Marked content
//            PDPageContentStream content = new PDPageContentStream(document, page);
//            content.beginMarkedContent(COSName.P, paraElem);

//            PDStructureElement paraElem = new PDStructureElement(StandardStructureTypes.P, docElem);
//            docElem.appendKid(paraElem);

// Create marked content with MCID (Marked Content ID)
            int mcid = 0;

// Link structure element to MCID via a dictionary


            COSDictionary paraDict = paraElem.getCOSObject();
            COSArray kArray = new COSArray();
            COSDictionary mcidDict = new COSDictionary();
            mcidDict.setInt(COSName.MCID, mcid);
            kArray.add(mcidDict);
            paraDict.setItem(COSName.K, kArray);

            COSDictionary props = new COSDictionary();
            props.setInt(COSName.MCID, mcid);

// Now write the content using BDC/EMC for the MCID
            PDPageContentStream content = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);




            content.beginText();
            content.setFont(new PDType1Font(HELVETICA), 12);
            content.newLineAtOffset(50, 750);
            content.showText("Please sign below:");
            content.endText();

            content.endMarkedContent();
            content.close();

            // 5. Create signature field
            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
            acroForm.setNeedAppearances(true);

            PDSignatureField signatureField = new PDSignatureField(acroForm);
            signatureField.setPartialName("Sig1");

            // Widget annotation for the signature field
            PDAnnotationWidget widget = new PDAnnotationWidget();
            PDRectangle rect = new PDRectangle(50, 650, 200, 50);
            widget.setRectangle(rect);
            widget.setPage(page);
//            widget.setHighlightMode(PDAnnotationHighlightingMode.INVERT);

            // Add widget to page
            page.getAnnotations().add(widget);
            signatureField.setWidgets(java.util.Collections.singletonList(widget));
            acroForm.getFields().add(signatureField);

            // 6. Structure element for the form field
            PDStructureElement formElem = new PDStructureElement(StandardStructureTypes.FORM, docElem);
            docElem.appendKid(formElem);

            // Link the widget annotation to the structure element (via StructParent)
            int structParent = 1;
            widget.getCOSObject().setInt(COSName.STRUCT_PARENT, structParent);
            COSDictionary parentTreeDict = new COSDictionary();
            COSArray numsArray = new COSArray();
            numsArray.add(COSInteger.get(structParent));
            numsArray.add(formElem.getCOSObject());

            parentTreeDict.setInt(COSName.NUMS, 1);
            parentTreeDict.setItem(COSName.NUMS, numsArray);

            COSDictionary parentTree = new COSDictionary();
            parentTree.setItem(COSName.NUMS, numsArray);
//            structureTreeRoot.setParentTree(new PDNumberTreeNode(parentTree));


//            int structParent = 1;
            widget.getCOSObject().setInt(COSName.STRUCT_PARENT, structParent);

// Create the ParentTree as a number tree mapping StructParent → StructureElement
            PDNumberTreeNode parentTreeNode = new PDNumberTreeNode(PDStructureElement.class);
            Map<Integer, PDStructureElement> nums = new HashMap<>();
            nums.put(structParent, formElem);
            parentTreeNode.setNumbers(nums);

// Set the ParentTree on the structure tree root
            structureTreeRoot.setParentTree(parentTreeNode);
            // Save
            document.save("~//Downloads/pdfua_signature_pdfbox1.pdf");

            document.save("pdfua_signature_pdfbox.pdf");


        }
    }
}
