import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void showFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                //System.out.println("Directory: " + file.getAbsolutePath());
                showFiles(file.listFiles()); //повторный вызов метода для директории
            } else {
                //System.out.println("File: " + file.getAbsolutePath());
                pdfStamper(file.getAbsolutePath());
            }
        }
    }
    public static void main(String[] args) {
        File dir = new File("C:\\Users\\Настя\\Desktop\\sc");
        showFiles(dir.listFiles());
    }

    public static void pdfStamper(String src) {// путь к входному PDF файлу
        // Получаем текущую дату
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = currentDate.format(formatter);

        try {
            // Загружаем шрифт, поддерживающий кириллицу
            PdfFont font = PdfFontFactory.createFont("ArialRegular.ttf", "CP1251");

            // Открываем существующий PDF документ для чтения
            PdfReader reader = new PdfReader(src);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(reader, writer);
            Document doc = new Document(pdfDoc);

            // Перебираем все страницы и добавляем штамп
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                Rectangle pageSize = page.getPageSize();

                // Позиция штампа (справа снизу)
                float stampWidth = 140; // фиксированная ширина штампа
                float x = pageSize.getRight() - stampWidth - 30; // отступ от правого края
                float y = pageSize.getBottom() + 20;

                // Создаем штамп с двумя строками текста
                Paragraph stamp = new Paragraph("Выдано в производство работ\n" + formattedDate)
                        .setFontSize(12)
                        .setFontColor(ColorConstants.BLACK)
                        .setFont(font)
                        .setBorder(new SolidBorder(ColorConstants.BLACK, 1))
                        .setPadding(5)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFixedPosition(i, x, y, stampWidth); // фиксированное положение и ширина

                // Добавляем штамп на страницу
                doc.add(stamp);
            }

            // Закрываем документ
            doc.close();
            pdfDoc.close();

            // Записываем измененный PDF обратно в файл
            try (FileOutputStream fos = new FileOutputStream(src)) {
                fos.write(baos.toByteArray());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}