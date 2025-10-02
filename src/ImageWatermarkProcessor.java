import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class ImageWatermarkProcessor {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ImageWatermarkProcessor <image_path> [font_size] [color] [position]");
            System.out.println("Example: java ImageWatermarkProcessor /path/to/image.jpg 24 white bottom-right");
            return;
        }

        String imagePath = args[0];
        int fontSize = args.length > 1 ? Integer.parseInt(args[1]) : 20;
        String color = args.length > 2 ? args[2] : "white";
        String position = args.length > 3 ? args[3] : "bottom-right";

        try {
            processImage(imagePath, fontSize, color, position);
        } catch (Exception e) {
            System.err.println("Error processing image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void processImage(String imagePath, int fontSize, String color, String position) throws IOException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("Image file does not exist: " + imagePath);
        }

        // 读取图片
        BufferedImage originalImage = ImageIO.read(imageFile);
        if (originalImage == null) {
            throw new IOException("Unable to read image file: " + imagePath);
        }

        // 获取拍摄时间
        String captureTime = getCaptureTime(imageFile);
        if (captureTime == null) {
            // 如果无法获取EXIF信息，使用当前时间
            captureTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            System.out.println("Warning: Could not extract capture time from EXIF, using current date: " + captureTime);
        }

        // 创建水印目录
        Path originalPath = Paths.get(imagePath);
        Path parentDir = originalPath.getParent();
        Path watermarkDir = parentDir.resolve(originalPath.getFileName().toString() + "_watermark");
        Files.createDirectories(watermarkDir);

        // 添加水印
        BufferedImage watermarkedImage = addWatermark(originalImage, captureTime, fontSize, color, position);

        // 保存图片
        String fileName = originalPath.getFileName().toString();
        String extension = getFileExtension(fileName);
        if (extension.isEmpty()) {
            extension = "jpg";
        }
        
        Path outputPath = watermarkDir.resolve(fileName);
        ImageIO.write(watermarkedImage, extension, outputPath.toFile());
        
        System.out.println("Watermarked image saved to: " + outputPath.toString());
    }

    private static String getCaptureTime(File imageFile) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(imageFile);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);
                
                IIOMetadata metadata = reader.getImageMetadata(0);
                if (metadata != null) {
                    // 尝试获取EXIF信息
                    String[] names = metadata.getMetadataFormatNames();
                    for (String name : names) {
                        if (name.equals("javax_imageio_jpeg_image_1.0")) {
                            // 解析JPEG的EXIF数据
                            org.w3c.dom.Node root = metadata.getAsTree(name);
                            return parseExifDateTime(root);
                        }
                    }
                }
            }
            iis.close();
        } catch (Exception e) {
            System.out.println("Could not extract EXIF data: " + e.getMessage());
        }
        return null;
    }

    private static String parseExifDateTime(org.w3c.dom.Node node) {
        if (node == null) return null;
        
        if (node.getNodeName().equals("app2ICC")) {
            // 这里简化处理，实际应用中需要更复杂的EXIF解析
            return null;
        }
        
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node child = children.item(i);
            if (child.getNodeName().equals("markerSequence")) {
                org.w3c.dom.NodeList markers = child.getChildNodes();
                for (int j = 0; j < markers.getLength(); j++) {
                    org.w3c.dom.Node marker = markers.item(j);
                    if (marker.getNodeName().equals("unknown")) {
                        // 这里简化处理EXIF数据
                        // 实际应用中需要使用专门的库如metadata-extractor来解析EXIF
                        return null;
                    }
                }
            }
            String result = parseExifDateTime(child);
            if (result != null) return result;
        }
        
        return null;
    }

    private static BufferedImage addWatermark(BufferedImage originalImage, String text, 
                                              int fontSize, String color, String position) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        
        // 创建新的图片用于绘制水印
        BufferedImage watermarkedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = watermarkedImage.createGraphics();
        
        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);
        
        // 设置字体
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g2d.setFont(font);
        
        // 设置颜色
        Color textColor = parseColor(color);
        g2d.setColor(textColor);
        
        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 计算文本尺寸
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = fontMetrics.getHeight();
        
        // 根据位置设置水印坐标
        int x = 0, y = 0;
        switch (position.toLowerCase()) {
            case "top-left":
                x = 10;
                y = textHeight;
                break;
            case "top-right":
                x = width - textWidth - 10;
                y = textHeight;
                break;
            case "center":
                x = (width - textWidth) / 2;
                y = height / 2;
                break;
            case "bottom-left":
                x = 10;
                y = height - 10;
                break;
            case "bottom-right":
            default:
                x = width - textWidth - 10;
                y = height - 10;
                break;
        }
        
        // 绘制水印
        g2d.drawString(text, x, y);
        
        g2d.dispose();
        return watermarkedImage;
    }
    
    private static Color parseColor(String color) {
        switch (color.toLowerCase()) {
            case "black": return Color.BLACK;
            case "blue": return Color.BLUE;
            case "red": return Color.RED;
            case "green": return Color.GREEN;
            case "yellow": return Color.YELLOW;
            case "cyan": return Color.CYAN;
            case "magenta": return Color.MAGENTA;
            case "white": 
            default: return Color.WHITE;
        }
    }
    
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
