import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ImageWatermarkProcessorGUI extends JFrame {
    private JPanel mainPanel;
    private JPanel imagePanel;
    private JScrollPane scrollPane;
    private JButton importButton;
    private JButton importFolderButton;
    private JButton exportButton;
    private JButton processButton;
    private JComboBox<String> fontSizeCombo;
    private JComboBox<String> colorCombo;
    private JComboBox<String> positionCombo;
    private JComboBox<String> outputFormatCombo;
    private JTextField prefixField;
    private JTextField suffixField;
    private JRadioButton keepOriginalRadio;
    private JRadioButton addPrefixRadio;
    private JRadioButton addSuffixRadio;
    private ButtonGroup namingGroup;
    private JTextField exportPathField;
    private JButton browseExportButton;
    private JLabel statusLabel;

    // 新增的JPEG质量控制组件
    private JSlider jpegQualitySlider;
    private JLabel jpegQualityLabel;
    private JPanel jpegQualityPanel;

    // 新增的图片尺寸调整组件
    private JCheckBox resizeCheckBox;
    private JComboBox<String> resizeTypeCombo;
    private JTextField resizeValueField;
    private JLabel resizeUnitLabel;
    private JPanel resizePanel;

    private List<ImageInfo> imageList;
    private DefaultListModel<ImageInfo> listModel;
    private JList<ImageInfo> imageJList;

    public ImageWatermarkProcessorGUI() {
        imageList = new ArrayList<>();
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        setupDragAndDrop();
        updateUIBasedOnFormat(); // 初始化时根据默认格式更新UI
    }

    private void initializeComponents() {
        setTitle("图片水印处理器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        mainPanel = new JPanel(new BorderLayout());

        // 初始化控件
        importButton = new JButton("导入图片");
        importFolderButton = new JButton("导入文件夹");
        exportButton = new JButton("选择导出目录");
        processButton = new JButton("添加水印并导出");

        fontSizeCombo = new JComboBox<>(new String[]{"16", "20", "24", "30", "36", "48"});
        fontSizeCombo.setSelectedItem("24");

        colorCombo = new JComboBox<>(new String[]{"white", "black", "red", "blue", "green", "yellow", "cyan", "magenta"});
        colorCombo.setSelectedItem("white");

        positionCombo = new JComboBox<>(new String[]{"top-left", "top-right", "center", "bottom-left", "bottom-right"});
        positionCombo.setSelectedItem("bottom-right");

        outputFormatCombo = new JComboBox<>(new String[]{"PNG", "JPEG"});

        prefixField = new JTextField("wm_", 10);
        suffixField = new JTextField("_watermarked", 10);

        keepOriginalRadio = new JRadioButton("保留原文件名", true);
        addPrefixRadio = new JRadioButton("添加前缀");
        addSuffixRadio = new JRadioButton("添加后缀");
        namingGroup = new ButtonGroup();
        namingGroup.add(keepOriginalRadio);
        namingGroup.add(addPrefixRadio);
        namingGroup.add(addSuffixRadio);

        exportPathField = new JTextField(20);
        browseExportButton = new JButton("浏览");

        statusLabel = new JLabel("就绪");

        // 初始化JPEG质量控制组件
        jpegQualityLabel = new JLabel("JPEG质量: 75");
        jpegQualitySlider = new JSlider(0, 100, 75);
        jpegQualitySlider.setMajorTickSpacing(25);
        jpegQualitySlider.setMinorTickSpacing(5);
        jpegQualitySlider.setPaintTicks(true);
        jpegQualitySlider.setPaintLabels(true);
        jpegQualityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 初始化图片尺寸调整组件
        resizeCheckBox = new JCheckBox("调整图片尺寸");
        resizeTypeCombo = new JComboBox<>(new String[]{"按宽度", "按高度", "按百分比"});
        resizeValueField = new JTextField(8);
        resizeUnitLabel = new JLabel("像素");
        resizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 初始化图片列表
        listModel = new DefaultListModel<>();
        imageJList = new JList<>(listModel);
        imageJList.setCellRenderer(new ImageListCellRenderer());
        imageJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        scrollPane = new JScrollPane(imageJList);
    }

    private void setupLayout() {
        // 顶部控制面板
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 导入按钮
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(importButton, gbc);
        gbc.gridx = 1;
        controlPanel.add(importFolderButton, gbc);

        // 字体大小
        gbc.gridx = 0; gbc.gridy = 1;
        controlPanel.add(new JLabel("字体大小:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(fontSizeCombo, gbc);

        // 颜色
        gbc.gridx = 0; gbc.gridy = 2;
        controlPanel.add(new JLabel("颜色:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(colorCombo, gbc);

        // 位置
        gbc.gridx = 0; gbc.gridy = 3;
        controlPanel.add(new JLabel("位置:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(positionCombo, gbc);

        // 输出格式
        gbc.gridx = 0; gbc.gridy = 4;
        controlPanel.add(new JLabel("输出格式:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(outputFormatCombo, gbc);

        // JPEG质量控制
        gbc.gridx = 0; gbc.gridy = 5;
        controlPanel.add(jpegQualityLabel, gbc);
        jpegQualityPanel.add(jpegQualitySlider);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(jpegQualityPanel, gbc);

        // 图片尺寸调整
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 6;
        controlPanel.add(resizeCheckBox, gbc);
        resizePanel.add(resizeTypeCombo);
        resizePanel.add(resizeValueField);
        resizePanel.add(resizeUnitLabel);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(resizePanel, gbc);

        // 命名规则
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 7;
        controlPanel.add(new JLabel("命名规则:"), gbc);
        JPanel namingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namingPanel.add(keepOriginalRadio);
        namingPanel.add(addPrefixRadio);
        namingPanel.add(prefixField);
        namingPanel.add(addSuffixRadio);
        namingPanel.add(suffixField);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(namingPanel, gbc);

        // 导出路径
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 8;
        controlPanel.add(new JLabel("导出目录:"), gbc);
        JPanel exportPanel = new JPanel(new BorderLayout());
        exportPanel.add(exportPathField, BorderLayout.CENTER);
        exportPanel.add(browseExportButton, BorderLayout.EAST);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(exportPanel, gbc);

        // 导出按钮
        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(exportButton);
        buttonPanel.add(processButton);
        controlPanel.add(buttonPanel, gbc);

        // 状态栏
        gbc.gridy = 10;
        controlPanel.add(statusLabel, gbc);

        // 主面板布局
        mainPanel.add(controlPanel, BorderLayout.WEST);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private void setupEventHandlers() {
        importButton.addActionListener(e -> importImages());
        importFolderButton.addActionListener(e -> importFolder());
        exportButton.addActionListener(e -> selectExportFolder());
        browseExportButton.addActionListener(e -> selectExportFolder());
        processButton.addActionListener(e -> processImages());

        // 添加输出格式变化监听器
        outputFormatCombo.addActionListener(e -> updateUIBasedOnFormat());

        // 添加JPEG质量滑块监听器
        jpegQualitySlider.addChangeListener(e -> {
            jpegQualityLabel.setText("JPEG质量: " + jpegQualitySlider.getValue());
        });

        // 添加尺寸调整复选框监听器
        resizeCheckBox.addActionListener(e -> {
            boolean enabled = resizeCheckBox.isSelected();
            resizeTypeCombo.setEnabled(enabled);
            resizeValueField.setEnabled(enabled);
            resizeUnitLabel.setEnabled(enabled);
        });

        // 添加尺寸类型变化监听器
        resizeTypeCombo.addActionListener(e -> {
            String selected = (String) resizeTypeCombo.getSelectedItem();
            if ("按百分比".equals(selected)) {
                resizeUnitLabel.setText("%");
            } else {
                resizeUnitLabel.setText("像素");
            }
        });

        // 初始化组件状态
        resizeTypeCombo.setEnabled(false);
        resizeValueField.setEnabled(false);
        resizeUnitLabel.setEnabled(false);
    }

    private void setupDragAndDrop() {
        new DropTarget(this, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {}

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {}

            @Override
            public void dragExit(DropTargetEvent dte) {}

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        importDroppedFiles(files);
                    }
                    dtde.dropComplete(true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    dtde.dropComplete(false);
                }
            }
        });
    }

    private void updateUIBasedOnFormat() {
        String format = (String) outputFormatCombo.getSelectedItem();
        boolean isJPEG = "JPEG".equals(format);
        jpegQualitySlider.setVisible(isJPEG);
        jpegQualityLabel.setVisible(isJPEG);

        // 重新布局以适应可见性变化
        jpegQualityPanel.revalidate();
        jpegQualityPanel.repaint();
    }

    private void importImages() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "图片文件 (JPEG, PNG, BMP, TIFF)", "jpg", "jpeg", "png", "bmp", "tiff", "tif"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                addImageToList(file);
            }
            statusLabel.setText("已导入 " + selectedFiles.length + " 个文件");
        }
    }

    private void importFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (folderChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = folderChooser.getSelectedFile();
            importImagesFromFolder(folder);
        }
    }

    private void importImagesFromFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            int count = 0;
            for (File file : files) {
                if (isImageFile(file)) {
                    addImageToList(file);
                    count++;
                }
            }
            statusLabel.setText("从文件夹导入 " + count + " 个图片文件");
        }
    }

    private void importDroppedFiles(List<File> files) {
        int count = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                importImagesFromFolder(file);
            } else if (isImageFile(file)) {
                addImageToList(file);
                count++;
            }
        }
        statusLabel.setText("拖拽导入 " + count + " 个文件");
    }

    private boolean isImageFile(File file) {
        if (!file.isFile()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".bmp") ||
                name.endsWith(".tiff") || name.endsWith(".tif");
    }

    private void addImageToList(File file) {
        ImageInfo imageInfo = new ImageInfo(file);
        if (!imageList.contains(imageInfo)) {
            imageList.add(imageInfo);
            listModel.addElement(imageInfo);
        }
    }

    private void selectExportFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (folderChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = folderChooser.getSelectedFile();
            exportPathField.setText(folder.getAbsolutePath());
        }
    }

    private void processImages() {
        if (imageList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先导入图片文件", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String exportPath = exportPathField.getText().trim();
        if (exportPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择导出目录", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 验证尺寸调整输入
        if (resizeCheckBox.isSelected()) {
            try {
                int value = Integer.parseInt(resizeValueField.getText().trim());
                String type = (String) resizeTypeCombo.getSelectedItem();
                if ("按百分比".equals(type) && (value <= 0 || value > 1000)) {
                    JOptionPane.showMessageDialog(this, "百分比必须在1-1000之间", "输入错误", JOptionPane.ERROR_MESSAGE);
                    return;
                } else if (!"按百分比".equals(type) && value <= 0) {
                    JOptionPane.showMessageDialog(this, "尺寸值必须大于0", "输入错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "请输入有效的数字", "输入错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        File exportDir = new File(exportPath);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        // 检查是否导出到原目录
        for (ImageInfo imageInfo : imageList) {
            File parentDir = imageInfo.getFile().getParentFile();
            try {
                if (Files.isSameFile(parentDir.toPath(), exportDir.toPath())) {
                    int result = JOptionPane.showConfirmDialog(this,
                            "导出目录与原文件目录相同，可能会覆盖原文件。是否继续？",
                            "警告", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (result != JOptionPane.YES_OPTION) {
                        return;
                    }
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        processButton.setEnabled(false);
        statusLabel.setText("正在处理图片...");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                int fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
                String color = (String) colorCombo.getSelectedItem();
                String position = (String) positionCombo.getSelectedItem();
                String format = (String) outputFormatCombo.getSelectedItem();
                float jpegQuality = jpegQualitySlider.getValue() / 100.0f;
                boolean resizeEnabled = resizeCheckBox.isSelected();
                String resizeType = resizeEnabled ? (String) resizeTypeCombo.getSelectedItem() : null;
                int resizeValue = resizeEnabled ? Integer.parseInt(resizeValueField.getText().trim()) : 0;

                for (int i = 0; i < imageList.size(); i++) {
                    ImageInfo imageInfo = imageList.get(i);
                    try {
                        processSingleImage(imageInfo, exportDir, fontSize, color, position, format,
                                jpegQuality, resizeEnabled, resizeType, resizeValue);
                        publish("已处理: " + imageInfo.getFile().getName() + " (" + (i + 1) + "/" + imageList.size() + ")");
                    } catch (Exception e) {
                        publish("错误: " + imageInfo.getFile().getName() + " - " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    statusLabel.setText(message);
                }
            }

            @Override
            protected void done() {
                processButton.setEnabled(true);
                statusLabel.setText("处理完成");
                JOptionPane.showMessageDialog(ImageWatermarkProcessorGUI.this,
                        "图片处理完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        worker.execute();
    }

    private void processSingleImage(ImageInfo imageInfo, File exportDir,
                                    int fontSize, String color, String position, String format,
                                    float jpegQuality, boolean resizeEnabled, String resizeType, int resizeValue) throws IOException {
        File imageFile = imageInfo.getFile();
        BufferedImage originalImage = ImageIO.read(imageFile);

        if (originalImage == null) {
            throw new IOException("无法读取图片文件");
        }

        // 调整图片尺寸（如果启用）
        BufferedImage processedImage = originalImage;
        if (resizeEnabled) {
            processedImage = resizeImage(originalImage, resizeType, resizeValue);
        }

        // 获取拍摄时间
        String captureTime = getCaptureTime(imageFile);
        if (captureTime == null) {
            captureTime = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        }

        // 添加水印
        BufferedImage watermarkedImage = addWatermark(processedImage, captureTime, fontSize, color, position);

        // 生成输出文件名
        String outputFileName = generateOutputFileName(imageFile.getName(), format);
        File outputFile = new File(exportDir, outputFileName);

        // 保存图片
        if ("JPEG".equals(format)) {
            saveAsJPEG(watermarkedImage, outputFile, jpegQuality);
        } else {
            ImageIO.write(watermarkedImage, format.toLowerCase(), outputFile);
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, String resizeType, int resizeValue) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        int newWidth = originalWidth;
        int newHeight = originalHeight;

        if ("按宽度".equals(resizeType)) {
            newWidth = resizeValue;
            newHeight = (int) ((double) originalHeight * resizeValue / originalWidth);
        } else if ("按高度".equals(resizeType)) {
            newHeight = resizeValue;
            newWidth = (int) ((double) originalWidth * resizeValue / originalHeight);
        } else if ("按百分比".equals(resizeType)) {
            double scale = resizeValue / 100.0;
            newWidth = (int) (originalWidth * scale);
            newHeight = (int) (originalHeight * scale);
        }

        // 确保尺寸至少为1
        newWidth = Math.max(1, newWidth);
        newHeight = Math.max(1, newHeight);

        Image scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(scaledImage, 0, 0, null);
        g2d.dispose();

        return resizedImage;
    }

    private void saveAsJPEG(BufferedImage image, File file, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("无法找到JPEG图像写入器");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();

        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        ios.close();
        writer.dispose();
    }

    private String generateOutputFileName(String originalName, String format) {
        String nameWithoutExtension = originalName;
        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            nameWithoutExtension = originalName.substring(0, lastDotIndex);
        }

        String extension = format.toLowerCase();

        if (keepOriginalRadio.isSelected()) {
            return nameWithoutExtension + "." + extension;
        } else if (addPrefixRadio.isSelected()) {
            return prefixField.getText() + nameWithoutExtension + "." + extension;
        } else if (addSuffixRadio.isSelected()) {
            return nameWithoutExtension + suffixField.getText() + "." + extension;
        }

        return nameWithoutExtension + "." + extension;
    }

    private String getCaptureTime(File imageFile) {
        try {
            ImageInputStream iis = ImageIO.createImageInputStream(imageFile);
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                reader.setInput(iis);

                IIOMetadata metadata = reader.getImageMetadata(0);
                if (metadata != null) {
                    // 简化处理，实际应用中需要使用专门的库如metadata-extractor来解析EXIF
                    return null;
                }
            }
            iis.close();
        } catch (Exception e) {
            System.out.println("无法提取EXIF数据: " + e.getMessage());
        }
        return null;
    }

    private BufferedImage addWatermark(BufferedImage originalImage, String text,
                                       int fontSize, String color, String position) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 创建新的图片用于绘制水印
        BufferedImage watermarkedImage = new BufferedImage(width, height,
                originalImage.getType() != BufferedImage.TYPE_INT_ARGB ?
                        BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
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
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

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

    private Color parseColor(String color) {
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

    // 图片信息类
    private static class ImageInfo {
        private File file;
        private ImageIcon thumbnail;

        public ImageInfo(File file) {
            this.file = file;
            createThumbnail();
        }

        private void createThumbnail() {
            try {
                ImageIcon icon = new ImageIcon(file.getAbsolutePath());
                Image img = icon.getImage();
                Image scaledImg = img.getScaledInstance(80, 60, Image.SCALE_SMOOTH);
                this.thumbnail = new ImageIcon(scaledImg);
            } catch (Exception e) {
                // 如果无法创建缩略图，使用默认图标
                this.thumbnail = new ImageIcon(); // 空图标
            }
        }

        public File getFile() {
            return file;
        }

        public ImageIcon getThumbnail() {
            return thumbnail;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ImageInfo imageInfo = (ImageInfo) obj;
            return Objects.equals(file, imageInfo.file);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file);
        }

        @Override
        public String toString() {
            return file.getName();
        }
    }

    // 自定义列表单元格渲染器
    private static class ImageListCellRenderer extends JPanel implements ListCellRenderer<ImageInfo> {
        private JLabel thumbnailLabel;
        private JLabel nameLabel;

        public ImageListCellRenderer() {
            setLayout(new BorderLayout(5, 5));
            thumbnailLabel = new JLabel();
            nameLabel = new JLabel();
            add(thumbnailLabel, BorderLayout.WEST);
            add(nameLabel, BorderLayout.CENTER);
            setPreferredSize(new Dimension(200, 70));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends ImageInfo> list,
                                                      ImageInfo value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            if (value != null) {
                thumbnailLabel.setIcon(value.getThumbnail());
                nameLabel.setText(value.getFile().getName());
            }

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ImageWatermarkProcessorGUI().setVisible(true);
        });
    }
}
