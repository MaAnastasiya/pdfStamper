import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class Main {

    public static void showFiles(File[] files, String destinationFolder, String stampText, float xOffset, float yOffset, float stampWidth, float stampHeight, String imagePath) {
        for (File file : files) {
            if (file.isDirectory()) {
                showFiles(file.listFiles(), destinationFolder, stampText, xOffset, yOffset, stampWidth, stampHeight, imagePath); // повторный вызов метода для директории
            } else {
                pdfStamper(file.getAbsolutePath(), destinationFolder, stampText, xOffset, yOffset, stampWidth, stampHeight, imagePath);
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
        float stampWidth = Float.parseFloat(properties.getProperty("stamp.width"));
        float stampHeight = Float.parseFloat(properties.getProperty("stamp.height"));
        String imagePath = properties.getProperty("stamp.image.path");

        File dir = new File(sourceFolder);
        showFiles(dir.listFiles(), destinationFolder, stampText, xOffset, yOffset, stampWidth, stampHeight, imagePath);
    }

    public static void pdfStamper(String pdfFilePath, String destinationFolder, String stampText, float rightMarginCm, float bottomMarginCm, float stampWidthCm, float stampHeightCm, String stampImagePath) {

        // Конвертация сантиметров в точки (1 см = 28.3465 точек)
        float bottomMarginPoints = bottomMarginCm * 28.3465f;
        float rightMarginPoints = rightMarginCm * 28.3465f;
        float stampWidthPoints = stampWidthCm * 28.3465f;
        float stampHeightPoints = stampHeightCm * 28.3465f;

        File pdfFile = new File(pdfFilePath);
        if (!pdfFile.exists()) {
            System.out.println("File not found: " + pdfFilePath);
            return;
        }

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDImageXObject stampImage = PDImageXObject.createFromFile(stampImagePath, document);

            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                int rotation = page.getRotation();

                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                float x, y;
                if (rotation == 90 || rotation == 270) {
                    // Учитываем ротацию страницы и размещаем штамп с поворотом
                    x = pageWidth - rightMarginPoints - stampWidthPoints;
                    y = bottomMarginPoints;
                    PDPageContentStream contentStream = new PDPageContentStream(document, page,true,true,true);
                    contentStream.saveGraphicsState();
                    // Применяем поворот штампа на 270 градусов
                    if (rotation == 90) {
                        contentStream.transform(new Matrix(0, 1, -1, 0, pageHeight, 0));
                    } else if (rotation == 270) {
                        contentStream.transform(new Matrix(0, -1, 1, 0, 0, pageWidth));
                    }
                    contentStream.drawImage(stampImage, x, y, stampWidthPoints, stampHeightPoints);
                    contentStream.restoreGraphicsState();
                    contentStream.close();
                } else {
                    x = pageWidth - rightMarginPoints - stampWidthPoints;
                    y = bottomMarginPoints;
                    PDPageContentStream contentStream = new PDPageContentStream(document, page,true,true,true);
                    contentStream.drawImage(stampImage, x, y, stampWidthPoints, stampHeightPoints);
                    contentStream.close();
                }
            }

            // Определяем путь для сохранения файла
            String savePath;
            if (destinationFolder.equals(pdfFile.getParent())) {
                savePath = pdfFilePath; // Если destinationFolder совпадает с папкой исходного файла
            } else {
                savePath = destinationFolder +"/"+ pdfFile.getName(); // Иначе используем destinationFolder
            }

            document.save(new File(savePath));
        } catch (IOException e) {
            System.out.println("An error occurred while processing the PDF file: " + e.getMessage());
        }
    }
}