import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
//import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationHighlightingMode;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class InjectSignatureField {
    public static void main(String[] args) throws IOException {
        Path inputPath = Path.of("doc1_LO.pdf");
        File output = new File("template_with_signature.pdf");

        try (InputStream in = Files.newInputStream(inputPath);
             RandomAccessRead rar = new RandomAccessReadBufferedFile(inputPath)) {

             PDFParser parser = new PDFParser(rar);

            try (PDDocument document = parser.parse()) {

                PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
                if (acroForm == null) {
                    acroForm = new PDAcroForm(document);
                    document.getDocumentCatalog().setAcroForm(acroForm);
                }

                acroForm.setNeedAppearances(true);

                PDSignatureField sigField = new PDSignatureField(acroForm);
                sigField.setPartialName("Signature1");

                PDPage page = document.getPage(0);
                PDAnnotationWidget widget = new PDAnnotationWidget();
                PDRectangle rect = new PDRectangle(50, 650, 200, 50); // Adjust position as needed
                widget.setRectangle(rect);
                widget.setPage(page);
//            widget.setHighlightMode(PDAnnotationHighlightingMode.INVERT);

                sigField.setWidgets(List.of(widget));
                page.getAnnotations().add(widget);
                acroForm.getFields().add(sigField);

                document.save(output);
                System.out.println("✅ Signature field added: " + output.getAbsolutePath());
            }
        }
    }
}
