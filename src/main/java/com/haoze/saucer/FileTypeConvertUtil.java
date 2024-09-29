package com.haoze.saucer;


import com.lowagie.text.Rectangle;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.*;

import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author luy
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileTypeConvertUtil {

    private static final Logger logger = LoggerFactory.getLogger(FileTypeConvertUtil.class);

    private static final String TARGET = "temp.pdf";

    public static String getCurrentOperatingSystem() {
        return System.getProperty("os.name").toLowerCase();
    }

    public static void createPdfOutputStream(String fileContent, OutputStream os) throws DocumentException, IOException {
        ITextRenderer renderer = getRenderer(fileContent, os);
        renderer.createPDF(os);
    }

    public static void createPdfOutputStreamWithWaterMark(String fileContent, ByteArrayOutputStream os, String waterContent) throws DocumentException, IOException {
        ITextRenderer renderer = getRenderer(fileContent, os);
        renderer.createPDF(os);
        generateWaterMark(os, waterContent);
    }


    public static ITextRenderer getRenderer(String fileContent, OutputStream os) throws DocumentException, IOException {
        // copy css and font file to temp folder if not exists
        String tempFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        if (!tempFolder.endsWith("/")) {
            tempFolder += "/";
        }
        String fontCssFile = tempFolder + "font-style.css";
        File fontCssFileObj = new File(fontCssFile);
        if (!fontCssFileObj.exists()) {
            prepareStyleAndFonts(tempFolder);
        }
        ITextRenderer renderer = new ITextRenderer();
        renderer.setListener(new DefaultPDFCreationListener());
        ITextFontResolver resolver = renderer.getFontResolver();
        if ("linux".equals(getCurrentOperatingSystem())) {
            resolver.addFont(tempFolder + "simsun.ttc", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            resolver.addFont(tempFolder + "simkai.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            resolver.addFont(tempFolder + "simhei.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } else {
            resolver.addFont(tempFolder + "simkai.ttf", BaseFont.IDENTITY_H, true);
            resolver.addFont(tempFolder + "simsun.ttf", BaseFont.IDENTITY_H, true);
            resolver.addFont(tempFolder + "simhei.ttf", BaseFont.IDENTITY_H, true);
        }
        ResourceLoaderUserAgent callback = new ResourceLoaderUserAgent(renderer.getOutputDevice());
        callback.setSharedContext(renderer.getSharedContext());
        renderer.getSharedContext().setUserAgentCallback(callback);
        renderer.setDocumentFromString(fileContent);
        renderer.layout();
        return renderer;
    }

    private static void prepareStyleAndFonts(String tempFolder) throws IOException {
        InputStream cssInputStream = FileTypeConvertUtil.class.getClassLoader().getResourceAsStream("fonts/font-style.css");
        assert cssInputStream != null;
        // use Paths to create new file
        new File(tempFolder + "font-style.css").createNewFile();
        IOUtils.copy(cssInputStream, new FileOutputStream(tempFolder + "font-style.css"));
        InputStream fontInputStream = FileTypeConvertUtil.class.getClassLoader().getResourceAsStream("fonts/simsun.ttf");
        assert fontInputStream != null;
        new File(tempFolder + "simsun.ttf").createNewFile();
        IOUtils.copy(fontInputStream, new FileOutputStream(tempFolder + "simsun.ttf"));
        fontInputStream = FileTypeConvertUtil.class.getClassLoader().getResourceAsStream("fonts/simhei.ttf");
        assert fontInputStream != null;
        new File(tempFolder + "simhei.ttf").createNewFile();
        IOUtils.copy(fontInputStream, new FileOutputStream(tempFolder + "simhei.ttf"));
        fontInputStream = FileTypeConvertUtil.class.getClassLoader().getResourceAsStream("fonts/simsun.ttc");
        assert fontInputStream != null;
        new File(tempFolder + "simsun.ttc").createNewFile();
        IOUtils.copy(fontInputStream, new FileOutputStream(tempFolder + "simsun.ttc"));
        fontInputStream = FileTypeConvertUtil.class.getClassLoader().getResourceAsStream("fonts/simkai.ttf");
        assert fontInputStream != null;
        new File(tempFolder + "simkai.ttf").createNewFile();
        IOUtils.copy(fontInputStream, new FileOutputStream(tempFolder + "simkai.ttf"));
    }

    public static String htmlFormat(String htmlContent) {
        org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent, "UTF-8");
        Elements spanElements = doc.select("span");
        for (org.jsoup.nodes.Element spanElement : spanElements) {
            String text = spanElement.ownText();
            if (text.isEmpty()) {
                continue;
            }
            if (text.contains("<")
                    || text.contains(">")
                    || text.contains("&")
                    || text.contains("/")
                    || text.contains("\"")
                    || text.contains("'")
            ) {
                if (text.length() == 1) {
                    continue;
                }
                String newText = text
                        .replaceAll("&", "&amp;")
                        .replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;")
                        .replaceAll("/", "&#x2F;")
                        .replaceAll("\"", "&quto;")
                        .replaceAll("'", "&#39;");
                htmlContent = htmlContent.replace(text, newText);
            }
        }

        String tempFolderURL = new File(System.getProperty("java.io.tmpdir")).toURI().toString();
        if (htmlContent != null && !htmlContent.startsWith("<!DOCTYPE")) {
            String prefix = """
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                    <link href='%sfont-style.css' rel='stylesheet' type='text/css' />
                </head>
                <body>
                	<div style="width:155mm;margin-left:1mm;">
                	<div style="width:155mm;
                		 line-height:24px;
                		 color:#000;
                		 font-size:14px;
                		 font-family:'SimSun';
                		 vertical-align:top;">
                """.formatted(tempFolderURL);
            String suffix = "</div></div></body></html>";
            htmlContent = prefix + htmlContent + suffix;
        }

        htmlContent = htmlContent
                .replaceAll("&ensp;", " ")
                .replaceAll("&nbsp", "&nbsp;")
                .replaceAll("&nbsp[;]+", "&#160;")
                .replaceAll("<br>", "<br/>")
                .replaceAll("<hr>", "<hr/>")
                // .replaceAll("(?![^<]*>|<[^/]*\\\\/>)/", "&#x2F;")
                // .replaceAll("(?<![<\\\\s])<(?!((?!</?[^>]*>).|<!--.*?-->))", "&lt;")
                .replaceAll("宋体", "'SimSun'");
        return htmlContent;
    }

    /**
     * html转pdf
     *
     * @param htmlFilePath HTML文件路径
     * @param pdfFilePath  PDF文件路径
     * @throws IOException       读写异常
     * @throws DocumentException 文档异常
     */
    public static void createPDF(String htmlFilePath, String pdfFilePath)
            throws IOException, DocumentException {
        try (OutputStream os = new FileOutputStream(pdfFilePath)) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setListener(new PDFCreationListener() {
                @Override
                public void preOpen(ITextRenderer iTextRenderer) {
                    logger.info("line");
                }

                @Override
                public void preWrite(ITextRenderer iTextRenderer, int i) {
                    PdfWriter writer = iTextRenderer.getWriter();
                    writer.setPageEvent(new PdfPageEventHelper() {
                        @Override
                        public void onOpenDocument(PdfWriter writer, Document document) {
                        }

                        @Override
                        public void onStartPage(PdfWriter writer, Document document) {
                        }

                        @Override
                        public void onEndPage(PdfWriter writer, Document document) {
                            logger.info("onEndPage");
                        }

                        @Override
                        public void onCloseDocument(PdfWriter writer, Document document) {
                        }

                        @Override
                        public void onParagraph(PdfWriter writer, Document document, float paragraphPosition) {
                        }

                        @Override
                        public void onParagraphEnd(PdfWriter writer, Document document, float paragraphPosition) {
                        }

                        @Override
                        public void onChapter(PdfWriter writer, Document document, float paragraphPosition, Paragraph title) {
                        }

                        @Override
                        public void onChapterEnd(PdfWriter writer, Document document, float position) {
                        }

                        @Override
                        public void onSection(PdfWriter writer, Document document, float paragraphPosition, int depth, Paragraph title) {
                        }

                        @Override
                        public void onSectionEnd(PdfWriter writer, Document document, float position) {
                        }

                        @Override
                        public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
                        }
                    });
                }

                @Override
                public void onClose(ITextRenderer iTextRenderer) {

                }
            });
            // copy css and font file to temp folder if not exists
            String tempFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
            String fontCssFile = tempFolder + "font-style.css";
            File fontCssFileObj = new File(fontCssFile);
            if (!fontCssFileObj.exists()) {
                prepareStyleAndFonts(tempFolder);
            }
            ITextFontResolver resolver = renderer.getFontResolver();
            if ("linux".equals(getCurrentOperatingSystem())) {
                resolver.addFont(tempFolder + "simsun.ttc", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                resolver.addFont(tempFolder + "simkai.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else {
                resolver.addFont(tempFolder + "simkai.ttf", BaseFont.IDENTITY_H, true);
                resolver.addFont(tempFolder + "simsun.ttf", BaseFont.IDENTITY_H, true);
            }
            ResourceLoaderUserAgent callback = new ResourceLoaderUserAgent(renderer.getOutputDevice());
            callback.setSharedContext(renderer.getSharedContext());
            renderer.getSharedContext().setUserAgentCallback(callback);
            File file = new File(htmlFilePath);
            renderer.setDocument(file);
            renderer.layout();
            renderer.createPDF(os);
        }
    }

    private static class ResourceLoaderUserAgent extends ITextUserAgent {
        public ResourceLoaderUserAgent(ITextOutputDevice outputDevice) {
            super(outputDevice);
        }

        @Override
        protected InputStream resolveAndOpenStream(String uri) {
            InputStream is = super.resolveAndOpenStream(uri);
            logger.info("IN resolveAndOpenStream(): {}", uri);
            return is;
        }
    }

    /**
     * 将HTML字符串转换为HTML文件
     *
     * @param textData HTML符串
     * @return 文件对象
     */
    public static File saveToFile(String textData) throws IOException {
        File file = File.createTempFile("HOSPITAL", "HTML");
        FileWriter fileWriter = new FileWriter(file);
        IOUtils.write(textData.getBytes(), fileWriter, StandardCharsets.UTF_8);
        fileWriter.close();
        return file;
    }

    public static void generateWaterMark(ByteArrayOutputStream baos, String watermarkWord) {
        ByteArrayInputStream bais = null;
        PdfReader pdfReader = null;
        PdfStamper pdfStamper = null;
        try {
            // 使用字节数组创建输入流
            bais = new ByteArrayInputStream(baos.toByteArray());
            pdfReader = new PdfReader(bais);
            pdfStamper = new PdfStamper(pdfReader, baos);
            // 原pdf文件的总页数
            int pageSize = pdfReader.getNumberOfPages();
            String tempFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
            String fontCssFile = tempFolder + "font-style.css";
            File fontCssFileObj = new File(fontCssFile);
            if (!fontCssFileObj.exists()) {
                prepareStyleAndFonts(tempFolder);
            }
            BaseFont font = BaseFont.createFont(tempFolder + "simsun.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);

            // 设置字体
            // 设置填充字体不透明度为0.2f
            PdfGState gs = new PdfGState();
            gs.setFillOpacity(0.4f);
            Document document = new Document();
            float documentWidth = document.getPageSize().getWidth(), documentHeight = document.getPageSize().getHeight();
            final float xStart = 0, yStart = 0, xInterval = 100, yInterval = 200, rotation = 45, fontSize = 18;
            int red = 128, green = 128, blue = 128;

            for (int i = 1; i <= pageSize; i++) {
                // 水印在之前文本下
                PdfContentByte pdfContentByte = pdfStamper.getUnderContent(i);
                pdfContentByte.beginText();
                // 文字水印 颜色
                pdfContentByte.setColorFill(new Color(red, green, blue));
                // 文字水印 字体及字号
                pdfContentByte.setFontAndSize(font, fontSize);
                pdfContentByte.setGState(gs);
                // 文字水印 起始位置
                pdfContentByte.setTextMatrix(xStart, yStart);

                for (float x = xStart; x <= documentWidth + xInterval; x += xInterval) {
                    for (float y = yStart; y <= documentHeight + yInterval; y += yInterval) {
                        pdfContentByte.showTextAligned(Element.ALIGN_CENTER, watermarkWord, x, y, rotation);
                    }
                }
                pdfContentByte.endText();
            }

        } catch (Exception e) {
            logger.error("添加水印失败", e);
        } finally {
            // 关闭
            if (pdfStamper != null) {
                try {
                    pdfStamper.close();
                } catch (DocumentException | IOException e) {
                    logger.error("关闭pdfStamper失败", e);
                }
            }
            if (pdfReader != null) {
                pdfReader.close();
            }
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException e) {
                    logger.error("关闭文件输入流失败", e);
                }
            }
        }
    }

    public static void main(String[] args) {
        String htmlFile = "index.html";
        String pdfFile = "test.pdf";
        try {
            String htmlContent = IOUtils.toString(new FileInputStream(htmlFile), Charset.defaultCharset());
            htmlContent = FileTypeConvertUtil.htmlFormat(htmlContent);
            // FileTypeConvertUtil.createPdfOutputStreamWithWaterMark(
            //         htmlContent,
            //         new FileOutputStream(pdfFile), "测试"
            // );
        } catch (Exception e) {
            logger.error("程序错误", e);
        }
    }
}
