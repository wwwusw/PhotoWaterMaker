import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        loadTemplates(); // 加载已保存的模板
        loadLastConfiguration(); // 加载上次配置
        saveConfigurationOnExit();

        // 增加窗体宽度，从900调整为1200
        setSize(1200, 700);
    }
    private JTextField watermarkTextField;
    private JComboBox<String> fontNameCombo;
    private JCheckBox boldCheckBox;
    private JCheckBox italicCheckBox;
    private JButton colorPickerButton;
    private JSlider transparencySlider;
    private JLabel transparencyLabel;
    private JCheckBox shadowCheckBox;
    private JCheckBox outlineCheckBox;
    private JPanel textWatermarkPanel;
    private JPanel imageWatermarkPanel;
    private JRadioButton textWatermarkRadio;
    private JRadioButton imageWatermarkRadio;
    private ButtonGroup watermarkTypeGroup;
    private JTextField imagePathField;
    private JButton browseImageButton;
    private JSlider imageTransparencySlider;
    private JLabel imageTransparencyLabel;
    private JSlider imageScaleSlider;
    private JLabel imageScaleLabel;
    // 在 ImageWatermarkProcessorGUI 类中添加以下新成员变量
    private JLabel previewLabel;
    private BufferedImage currentPreviewImage;
    private Point watermarkPosition = new Point(0, 0);
    private int watermarkRotation = 0;
    private boolean isDraggingWatermark = false;
    private Point dragStartPoint = new Point(0, 0);
    private Point watermarkStartPoint = new Point(0, 0);
    private JSlider rotationSlider;
    private JLabel rotationLabel;
    private JButton[] positionButtons = new JButton[9];
    // 在 ImageWatermarkProcessorGUI 类中添加以下成员变量
    private Timer previewUpdateTimer; // 用于延迟更新预览的定时器
    private static final int PREVIEW_UPDATE_DELAY = 300; // 延迟300毫秒更新预览
    private static final int PREVIEW_MAX_WIDTH = 500; // 增大预览图最大宽度
    private static final int PREVIEW_MAX_HEIGHT = 400; // 增大预览图最大高度

    private static class WatermarkTemplate {
        // 文本水印配置
        public String watermarkText;
        public String fontName;
        public int fontSize;
        public boolean bold;
        public boolean italic;
        public Color color;
        public int transparency;
        public boolean shadow;
        public boolean outline;

        // 图片水印配置
        public String imagePath;
        public int imageTransparency;
        public int imageScale;

        // 通用配置
        public boolean isTextWatermark;
        public String position;
        public int rotation;

        public WatermarkTemplate() {}
    }
    private static final String CONFIG_FILE = "watermark_config.json";
    private Map<String, WatermarkTemplate> templates = new HashMap<>();
    private JComboBox<String> templateCombo;
    private JButton saveTemplateButton;
    private JButton deleteTemplateButton;
    private JButton loadTemplateButton;
    private void initializeComponents() {
        // 在 initializeComponents() 方法中添加新组件
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

        positionCombo = new JComboBox<>(new String[]{
                "top-left", "top-center", "top-right",
                "middle-left", "center", "middle-right",
                "bottom-left", "bottom-center", "bottom-right"
        });
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
        // 初始化水印文本组件
        watermarkTextField = new JTextField("自定义水印文本", 15);
        fontNameCombo = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        boldCheckBox = new JCheckBox("粗体");
        italicCheckBox = new JCheckBox("斜体");
        colorPickerButton = new JButton("选择颜色");
        colorPickerButton.setBackground(Color.WHITE);
        transparencySlider = new JSlider(0, 100, 100);
        transparencyLabel = new JLabel("透明度: 100%");
        shadowCheckBox = new JCheckBox("阴影");
        outlineCheckBox = new JCheckBox("描边");
        textWatermarkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        textWatermarkRadio = new JRadioButton("文本水印", true);
        imageWatermarkRadio = new JRadioButton("图片水印");
        watermarkTypeGroup = new ButtonGroup();
        watermarkTypeGroup.add(textWatermarkRadio);
        watermarkTypeGroup.add(imageWatermarkRadio);

        imagePathField = new JTextField(15);
        browseImageButton = new JButton("浏览");
        imageTransparencySlider = new JSlider(0, 100, 100);
        imageTransparencyLabel = new JLabel("透明度: 100%");
        imageScaleSlider = new JSlider(10, 200, 100);
        imageScaleLabel = new JLabel("缩放: 100%");
        imageWatermarkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
        // 在 initializeComponents() 方法末尾添加
        previewLabel = new JLabel();
        previewLabel.setPreferredSize(new Dimension(PREVIEW_MAX_WIDTH, PREVIEW_MAX_HEIGHT));
        previewLabel.setBorder(BorderFactory.createEtchedBorder());
        previewLabel.setHorizontalAlignment(JLabel.CENTER);
        previewLabel.setVerticalAlignment(JLabel.CENTER);

        rotationLabel = new JLabel("旋转角度: 0°");
        rotationSlider = new JSlider(-180, 180, 0);
        rotationSlider.setMajorTickSpacing(90);
        rotationSlider.setMinorTickSpacing(30);
        rotationSlider.setPaintTicks(true);
        rotationSlider.setPaintLabels(true);

        // 初始化九宫格按钮图标或文字
        String[] positionLabels = {"TL", "TC", "TR", "ML", "C", "MR", "BL", "BC", "BR"};
        for (int i = 0; i < 9; i++) {
            positionButtons[i] = new JButton(positionLabels[i]);
            positionButtons[i].setFont(new Font("Arial", Font.PLAIN, 10));
        }

        // 模板管理控件初始化
        templateCombo = new JComboBox<>();
        templateCombo.setPreferredSize(new Dimension(120, 25));

        saveTemplateButton = new JButton("保存模板");
        deleteTemplateButton = new JButton("删除模板");
        loadTemplateButton = new JButton("加载模板");
    }

    // 修改 setupLayout 方法中的布局设置
    private void setupLayout() {
        // 顶部控制面板
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 导入按钮
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        controlPanel.add(importButton, gbc);
        gbc.gridx = 1;
        controlPanel.add(importFolderButton, gbc);

        // 字体大小
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
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

        // 水印类型选择
        gbc.gridx = 0; gbc.gridy = 5;
        controlPanel.add(textWatermarkRadio, gbc);
        gbc.gridx = 1;
        controlPanel.add(imageWatermarkRadio, gbc);

        // 文本水印面板
        gbc.gridx = 0; gbc.gridy = 6;
        controlPanel.add(new JLabel("水印文本:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(watermarkTextField, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        controlPanel.add(new JLabel("字体:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(fontNameCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        controlPanel.add(new JLabel("样式:"), gbc);
        JPanel stylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stylePanel.add(boldCheckBox);
        stylePanel.add(italicCheckBox);
        stylePanel.add(shadowCheckBox);
        stylePanel.add(outlineCheckBox);
        gbc.gridx = 1; gbc.gridwidth = 1;
        controlPanel.add(stylePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 9;
        controlPanel.add(new JLabel("颜色:"), gbc);
        gbc.gridx = 1;
        controlPanel.add(colorPickerButton, gbc);

        gbc.gridx = 0; gbc.gridy = 10;
        controlPanel.add(transparencyLabel, gbc);
        gbc.gridx = 1;
        controlPanel.add(transparencySlider, gbc);

        // 图片水印面板
        gbc.gridx = 0; gbc.gridy = 11;
        controlPanel.add(new JLabel("图片路径:"), gbc);
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.add(imagePathField, BorderLayout.CENTER);
        imagePanel.add(browseImageButton, BorderLayout.EAST);
        gbc.gridx = 1; gbc.gridwidth = 1;
        controlPanel.add(imagePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 12;
        controlPanel.add(imageTransparencyLabel, gbc);
        gbc.gridx = 1;
        controlPanel.add(imageTransparencySlider, gbc);

        gbc.gridx = 0; gbc.gridy = 13;
        controlPanel.add(imageScaleLabel, gbc);
        gbc.gridx = 1;
        controlPanel.add(imageScaleSlider, gbc);

        // JPEG质量控制
        gbc.gridx = 0; gbc.gridy = 14;
        controlPanel.add(jpegQualityLabel, gbc);
        gbc.gridx = 1;
        controlPanel.add(jpegQualitySlider, gbc);

        // 图片尺寸调整
        gbc.gridx = 0; gbc.gridy = 15;
        controlPanel.add(resizeCheckBox, gbc);
        gbc.gridx = 1;
        resizePanel.add(resizeTypeCombo);
        resizePanel.add(resizeValueField);
        resizePanel.add(resizeUnitLabel);
        controlPanel.add(resizePanel, gbc);

        // 命名规则
        gbc.gridx = 0; gbc.gridy = 16;
        controlPanel.add(new JLabel("命名规则:"), gbc);
        JPanel namingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namingPanel.add(keepOriginalRadio);
        namingPanel.add(addPrefixRadio);
        namingPanel.add(prefixField);
        namingPanel.add(addSuffixRadio);
        namingPanel.add(suffixField);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(namingPanel, gbc);

        // 导出路径
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 17;
        controlPanel.add(new JLabel("导出目录:"), gbc);
        JPanel exportPanel = new JPanel(new BorderLayout());
        exportPanel.add(exportPathField, BorderLayout.CENTER);
        exportPanel.add(browseExportButton, BorderLayout.EAST);
        gbc.gridx = 1; gbc.gridwidth = 1;
        controlPanel.add(exportPanel, gbc);

        // 导出按钮
        gbc.gridx = 0; gbc.gridy = 18; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(exportButton);
        buttonPanel.add(processButton);
        controlPanel.add(buttonPanel, gbc);

        // 状态栏
        gbc.gridx = 0; gbc.gridy = 19; gbc.gridwidth = 2;
        controlPanel.add(statusLabel, gbc);

        // 预览面板 - 放在主面板的东部
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("预览"));
        previewPanel.setPreferredSize(new Dimension(PREVIEW_MAX_WIDTH + 20, PREVIEW_MAX_HEIGHT + 250)); // 增加高度以容纳所有组件

        // 模板管理面板
        JPanel templatePanel = new JPanel(new GridBagLayout());
        GridBagConstraints tgbc = new GridBagConstraints();
        tgbc.insets = new Insets(2, 2, 2, 2);
        tgbc.fill = GridBagConstraints.HORIZONTAL;

        templatePanel.setBorder(BorderFactory.createTitledBorder("模板管理"));
        tgbc.gridx = 0; tgbc.gridy = 0;
        templatePanel.add(new JLabel("模板:"), tgbc);
        tgbc.gridx = 1;
        templatePanel.add(templateCombo, tgbc);
        tgbc.gridx = 0; tgbc.gridy = 1;
        templatePanel.add(saveTemplateButton, tgbc);
        tgbc.gridx = 1;
        templatePanel.add(loadTemplateButton, tgbc);
        tgbc.gridx = 0; tgbc.gridy = 2; tgbc.gridwidth = 2;
        templatePanel.add(deleteTemplateButton, tgbc);

        // 九宫格位置按钮
        JPanel positionGridPanel = new JPanel(new GridLayout(3, 3, 5, 5));
        positionGridPanel.setBorder(BorderFactory.createTitledBorder("位置"));
        positionGridPanel.setPreferredSize(new Dimension(150, 150));
        for (int i = 0; i < 9; i++) {
            positionButtons[i] = new JButton();
            positionButtons[i].setPreferredSize(new Dimension(40, 40));
            positionButtons[i].addActionListener(e -> {
                JButton source = (JButton) e.getSource();
                handlePositionButtonClick(source);
            });
            positionGridPanel.add(positionButtons[i]);
        }

        // 旋转控制
        JPanel rotationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rotationPanel.add(rotationLabel);
        rotationPanel.add(rotationSlider);

        // 创建一个包含预览图像和旋转控制的面板
        JPanel previewAndRotationPanel = new JPanel(new BorderLayout());
        previewAndRotationPanel.add(previewLabel, BorderLayout.CENTER);
        previewAndRotationPanel.add(rotationPanel, BorderLayout.SOUTH);

        // 组装预览面板 (按正确顺序添加组件)
        previewPanel.add(templatePanel, BorderLayout.NORTH);           // 模板管理面板放在顶部
        previewPanel.add(positionGridPanel, BorderLayout.CENTER);      // 位置按钮放在中间
        previewPanel.add(previewAndRotationPanel, BorderLayout.SOUTH); // 预览图像和旋转控制放在底部


        // 主面板布局
        mainPanel.add(controlPanel, BorderLayout.WEST);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(previewPanel, BorderLayout.EAST);

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
        // 在 setupEventHandlers() 方法中添加新事件监听器
        textWatermarkRadio.addActionListener(e -> updateWatermarkUI());
        imageWatermarkRadio.addActionListener(e -> updateWatermarkUI());
        colorPickerButton.addActionListener(e -> chooseColor());
        browseImageButton.addActionListener(e -> browseImageFile());
        transparencySlider.addChangeListener(e -> {
            transparencyLabel.setText("透明度: " + transparencySlider.getValue() + "%");
        });
        imageTransparencySlider.addChangeListener(e -> {
            imageTransparencyLabel.setText("透明度: " + imageTransparencySlider.getValue() + "%");
        });
        imageScaleSlider.addChangeListener(e -> {
            imageScaleLabel.setText("缩放: " + imageScaleSlider.getValue() + "%");
        });
        // 初始化UI状态
        updateWatermarkUI();
        // 初始化组件状态
        resizeTypeCombo.setEnabled(false);
        resizeValueField.setEnabled(false);
        resizeUnitLabel.setEnabled(false);
        // 添加旋转滑块监听器
        rotationSlider.addChangeListener(e -> {
            watermarkRotation = rotationSlider.getValue();
            rotationLabel.setText("旋转角度: " + watermarkRotation + "°");
            updatePreview();
        });

        // 添加图片列表选择监听器
        imageJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreview();
            }
        });

        // 添加预览标签的鼠标监听器（用于拖拽水印）
        previewLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (currentPreviewImage != null) {
                    Point clickPoint = e.getPoint();
                    // 检查点击是否在水印区域内
                    if (isPointInWatermark(clickPoint)) {
                        isDraggingWatermark = true;
                        dragStartPoint = clickPoint;
                        watermarkStartPoint = new Point(watermarkPosition);
                        // 设置鼠标光标为移动光标
                        previewLabel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDraggingWatermark) {
                    isDraggingWatermark = false;
                    previewLabel.setCursor(Cursor.getDefaultCursor());
                    // 更新预览
                    updatePreview();
                }
            }
        });

        previewLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDraggingWatermark && currentPreviewImage != null) {
                    int dx = e.getX() - dragStartPoint.x;
                    int dy = e.getY() - dragStartPoint.y;

                    watermarkPosition.x = watermarkStartPoint.x + dx;
                    watermarkPosition.y = watermarkStartPoint.y + dy;

                    // 实时更新预览（仅在拖拽时）
                    updatePreview();
                }
            }
        });

        addWatermarkPreviewListeners();

    // 初始化九宫格按钮图标或文字
        String[] positionLabels = {"TL", "TC", "TR", "ML", "C", "MR", "BL", "BC", "BR"};
        for (int i = 0; i < 9; i++) {
            positionButtons[i].setText(positionLabels[i]);
        }
        saveTemplateButton.addActionListener(e -> saveTemplate());
        deleteTemplateButton.addActionListener(e -> deleteTemplate());
        loadTemplateButton.addActionListener(e -> loadTemplate());
    }
    // 在类中添加以下新方法

    /**
     * 更新预览图像
     */
    private void updatePreview() {
        if (imageJList.getSelectedValue() != null) {
            ImageInfo selectedImage = imageJList.getSelectedValue();
            try {
                BufferedImage originalImage = ImageIO.read(selectedImage.getFile());
                if (originalImage == null) {
                    previewLabel.setIcon(null);
                    currentPreviewImage = null;
                    return;
                }

                // 缩放到适合预览的大小
                BufferedImage scaledImage = scaleImageForPreview(originalImage);

                // 添加水印
                BufferedImage watermarkedImage = addWatermarkToPreview(scaledImage);

                // 显示在预览标签上
                previewLabel.setIcon(new ImageIcon(watermarkedImage));
                currentPreviewImage = watermarkedImage;
            } catch (IOException e) {
                e.printStackTrace();
                previewLabel.setIcon(null);
                currentPreviewImage = null;
            }
        } else {
            previewLabel.setIcon(null);
            currentPreviewImage = null;
        }
    }


    /**
     * 缩放图片以适合预览区域
     */
    /**
     * 缩放图片以适合预览区域
     */
    private BufferedImage scaleImageForPreview(BufferedImage originalImage) {
        int maxWidth = PREVIEW_MAX_WIDTH;
        int maxHeight = PREVIEW_MAX_HEIGHT;

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 保持宽高比
        double scale = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // 使用更高质量的缩放算法
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g2d = scaledImage.createGraphics();

        // 设置渲染提示以获得更好的图像质量
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaledImage;
    }


    /**
     * 为预览图像添加水印
     */
    private BufferedImage addWatermarkToPreview(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        BufferedImage watermarkedImage = new BufferedImage(width, height,
                originalImage.getType() != BufferedImage.TYPE_INT_ARGB ?
                        BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarkedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);

        if (textWatermarkRadio.isSelected()) {
            addTextWatermarkToPreview(g2d, width, height);
        } else {
            addImageWatermarkToPreview(g2d, width, height);
        }

        g2d.dispose();
        return watermarkedImage;
    }

    /**
     * 为预览图像添加文本水印
     */
    private void addTextWatermarkToPreview(Graphics2D g2d, int imageWidth, int imageHeight) {
        String text = watermarkTextField.getText();
        String fontName = (String) fontNameCombo.getSelectedItem();
        int fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
        int style = Font.PLAIN;
        if (boldCheckBox.isSelected()) style |= Font.BOLD;
        if (italicCheckBox.isSelected()) style |= Font.ITALIC;

        Font font = new Font(fontName, style, fontSize);
        g2d.setFont(font);

        Color baseColor = colorPickerButton.getBackground();
        int alpha = (int) (transparencySlider.getValue() * 2.55);
        Color color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        g2d.setColor(color);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = fontMetrics.getHeight();

        // 计算水印位置
        Point position = calculateWatermarkPosition(imageWidth, imageHeight, textWidth, textHeight);

        // 应用手动拖拽偏移
        position.x += watermarkPosition.x;
        position.y += watermarkPosition.y;

        AffineTransform orig = g2d.getTransform();

        if (watermarkRotation != 0) {
            // 执行旋转变换
            AffineTransform transform = new AffineTransform();
            transform.rotate(Math.toRadians(watermarkRotation), position.x + textWidth/2.0, position.y - textHeight/2.0);
            g2d.transform(transform);
        }

        // 添加阴影效果
        if (shadowCheckBox.isSelected()) {
            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.drawString(text, position.x + 2, position.y + 2);
            g2d.setColor(color);
        }

        // 添加描边效果
        if (outlineCheckBox.isSelected()) {
            g2d.setColor(new Color(0, 0, 0, alpha));
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) {
                        g2d.drawString(text, position.x + i, position.y + j);
                    }
                }
            }
            g2d.setColor(color);
        }

        // 绘制文本
        g2d.drawString(text, position.x, position.y);

        // 恢复原始变换
        g2d.setTransform(orig);
    }

    /**
     * 为预览图像添加图片水印
     */
    private void addImageWatermarkToPreview(Graphics2D g2d, int imageWidth, int imageHeight) {
        String imagePath = imagePathField.getText().trim();
        if (imagePath.isEmpty()) {
            return;
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            return;
        }

        try {
            BufferedImage watermarkImage = ImageIO.read(imageFile);
            if (watermarkImage == null) {
                return;
            }

            int scalePercent = imageScaleSlider.getValue();
            int scaledWidth = (int) (watermarkImage.getWidth() * scalePercent / 100.0);
            int scaledHeight = (int) (watermarkImage.getHeight() * scalePercent / 100.0);

            Point position = calculateWatermarkPosition(imageWidth, imageHeight, scaledWidth, scaledHeight);

            // 应用手动拖拽偏移
            position.x += watermarkPosition.x;
            position.y += watermarkPosition.y;

            int alpha = imageTransparencySlider.getValue();
            if (alpha < 100) {
                AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 100.0f);
                g2d.setComposite(alphaComposite);
            }

            AffineTransform orig = g2d.getTransform();

            if (watermarkRotation != 0) {
                // 执行旋转变换
                AffineTransform transform = new AffineTransform();
                transform.rotate(Math.toRadians(watermarkRotation), position.x + scaledWidth/2.0, position.y + scaledHeight/2.0);
                g2d.transform(transform);
            }

            g2d.drawImage(watermarkImage, position.x, position.y, scaledWidth, scaledHeight, null);

            // 恢复原始变换
            g2d.setTransform(orig);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据选定的位置计算水印坐标
     */
    private Point calculateWatermarkPosition(int imageWidth, int imageHeight, int watermarkWidth, int watermarkHeight) {
        Point position = new Point(0, 0);

        // 如果用户手动设置了位置，则使用该位置
        if (watermarkPosition.x != 0 || watermarkPosition.y != 0) {
            position.x = watermarkPosition.x;
            position.y = watermarkPosition.y;
            return position;
        }

        String positionStr = (String) positionCombo.getSelectedItem();
        switch (positionStr.toLowerCase()) {
            case "top-left":
                position.x = 10;
                position.y = watermarkHeight;
                break;
            case "top-center":
                position.x = (imageWidth - watermarkWidth) / 2;
                position.y = watermarkHeight;
                break;
            case "top-right":
                position.x = imageWidth - watermarkWidth - 10;
                position.y = watermarkHeight;
                break;
            case "middle-left":
                position.x = 10;
                position.y = (imageHeight - watermarkHeight) / 2;
                break;
            case "center":
                position.x = (imageWidth - watermarkWidth) / 2;
                position.y = (imageHeight - watermarkHeight) / 2;
                break;
            case "middle-right":
                position.x = imageWidth - watermarkWidth - 10;
                position.y = (imageHeight - watermarkHeight) / 2;
                break;
            case "bottom-left":
                position.x = 10;
                position.y = imageHeight - 10;
                break;
            case "bottom-center":
                position.x = (imageWidth - watermarkWidth) / 2;
                position.y = imageHeight - 10;
                break;
            case "bottom-right":
            default:
                position.x = imageWidth - watermarkWidth - 10;
                position.y = imageHeight - 10;
                break;
        }

        return position;
    }

    /**
     * 检查点击点是否在水印区域内
     */
    private boolean isPointInWatermark(Point point) {
        if (currentPreviewImage == null) return false;

        try {
            // 获取水印的大概位置和尺寸
            if (textWatermarkRadio.isSelected()) {
                // 文本水印
                String text = watermarkTextField.getText();
                int fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
                Font font = new Font((String) fontNameCombo.getSelectedItem(), Font.PLAIN, fontSize);

                BufferedImage temp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = temp.createGraphics();
                g2d.setFont(font);
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.dispose();

                // 计算水印位置
                Point position = calculateWatermarkPosition(
                        currentPreviewImage.getWidth(),
                        currentPreviewImage.getHeight(),
                        textWidth,
                        textHeight
                );

                // 应用手动拖拽偏移
                position.x += watermarkPosition.x;
                position.y += watermarkPosition.y;

                // 检查点是否在文本区域内
                return point.x >= position.x && point.x <= position.x + textWidth &&
                        point.y >= position.y - textHeight && point.y <= position.y;
            } else {
                // 图片水印
                String imagePath = imagePathField.getText().trim();
                if (imagePath.isEmpty()) return false;

                File imageFile = new File(imagePath);
                if (!imageFile.exists()) return false;

                BufferedImage watermarkImage = ImageIO.read(imageFile);
                if (watermarkImage == null) return false;

                int scalePercent = imageScaleSlider.getValue();
                int scaledWidth = (int) (watermarkImage.getWidth() * scalePercent / 100.0);
                int scaledHeight = (int) (watermarkImage.getHeight() * scalePercent / 100.0);

                // 计算水印位置
                Point position = calculateWatermarkPosition(
                        currentPreviewImage.getWidth(),
                        currentPreviewImage.getHeight(),
                        scaledWidth,
                        scaledHeight
                );

                // 应用手动拖拽偏移
                position.x += watermarkPosition.x;
                position.y += watermarkPosition.y;

                // 检查点是否在图片区域内
                return point.x >= position.x && point.x <= position.x + scaledWidth &&
                        point.y >= position.y && point.y <= position.y + scaledHeight;
            }
        } catch (Exception e) {
            // 出错时使用简化判断
            int watermarkAreaX = currentPreviewImage.getWidth() - 150;
            int watermarkAreaY = currentPreviewImage.getHeight() - 100;

            return point.x >= watermarkAreaX && point.x <= currentPreviewImage.getWidth() &&
                    point.y >= watermarkAreaY && point.y <= currentPreviewImage.getHeight();
        }
    }


    /**
     * 处理位置按钮点击事件
     */
    private void handlePositionButtonClick(JButton button) {
        // 重置手动位置偏移
        watermarkPosition = new Point(0, 0);

        // 根据按钮位置更新下拉框选项
        for (int i = 0; i < positionButtons.length; i++) {
            if (positionButtons[i] == button) {
                String[] positions = {"top-left", "top-center", "top-right",
                        "middle-left", "center", "middle-right",
                        "bottom-left", "bottom-center", "bottom-right"};
                positionCombo.setSelectedItem(positions[i]);
                break;
            }
        }

        updatePreview();
    }

    /**
     * 根据当前坐标更新位置下拉框
     */
    private void updatePositionComboFromCoordinates() {
        // 此方法可以根据水印的实际位置更新位置下拉框
        // 为了简化，这里留空，但在完整实现中可以根据watermarkPosition计算最接近的预设位置
    }

    /**
     * 在适当的地方调用此方法以更新预览
     * 例如，在setupEventHandlers()中添加对水印相关控件的监听器
     */
    /**
     * 在适当的地方调用此方法以更新预览
     * 例如，在setupEventHandlers()中添加对水印相关控件的监听器
     */
    private void addWatermarkPreviewListeners() {
        // 初始化预览更新定时器
        previewUpdateTimer = new Timer(PREVIEW_UPDATE_DELAY, e -> updatePreview());
        previewUpdateTimer.setRepeats(false); // 只执行一次

        // 创建一个通用的更新预览方法
        ActionListener previewUpdateAction = e -> {
            if (previewUpdateTimer.isRunning()) {
                previewUpdateTimer.restart(); // 重置定时器
            } else {
                previewUpdateTimer.start(); // 启动定时器
            }
        };

        // 监听所有影响水印外观的控件
        watermarkTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                previewUpdateAction.actionPerformed(null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                previewUpdateAction.actionPerformed(null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                previewUpdateAction.actionPerformed(null);
            }
        });

        fontNameCombo.addActionListener(previewUpdateAction);
        fontSizeCombo.addActionListener(previewUpdateAction);
        boldCheckBox.addActionListener(previewUpdateAction);
        italicCheckBox.addActionListener(previewUpdateAction);
        colorPickerButton.addActionListener(previewUpdateAction);

        transparencySlider.addChangeListener(e -> {
            transparencyLabel.setText("透明度: " + transparencySlider.getValue() + "%");
            previewUpdateAction.actionPerformed(null);
        });

        shadowCheckBox.addActionListener(previewUpdateAction);
        outlineCheckBox.addActionListener(previewUpdateAction);

        imagePathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                previewUpdateAction.actionPerformed(null);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                previewUpdateAction.actionPerformed(null);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                previewUpdateAction.actionPerformed(null);
            }
        });

        imageTransparencySlider.addChangeListener(e -> {
            imageTransparencyLabel.setText("透明度: " + imageTransparencySlider.getValue() + "%");
            previewUpdateAction.actionPerformed(null);
        });

        imageScaleSlider.addChangeListener(e -> {
            imageScaleLabel.setText("缩放: " + imageScaleSlider.getValue() + "%");
            previewUpdateAction.actionPerformed(null);
        });

        positionCombo.addActionListener(e -> {
            // 重置手动位置偏移
            watermarkPosition = new Point(0, 0);
            previewUpdateAction.actionPerformed(null);
        });
    }


    // 添加颜色选择方法
    private void chooseColor() {
        Color newColor = JColorChooser.showDialog(this, "选择水印颜色", colorPickerButton.getBackground());
        if (newColor != null) {
            colorPickerButton.setBackground(newColor);
        }
    }

    // 添加图片文件浏览方法
    private void browseImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("图片文件 (PNG, JPG, JPEG)", "png", "jpg", "jpeg"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            imagePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    // 更新水印UI状态
    private void updateWatermarkUI() {
        boolean isTextWatermark = textWatermarkRadio.isSelected();

        // 文本水印组件
        watermarkTextField.setEnabled(isTextWatermark);
        fontNameCombo.setEnabled(isTextWatermark);
        boldCheckBox.setEnabled(isTextWatermark);
        italicCheckBox.setEnabled(isTextWatermark);
        colorPickerButton.setEnabled(isTextWatermark);
        transparencySlider.setEnabled(isTextWatermark);
        shadowCheckBox.setEnabled(isTextWatermark);
        outlineCheckBox.setEnabled(isTextWatermark);

        // 图片水印组件
        imagePathField.setEnabled(!isTextWatermark);
        browseImageButton.setEnabled(!isTextWatermark);
        imageTransparencySlider.setEnabled(!isTextWatermark);
        imageScaleSlider.setEnabled(!isTextWatermark);
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

    // 修改 processSingleImage 方法调用
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

        // 添加水印
        BufferedImage watermarkedImage = addWatermark(processedImage);

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

    // 修改 addWatermark 方法以支持新功能
    private BufferedImage addWatermark(BufferedImage originalImage) throws IOException {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // 创建新的图片用于绘制水印
        BufferedImage watermarkedImage = new BufferedImage(width, height,
                originalImage.getType() != BufferedImage.TYPE_INT_ARGB ?
                        BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = watermarkedImage.createGraphics();

        // 绘制原图
        g2d.drawImage(originalImage, 0, 0, null);

        if (textWatermarkRadio.isSelected()) {
            // 添加文本水印
            addTextWatermark(g2d, width, height);
        } else {
            // 添加图片水印
            addImageWatermark(g2d, width, height);
        }

        g2d.dispose();
        return watermarkedImage;
    }
    // 添加文本水印方法
    private void addTextWatermark(Graphics2D g2d, int imageWidth, int imageHeight) {
        String text = watermarkTextField.getText();
        String fontName = (String) fontNameCombo.getSelectedItem();
        int fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
        int style = Font.PLAIN;
        if (boldCheckBox.isSelected()) style |= Font.BOLD;
        if (italicCheckBox.isSelected()) style |= Font.ITALIC;

        Font font = new Font(fontName, style, fontSize);
        g2d.setFont(font);

        // 设置颜色和透明度
        Color baseColor = colorPickerButton.getBackground();
        int alpha = (int) (transparencySlider.getValue() * 2.55); // 转换为0-255范围
        Color color = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alpha);
        g2d.setColor(color);

        // 启用抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 计算文本尺寸
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int textHeight = fontMetrics.getHeight();

        // 根据位置设置水印坐标
        String position = (String) positionCombo.getSelectedItem();
        int x = 0, y = 0;
        switch (position.toLowerCase()) {
            case "top-left":
                x = 10;
                y = textHeight;
                break;
            case "top-right":
                x = imageWidth - textWidth - 10;
                y = textHeight;
                break;
            case "center":
                x = (imageWidth - textWidth) / 2;
                y = imageHeight / 2;
                break;
            case "bottom-left":
                x = 10;
                y = imageHeight - 10;
                break;
            case "bottom-right":
            default:
                x = imageWidth - textWidth - 10;
                y = imageHeight - 10;
                break;
        }

        // 添加阴影效果
        if (shadowCheckBox.isSelected()) {
            g2d.setColor(new Color(0, 0, 0, alpha));
            g2d.drawString(text, x + 2, y + 2);
            g2d.setColor(color);
        }

        // 添加描边效果
        if (outlineCheckBox.isSelected()) {
            // 绘制描边
            g2d.setColor(new Color(0, 0, 0, alpha));
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) {
                        g2d.drawString(text, x + i, y + j);
                    }
                }
            }
            g2d.setColor(color);
        }

        // 绘制文本
        g2d.drawString(text, x, y);
    }

    // 添加图片水印方法
    private void addImageWatermark(Graphics2D g2d, int imageWidth, int imageHeight) throws IOException {
        String imagePath = imagePathField.getText().trim();
        if (imagePath.isEmpty()) {
            throw new IOException("请选择水印图片");
        }

        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("水印图片文件不存在");
        }

        BufferedImage watermarkImage = ImageIO.read(imageFile);
        if (watermarkImage == null) {
            throw new IOException("无法读取水印图片");
        }

        // 计算缩放后的尺寸
        int scalePercent = imageScaleSlider.getValue();
        int scaledWidth = (int) (watermarkImage.getWidth() * scalePercent / 100.0);
        int scaledHeight = (int) (watermarkImage.getHeight() * scalePercent / 100.0);

        // 根据位置设置水印坐标
        String position = (String) positionCombo.getSelectedItem();
        int x = 0, y = 0;
        switch (position.toLowerCase()) {
            case "top-left":
                x = 10;
                y = 10;
                break;
            case "top-right":
                x = imageWidth - scaledWidth - 10;
                y = 10;
                break;
            case "center":
                x = (imageWidth - scaledWidth) / 2;
                y = (imageHeight - scaledHeight) / 2;
                break;
            case "bottom-left":
                x = 10;
                y = imageHeight - scaledHeight - 10;
                break;
            case "bottom-right":
            default:
                x = imageWidth - scaledWidth - 10;
                y = imageHeight - scaledHeight - 10;
                break;
        }

        // 设置透明度
        int alpha = imageTransparencySlider.getValue();
        if (alpha < 100) {
            AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 100.0f);
            g2d.setComposite(alphaComposite);
        }

        // 绘制水印图片
        g2d.drawImage(watermarkImage, x, y, scaledWidth, scaledHeight, null);
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

    // 在类内部添加这个简单文档监听器类
    abstract class SimpleDocumentListener implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            update(e);
        }

        public void removeUpdate(DocumentEvent e) {
            update(e);
        }

        public void changedUpdate(DocumentEvent e) {
            update(e);
        }

        public abstract void update(DocumentEvent e);
    }

    // 在类中添加以下方法

    /**
     * 检查模板是否为空
     */
    private boolean isTemplateEmpty(WatermarkTemplate template) {
        // 检查文本水印是否为空
        boolean isTextEmpty = template.watermarkText == null || template.watermarkText.isEmpty();
        
        // 检查图片水印是否为空
        boolean isImageEmpty = template.imagePath == null || template.imagePath.isEmpty();
        
        // 如果文本和图片水印都为空，则模板为空
        return isTextEmpty && isImageEmpty;
    }

    /**
     * 保存当前配置为模板
     */
    private void saveTemplate() {
        String templateName = JOptionPane.showInputDialog(this, "请输入模板名称:", "保存模板", JOptionPane.QUESTION_MESSAGE);
        if (templateName != null && !templateName.trim().isEmpty()) {
            templateName = templateName.trim();
            if (templates.containsKey(templateName)) {
                int result = JOptionPane.showConfirmDialog(this,
                        "模板 \"" + templateName + "\" 已存在，是否覆盖?",
                        "确认覆盖",
                        JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            WatermarkTemplate template = new WatermarkTemplate();

            // 保存文本水印配置
            template.watermarkText = watermarkTextField.getText();
            template.fontName = (String) fontNameCombo.getSelectedItem();
            template.fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
            template.bold = boldCheckBox.isSelected();
            template.italic = italicCheckBox.isSelected();
            template.color = colorPickerButton.getBackground();
            template.transparency = transparencySlider.getValue();
            template.shadow = shadowCheckBox.isSelected();
            template.outline = outlineCheckBox.isSelected();

            // 保存图片水印配置
            template.imagePath = imagePathField.getText();
            template.imageTransparency = imageTransparencySlider.getValue();
            template.imageScale = imageScaleSlider.getValue();

            // 保存通用配置
            template.isTextWatermark = textWatermarkRadio.isSelected();
            template.position = (String) positionCombo.getSelectedItem();
            template.rotation = rotationSlider.getValue();

            templates.put(templateName, template);
            saveTemplates();
            updateTemplateCombo();

            JOptionPane.showMessageDialog(this, "模板保存成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 删除选中的模板
     */
    private void deleteTemplate() {
        String selectedTemplate = (String) templateCombo.getSelectedItem();
        if (selectedTemplate != null) {
            int result = JOptionPane.showConfirmDialog(this,
                    "确定要删除模板 \"" + selectedTemplate + "\" 吗?",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                templates.remove(selectedTemplate);
                saveTemplates();
                updateTemplateCombo();
                JOptionPane.showMessageDialog(this, "模板删除成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "请先选择一个模板!", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * 加载选中的模板
     */
    private void loadTemplate() {
        String selectedTemplate = (String) templateCombo.getSelectedItem();
        if (selectedTemplate != null) {
            WatermarkTemplate template = templates.get(selectedTemplate);
            if (template != null) {
                applyTemplate(template);
                JOptionPane.showMessageDialog(this, "模板加载成功!", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "请先选择一个模板!", "提示", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * 应用模板配置
     */
    private void applyTemplate(WatermarkTemplate template) {
        // 应用文本水印配置
        watermarkTextField.setText(template.watermarkText);
        fontNameCombo.setSelectedItem(template.fontName);
        fontSizeCombo.setSelectedItem(String.valueOf(template.fontSize));
        boldCheckBox.setSelected(template.bold);
        italicCheckBox.setSelected(template.italic);
        colorPickerButton.setBackground(template.color);
        transparencySlider.setValue(template.transparency);
        shadowCheckBox.setSelected(template.shadow);
        outlineCheckBox.setSelected(template.outline);

        // 应用图片水印配置
        imagePathField.setText(template.imagePath);
        imageTransparencySlider.setValue(template.imageTransparency);
        imageScaleSlider.setValue(template.imageScale);

        // 应用通用配置
        if (template.isTextWatermark) {
            textWatermarkRadio.setSelected(true);
        } else {
            imageWatermarkRadio.setSelected(true);
        }
        updateWatermarkUI();
        positionCombo.setSelectedItem(template.position);
        rotationSlider.setValue(template.rotation);

        // 更新UI显示
        transparencyLabel.setText("透明度: " + template.transparency + "%");
        imageTransparencyLabel.setText("透明度: " + template.imageTransparency + "%");
        imageScaleLabel.setText("缩放: " + template.imageScale + "%");
        rotationLabel.setText("旋转角度: " + template.rotation + "°");

        updatePreview();
    }

    /**
     * 更新模板下拉框
     */
    private void updateTemplateCombo() {
        templateCombo.removeAllItems();
        for (String templateName : templates.keySet()) {
            templateCombo.addItem(templateName);
        }
    }

    /**
     * 保存模板到文件
     */
    private void saveTemplates() {
        try {
            Map<String, Object> data = new HashMap<>();

            // 保存模板数据
            Map<String, Map<String, Object>> templatesData = new HashMap<>();
            for (Map.Entry<String, WatermarkTemplate> entry : templates.entrySet()) {
                String name = entry.getKey();
                WatermarkTemplate template = entry.getValue();

                Map<String, Object> templateData = new HashMap<>();

                // 保存文本水印配置
                templateData.put("watermarkText", template.watermarkText != null ? template.watermarkText : "");
                templateData.put("fontName", template.fontName != null ? template.fontName : "Arial");
                templateData.put("fontSize", template.fontSize);
                templateData.put("bold", template.bold);
                templateData.put("italic", template.italic);
                templateData.put("color", String.format("#%02x%02x%02x",
                        template.color.getRed(),
                        template.color.getGreen(),
                        template.color.getBlue()));
                templateData.put("transparency", template.transparency);
                templateData.put("shadow", template.shadow);
                templateData.put("outline", template.outline);

                // 保存图片水印配置
                templateData.put("imagePath", template.imagePath != null ? template.imagePath : "");
                templateData.put("imageTransparency", template.imageTransparency);
                templateData.put("imageScale", template.imageScale);

                // 保存通用配置
                templateData.put("isTextWatermark", template.isTextWatermark);
                templateData.put("position", template.position != null ? template.position : "bottom-right");
                templateData.put("rotation", template.rotation);

                templatesData.put(name, templateData);
            }
            data.put("templates", templatesData);

            // 保存当前配置作为默认配置
            WatermarkTemplate currentConfig = new WatermarkTemplate();
            currentConfig.watermarkText = watermarkTextField.getText();
            currentConfig.fontName = (String) fontNameCombo.getSelectedItem();
            currentConfig.fontSize = Integer.parseInt((String) fontSizeCombo.getSelectedItem());
            currentConfig.bold = boldCheckBox.isSelected();
            currentConfig.italic = italicCheckBox.isSelected();
            currentConfig.color = colorPickerButton.getBackground();
            currentConfig.transparency = transparencySlider.getValue();
            currentConfig.shadow = shadowCheckBox.isSelected();
            currentConfig.outline = outlineCheckBox.isSelected();
            currentConfig.imagePath = imagePathField.getText();
            currentConfig.imageTransparency = imageTransparencySlider.getValue();
            currentConfig.imageScale = imageScaleSlider.getValue();
            currentConfig.isTextWatermark = textWatermarkRadio.isSelected();
            currentConfig.position = (String) positionCombo.getSelectedItem();
            currentConfig.rotation = rotationSlider.getValue();

            // 只有当配置不为空时才保存
            if (!isTemplateEmpty(currentConfig)) {
                Map<String, Object> currentConfigData = new HashMap<>();
                currentConfigData.put("watermarkText", currentConfig.watermarkText != null ? currentConfig.watermarkText : "");
                currentConfigData.put("fontName", currentConfig.fontName != null ? currentConfig.fontName : "Arial");
                currentConfigData.put("fontSize", currentConfig.fontSize);
                currentConfigData.put("bold", currentConfig.bold);
                currentConfigData.put("italic", currentConfig.italic);
                currentConfigData.put("color", String.format("#%02x%02x%02x",
                        currentConfig.color.getRed(),
                        currentConfig.color.getGreen(),
                        currentConfig.color.getBlue()));
                currentConfigData.put("transparency", currentConfig.transparency);
                currentConfigData.put("shadow", currentConfig.shadow);
                currentConfigData.put("outline", currentConfig.outline);
                currentConfigData.put("imagePath", currentConfig.imagePath != null ? currentConfig.imagePath : "");
                currentConfigData.put("imageTransparency", currentConfig.imageTransparency);
                currentConfigData.put("imageScale", currentConfig.imageScale);
                currentConfigData.put("isTextWatermark", currentConfig.isTextWatermark);
                currentConfigData.put("position", currentConfig.position != null ? currentConfig.position : "bottom-right");
                currentConfigData.put("rotation", currentConfig.rotation);

                data.put("lastConfiguration", currentConfigData);
            }

            // 写入文件
            try (FileWriter writer = new FileWriter(CONFIG_FILE, StandardCharsets.UTF_8)) {
                writer.write(mapToJson(data));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 从文件加载模板
     */
    private void loadTemplates() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                    StringBuilder jsonBuilder = new StringBuilder();
                    int character;
                    while ((character = reader.read()) != -1) {
                        jsonBuilder.append((char) character);
                    }

                    String jsonString = jsonBuilder.toString();
                    if (jsonString.trim().isEmpty()) {
                        return; // 文件为空，直接返回
                    }

                    Map<String, Object> data = jsonToMap(jsonString);

                    // 加载模板
                    if (data.containsKey("templates")) {
                        Object templatesObj = data.get("templates");
                        if (templatesObj instanceof Map) {
                            Map<String, Object> templatesData = (Map<String, Object>) templatesObj;
                            templates.clear();

                            for (Map.Entry<String, Object> entry : templatesData.entrySet()) {
                                String name = entry.getKey();
                                Object templateObj = entry.getValue();

                                if (templateObj instanceof Map) {
                                    Map<String, Object> templateData = (Map<String, Object>) templateObj;

                                    WatermarkTemplate template = new WatermarkTemplate();

                                    // 加载文本水印配置
                                    template.watermarkText = getStringValue(templateData.get("watermarkText"));
                                    template.fontName = getStringValue(templateData.get("fontName"));
                                    template.fontSize = getObjectAsInt(templateData.get("fontSize"));
                                    template.bold = getObjectAsBoolean(templateData.get("bold"));
                                    template.italic = getObjectAsBoolean(templateData.get("italic"));
                                    template.color = colorFromString(getStringValue(templateData.get("color")));
                                    template.transparency = getObjectAsInt(templateData.get("transparency"));
                                    template.shadow = getObjectAsBoolean(templateData.get("shadow"));
                                    template.outline = getObjectAsBoolean(templateData.get("outline"));

                                    // 加载图片水印配置
                                    template.imagePath = getStringValue(templateData.get("imagePath"));
                                    template.imageTransparency = getObjectAsInt(templateData.get("imageTransparency"));
                                    template.imageScale = getObjectAsInt(templateData.get("imageScale"));

                                    // 加载通用配置
                                    template.isTextWatermark = getObjectAsBoolean(templateData.get("isTextWatermark"));
                                    template.position = getStringValue(templateData.get("position"));
                                    template.rotation = getObjectAsInt(templateData.get("rotation"));

                                    templates.put(name, template);
                                }
                            }
                            updateTemplateCombo();
                        }
                    }
                }
            }
            // 即使配置文件不存在，也要更新模板下拉框以确保界面一致性
            updateTemplateCombo();
        } catch (Exception e) {
            e.printStackTrace();
            // 出错时也要更新模板下拉框
            updateTemplateCombo();
        }
    }



    /**
     * 安全获取字符串值
     */
    private String getStringValue(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    /**
     * 将Object转换为int
     */
    private int getObjectAsInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 将Object转换为boolean
     */
    private boolean getObjectAsBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        }
        return false;
    }

    /**
     * 加载上次配置
     */
    private void loadLastConfiguration() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    StringBuilder jsonBuilder = new StringBuilder();
                    int character;
                    while ((character = reader.read()) != -1) {
                        jsonBuilder.append((char) character);
                    }

                    String jsonString = jsonBuilder.toString();
                    if (jsonString.trim().isEmpty()) {
                        return; // 文件为空，直接返回
                    }

                    Map<String, Object> data = jsonToMap(jsonString);

                    // 只有在存在上次配置且不为空时才加载，否则不执行任何操作
                    if (data.containsKey("lastConfiguration")) {
                        Object configObj = data.get("lastConfiguration");
                        if (configObj instanceof Map) {
                            Map<String, Object> configData = (Map<String, Object>) configObj;
                            
                            // 检查配置是否包含有效数据
                            if (!configData.isEmpty()) {
                                WatermarkTemplate template = new WatermarkTemplate();

                                // 加载文本水印配置
                                template.watermarkText = getStringValue(configData.get("watermarkText"));
                                template.fontName = getStringValue(configData.get("fontName"));
                                template.fontSize = getObjectAsInt(configData.get("fontSize"));
                                template.bold = getObjectAsBoolean(configData.get("bold"));
                                template.italic = getObjectAsBoolean(configData.get("italic"));
                                template.color = colorFromString(getStringValue(configData.get("color")));
                                template.transparency = getObjectAsInt(configData.get("transparency"));
                                template.shadow = getObjectAsBoolean(configData.get("shadow"));
                                template.outline = getObjectAsBoolean(configData.get("outline"));

                                // 加载图片水印配置
                                template.imagePath = getStringValue(configData.get("imagePath"));
                                template.imageTransparency = getObjectAsInt(configData.get("imageTransparency"));
                                template.imageScale = getObjectAsInt(configData.get("imageScale"));

                                // 加载通用配置
                                template.isTextWatermark = getObjectAsBoolean(configData.get("isTextWatermark"));
                                template.position = getStringValue(configData.get("position"));
                                template.rotation = getObjectAsInt(configData.get("rotation"));

                                applyTemplate(template);
                            }
                        }
                    }
                }
            }
            // 如果配置文件不存在，不执行任何操作，保持界面默认值
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 从颜色字符串创建Color对象
     */
    private Color colorFromString(String colorStr) {
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }

        int r = Integer.parseInt(colorStr.substring(0, 2), 16);
        int g = Integer.parseInt(colorStr.substring(2, 4), 16);
        int b = Integer.parseInt(colorStr.substring(4, 6), 16);

        return new Color(r, g, b);
    }

    /**
     * 简单的Map转JSON字符串方法
     */
    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }

            json.append("\"").append(escapeJsonString(entry.getKey())).append("\":");
            Object value = entry.getValue();
            json.append(objectToJson(value));

            first = false;
        }

        json.append("}");
        return json.toString();
    }
    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJsonString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    /**
     * 将Object转换为JSON字符串
     */
    private String objectToJson(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof String) {
            return "\"" + escapeJsonString((String) obj) + "\"";
        } else if (obj instanceof Map) {
            return mapToJson((Map<String, Object>) obj);
        } else {
            return obj.toString();
        }
    }
    /**
     * 简单的JSON字符串转Map方法
     */
    private Map<String, Object> jsonToMap(String json) {
        Map<String, Object> map = new HashMap<>();

        try {
            // 移除大括号
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1);
            }

            // 处理空对象
            if (json.trim().isEmpty()) {
                return map;
            }

            // 分割键值对，但需要考虑嵌套对象和数组
            int braceCount = 0;
            int bracketCount = 0;
            int start = 0;
            boolean inString = false;
            char stringDelimiter = '"';

            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);

                // 处理字符串内的字符（忽略分隔符）
                if (inString) {
                    if (c == stringDelimiter && (i == 0 || json.charAt(i - 1) != '\\')) {
                        inString = false;
                    }
                    continue;
                }

                // 检查字符串开始
                if (c == '"' || c == '\'') {
                    inString = true;
                    stringDelimiter = c;
                    continue;
                }

                // 计算大括号和中括号层级
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                } else if (c == '[') {
                    bracketCount++;
                } else if (c == ']') {
                    bracketCount--;
                }

                // 当不在嵌套结构中且遇到逗号时，分割键值对
                if (braceCount == 0 && bracketCount == 0 && c == ',') {
                    String pair = json.substring(start, i).trim();
                    parseKeyValuePair(pair, map);
                    start = i + 1;
                }
            }

            // 处理最后一个键值对
            if (start < json.length()) {
                String pair = json.substring(start).trim();
                parseKeyValuePair(pair, map);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    /**
     * 解析单个键值对
     */
    private void parseKeyValuePair(String pair, Map<String, Object> map) {
        try {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();

                // 清理键名（移除引号）
                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }

                // 根据值的类型进行解析
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // 字符串值
                    String stringValue = value.substring(1, value.length() - 1);
                    // 处理转义字符
                    stringValue = stringValue.replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                            .replace("\\n", "\n")
                            .replace("\\r", "\r")
                            .replace("\\t", "\t");
                    map.put(key, stringValue);
                } else if (value.equals("true")) {
                    // 布尔值 true
                    map.put(key, true);
                } else if (value.equals("false")) {
                    // 布尔值 false
                    map.put(key, false);
                } else if (value.equals("null")) {
                    // null 值
                    map.put(key, null);
                } else if (value.startsWith("{") && value.endsWith("}")) {
                    // 嵌套对象
                    map.put(key, jsonToMap(value));
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    // 数组（简化处理为字符串）
                    map.put(key, value);
                } else {
                    // 数字值
                    try {
                        if (value.contains(".")) {
                            map.put(key, Double.parseDouble(value));
                        } else {
                            map.put(key, Integer.parseInt(value));
                        }
                    } catch (NumberFormatException e) {
                        // 如果解析失败，作为字符串处理
                        map.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 在窗口关闭时保存配置
     */
    private void saveConfigurationOnExit() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveTemplates();
                super.windowClosing(e);
            }
        });
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
