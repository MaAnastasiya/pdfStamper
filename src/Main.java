import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class Main {
    public static void showFiles(File[] files, String destinationFolder, String stampText, float xOffset, float yOffset, String imagePath, float stampWidth) {
        for (File file : files) {
            if (file.isDirectory()) {
                showFiles(file.listFiles(), destinationFolder, stampText, xOffset, yOffset, imagePath, stampWidth); //повторный вызов метода для директории
            } else {
                pdfStamper(file.getAbsolutePath(), destinationFolder, stampText, xOffset, yOffset, imagePath, stampWidth);
            }
        }
    }

    public static void main(String[] args) {
        // Чтение конфигурации с указанием кодировки UTF-8
        Properties properties = new Properties();
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream("config.properties"), "UTF-8")) {
            properties.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String sourceFolder = properties.getProperty("source.folder");
        String destinationFolder = properties.getProperty("destination.folder");
        String stampText = properties.getProperty("stamp.text");
        float xOffset = Float.parseFloat(properties.getProperty("stamp.x.offset"));
        float yOffset = Float.parseFloat(properties.getProperty("stamp.y.offset"));
        String imagePath = properties.getProperty("stamp.image.path");
        float stampWidth = Float.parseFloat(properties.getProperty("stamp.width"));

        File dir = new File(sourceFolder);
        showFiles(dir.listFiles(), destinationFolder, stampText, xOffset, yOffset, imagePath, stampWidth);
    }

    public static void pdfStamper(String src, String destinationFolder, String stampText, float xOffset, float yOffset, String imagePath, float stampWidth) {
        // Получаем текущую дату
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String formattedDate = currentDate.format(formatter);

        String dest = destinationFolder.equals(new File(src).getParent()) ? src : destinationFolder + "/" + new File(src).getName();
        String temp = "temp.pdf";

        try {
            // Открываем существующий PDF документ для чтения
            PdfReader reader = new PdfReader(src);
            PdfWriter writer = new PdfWriter(temp);
            PdfDocument pdfDoc = new PdfDocument(reader, writer);
            Document doc = new Document(pdfDoc);

            // Загружаем шрифт, поддерживающий кириллицу
            PdfFont font = PdfFontFactory.createFont("ArialRegular.ttf", "CP1251");

            // Перебираем все страницы и добавляем штамп
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                PdfPage page = pdfDoc.getPage(i);
                Rectangle pageSize = page.getPageSize();

                // Позиция штампа (справа снизу)
                float x = pageSize.getRight() - stampWidth - xOffset; // отступ от правого края
                float y = pageSize.getBottom() + yOffset; // отступ от нижнего края

                if (imagePath != null && !imagePath.isEmpty()) {
                    // Если указан путь к изображению, добавляем изображение
                    ImageData imageData = ImageDataFactory.create(imagePath);
                    Image imageStamp = new Image(imageData)
                            .setFixedPosition(i, x, y)
                            .setWidth(stampWidth);
                    doc.add(imageStamp);
                } else {
                    // Создаем штамп с двумя строками текста
                    Paragraph stamp = new Paragraph(stampText + "\n" + formattedDate)
                            .setFontSize(12)
                            .setFontColor(ColorConstants.BLACK)
                            .setFont(font)
                            .setBorder(new SolidBorder(ColorConstants.BLACK, 1))
                            .setPadding(5)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setFixedPosition(i, x, y, stampWidth); // фиксированное положение и ширина
                    doc.add(stamp);
                }
            }

            // Закрываем документ
            doc.close();
            pdfDoc.close();

            // Заменяем исходный файл временным файлом или сохраняем в новую папку
            if (dest.equals(src)) {
                Files.move(Paths.get(temp), Paths.get(src), StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(Paths.get(temp), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}