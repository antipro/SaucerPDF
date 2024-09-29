package com.haoze.saucer;

import com.lowagie.text.DocumentException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class SwtApp {

    private static final Logger logger = LoggerFactory.getLogger(SwtApp.class);

    public static void main(String[] args) {
        var display = new Display();
        var shell = new Shell(display);
        shell.setLayout(new GridLayout(2, false));
        shell.setText("Saucer PDF");
        shell.setSize(800, 600);

        var header = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        header.setText("""
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
                """);
        header.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
        var footer = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL| SWT.READ_ONLY);
        footer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        footer.setText("""
                </div>
                </div>
                </body>
                </html>
                """);
        footer.setBackground(display.getSystemColor(SWT.COLOR_GRAY));

        // Text area for block text
        var textArea = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        textArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Generate button
        var generateButton = new Button(shell, SWT.PUSH);
        generateButton.setText("Generate");
        generateButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        // Read-only text area for program log
        var logArea = new Text(shell, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        logArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));


        generateButton.addListener(SWT.Selection, event -> {
            try {
                var html = textArea.getText();
                html = FileTypeConvertUtil.htmlFormat(html);
                html = html.replaceAll("50%", "48%");
                File htmlFile = FileTypeConvertUtil.saveToFile(html);
                File pdfFile = File.createTempFile("HOSPITAL", ".pdf");
                logger.info("htmlPath: {} -- pdfName: {}", htmlFile.getAbsolutePath(), pdfFile.getAbsolutePath());
                FileTypeConvertUtil.createPDF(htmlFile.getAbsolutePath(), pdfFile.getAbsolutePath());
                logger.info("PDF generated successfully [{}]", pdfFile.getAbsolutePath());
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                } else {
                    logger.error("Desktop is not supported. Cannot open the PDF file.");
                }
            } catch (IOException | DocumentException e) {
                logger.error("Error generating PDF", e);
                logArea.setText(e.toString());
            }
        });


        // Open the shell
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
    }
}
